#!/usr/bin/env bash
set -euo pipefail

# Prints a recommended Palladium start command for a hardware profile.
# See docs/palladium/performance-profiles.md for the reasoning behind each flag.

usage() {
  cat >&2 <<'EOF'
Usage: scripts/jvmFlags.sh <low-end|balanced|high-player> [--heap <MB>] [--gc g1|zgc|serial] [--jar <file>]

  --heap   Heap size in MB (sets -Xms and -Xmx). Default: derived from total RAM.
  --gc     Garbage collector. Default: serial for 1 CPU, g1 otherwise.
  --jar    Server jar name. Default: palladium.jar

RAM and CPU detection honours cgroup (container) limits.
EOF
  exit 2
}

[ "$#" -ge 1 ] || usage
profile="$1"
shift

case "$profile" in
  low-end|balanced|high-player) ;;
  *) usage ;;
esac

heap_mb=""
gc=""
jar="palladium.jar"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --heap) heap_mb="${2:-}"; shift 2 ;;
    --gc) gc="${2:-}"; shift 2 ;;
    --jar) jar="${2:-}"; shift 2 ;;
    *) usage ;;
  esac
done

# Containers (Docker, Pterodactyl, ...) expose host totals through /proc/meminfo
# and nproc; prefer the cgroup limits when they are lower.
cgroup_root="${PALLADIUM_CGROUP_ROOT:-/sys/fs/cgroup}"

cgroup_cpus() {
  local quota period
  if [ -r "$cgroup_root/cpu.max" ]; then
    read -r quota period < "$cgroup_root/cpu.max"
  elif [ -r "$cgroup_root/cpu/cpu.cfs_quota_us" ] && [ -r "$cgroup_root/cpu/cpu.cfs_period_us" ]; then
    quota=$(cat "$cgroup_root/cpu/cpu.cfs_quota_us")
    period=$(cat "$cgroup_root/cpu/cpu.cfs_period_us")
  fi
  case "${quota:-max}" in
    max|-1|'') return 1 ;;
  esac
  [ "${period:-0}" -gt 0 ] || return 1
  echo $(( (quota + period - 1) / period ))
}

cgroup_mem_mb() {
  local limit=""
  if [ -r "$cgroup_root/memory.max" ]; then
    limit=$(cat "$cgroup_root/memory.max")
  elif [ -r "$cgroup_root/memory/memory.limit_in_bytes" ]; then
    limit=$(cat "$cgroup_root/memory/memory.limit_in_bytes")
  fi
  case "$limit" in
    ''|max|*[!0-9]*) return 1 ;;
  esac
  echo $(( limit / 1024 / 1024 ))
}

cpus=$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 2)
if quota_cpus=$(cgroup_cpus) && [ "$quota_cpus" -lt "$cpus" ]; then
  cpus=$quota_cpus
fi

if [ -z "$heap_mb" ]; then
  total_mb=0
  if [ -r /proc/meminfo ]; then
    total_mb=$(awk '/^MemTotal:/ { print int($2 / 1024) }' /proc/meminfo)
  fi
  if limit_mb=$(cgroup_mem_mb) && [ "$limit_mb" -gt 0 ] && { [ "$total_mb" -le 0 ] || [ "$limit_mb" -lt "$total_mb" ]; }; then
    total_mb=$limit_mb
  fi
  if [ "$total_mb" -le 0 ]; then
    echo "Cannot detect total RAM; pass --heap <MB>." >&2
    exit 1
  fi

  # Leave room for the OS and for JVM off-heap memory (metaspace, code cache,
  # thread stacks, Netty direct buffers, GC bookkeeping): roughly 600-900 MB on
  # small servers and several GB with hundreds of connections.
  case "$profile" in
    low-end) min_reserve=900; pct=30 ;;
    balanced) min_reserve=1536; pct=25 ;;
    high-player) min_reserve=4096; pct=25 ;;
  esac
  reserve=$(( total_mb * pct / 100 ))
  [ "$reserve" -ge "$min_reserve" ] || reserve=$min_reserve
  heap_mb=$(( (total_mb - reserve) / 256 * 256 ))
  # A handful of players never needs more; a smaller heap keeps GC work and RSS down.
  if [ "$profile" = "low-end" ] && [ "$heap_mb" -gt 4096 ]; then
    heap_mb=4096
  fi
  if [ "$heap_mb" -lt 768 ]; then
    echo "Only ${total_mb} MB RAM detected; Palladium needs at least a 768 MB heap. Pass --heap to override." >&2
    exit 1
  fi
fi

case "$heap_mb" in
  ''|*[!0-9]*) echo "--heap must be a number of megabytes" >&2; exit 2 ;;
esac

if [ -z "$gc" ]; then
  if [ "$cpus" -le 1 ]; then gc=serial; else gc=g1; fi
fi

flags=("-Xms${heap_mb}M" "-Xmx${heap_mb}M")

case "$gc" in
  g1)
    flags+=(-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200
      -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC
      -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:G1MixedGCLiveThresholdPercent=90
      -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:MaxTenuringThreshold=1
      -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true)
    if [ "$heap_mb" -ge 12288 ]; then
      flags+=(-XX:G1NewSizePercent=40 -XX:G1MaxNewSizePercent=50 -XX:G1HeapRegionSize=16M
        -XX:G1ReservePercent=15 -XX:InitiatingHeapOccupancyPercent=20)
    else
      flags+=(-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M
        -XX:G1ReservePercent=20 -XX:InitiatingHeapOccupancyPercent=15)
    fi
    ;;
  zgc)
    # Generational ZGC. On JDK 21 generational mode must be requested explicitly.
    flags+=(-XX:+UseZGC -XX:+ZGenerational -XX:+DisableExplicitGC)
    ;;
  serial)
    flags+=(-XX:+UseSerialGC -XX:+DisableExplicitGC)
    ;;
  *)
    echo "--gc must be g1, zgc, or serial" >&2
    exit 2
    ;;
esac

flags+=(-XX:+PerfDisableSharedMem)

# Pre-touching the whole heap makes boot slow on weak CPUs and commits memory a
# small server may never use; larger hosts want it so a bad -Xmx fails at boot.
if [ "$profile" != "low-end" ] && [ "$gc" != "serial" ]; then
  flags+=(-XX:+AlwaysPreTouch)
fi

if [ "$profile" = "high-player" ]; then
  flags+=(-XX:+UseTransparentHugePages)
fi

# Palladium-specific: skip the vanilla profiler's per-call ThreadLocal lookups.
# /debug start and /perf start become unavailable; use spark instead.
flags+=(-DPalladium.disable-vanilla-profiler=true)

echo "# profile=${profile} cpus=${cpus} heap=${heap_mb}M gc=${gc}"
echo "java ${flags[*]} -jar ${jar} --nogui"
