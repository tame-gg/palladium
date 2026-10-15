# Palladium Performance Profiles

Two ready-made tuning profiles live under [`profiles/`](profiles/):

| Profile | Target hardware | Players |
| --- | --- | --- |
| [`low-end`](profiles/low-end/) | 1-2 vCPU, 2-4 GB RAM (small VPS, container, Raspberry-Pi class) | up to ~10 |
| [`high-player`](profiles/high-player/) | 16+ fast cores, 32 GB RAM, behind Velocity | ~500 |

Each profile is a set of **partial** config files that only list the keys they change. Palladium's
code defaults stay Paper/Purpur-compatible; profiles are how an operator opts into trade-offs.
Every key that changes gameplay or plugin-visible behaviour is marked `[B]` in the overlay files —
review those lines and drop any you do not want.

## Applying A Profile

1. Stop the server and back up the world folders and every config file.
2. Start Palladium once so it writes complete config files (skip on a fresh install: you can copy the
   overlays in before the first boot and the server fills in the remaining defaults).
3. Merge each overlay into the file with the same name. With [yq](https://github.com/mikefarah/yq):

   ```bash
   yq -i '. *= load("docs/palladium/profiles/low-end/config/paper-global.yml")' config/paper-global.yml
   ```

   `server.properties` overlays are plain `key=value` lines; replace the matching lines by hand.
4. Generate a start command with [`scripts/jvmFlags.sh`](../../scripts/jvmFlags.sh) (see below).
5. Start the server and check `/palladium perf all`, `/palladium mspt`, and a spark profile.

Rollback is restoring the backed-up config files.

## Low-End Profile

Goal: keep one core free for the main thread, keep the heap small, and stop a single exploring
player from saturating the CPU or disk.

- **Distances**: `view-distance=6`, `simulation-distance=4`. Loaded chunks scale with the square of the
  view distance (about 529 chunks per player at 10, 225 at 6), so this is the largest memory and
  chunk-IO saving.
- **Idle**: `pause-when-empty-seconds=60` (the vanilla default that Paper turns off) stops ticking
  when nobody is online. Keep `-1` if a plugin must keep worlds ticking while the server is empty.
- **Threads**: one Moonrise worker and one IO thread, two Netty threads, and async mob spawning off
  (on one vCPU that thread cannot run in parallel; synchronous spawning is also vanilla behaviour —
  set it back to `true` on 2+ vCPUs).
- **Chunk pacing**: per-player send/load/generate rate caps and at most 8 concurrent generations.
  World generation is the most expensive thing a small server does: pregenerate (for example with
  Chunky) inside a world border.
- **Autosave smoothing**: 8 chunks per tick, at most 2 player saves per tick.
- **Network compression**: zlib level 4 instead of 6 (less CPU, slightly more bandwidth).
- **Entities** `[B]`: tighter activation and tracking ranges, `tick-inactive-villagers: false`, lower
  mob caps, closer monster despawn, larger item/XP merge radius, DAB for distant mob brains, entity
  save limits for projectiles and XP orbs, `max-entity-collisions: 4`.
- **Ticking** `[B]`: hoppers check for items every 8 ticks, grass spread every 4, spawners every 2,
  Purpur `tps-catchup: false` so a lag spike is not followed by a burst of catch-up ticks.
- **Optional** `[B]`: `redstone-implementation: ALTERNATE_CURRENT` and `hopper.disable-move-event`.

## High-Player Profile

Goal: keep per-player costs (chunk sending, entity tracking, locator bar, saves) bounded as the player
count grows. Honest expectations: 500 players on one Paper-based instance is at the edge of what a
single main thread can do. It needs a pregenerated world, players spread across the map, a lean plugin
set, Velocity in front (which takes over encryption and login), and load testing before launch.

- **Distances** `[B]`: `view-distance=8`, `simulation-distance=5`, `entity-broadcast-range-percentage=75`.
- **Locator bar** (experimental): the profile opts into two experimental options.
  `coalesce-move-waypoint-updates: true` sends one waypoint update per move instead of three (see "Code
  changes" below; off by default). `optimize-waypoint: true` `[B]` only updates waypoints on block
  crossings. Both log an experimental-module warning at startup; load-test before production. If the
  locator bar is not wanted, `/gamerule locator_bar false` in every world removes the O(players²) cost
  entirely.
- **Netty**: `settings.netty-threads: 8` in `spigot.yml` (still read by Paper; it sets
  `io.netty.eventLoopThreads` before the listener starts). Packet encoding and compression for 500
  connections run on these threads; the default of 4 becomes a bottleneck.
- **Chunks**: per-player send/load caps, `player-max-chunk-generate-rate: 10` (pregenerate!), and
  `prevent-moving-into-unloaded-chunks` `[B]`.
- **Saves**: `player-auto-save.max-per-tick: 5`; the default resolves to 20, so a mass join produces a
  burst of player saves on the main thread a few minutes later.
- **Statistics** `[B]`: Gale `increase-time-statistics: 20` (same totals, coarser granularity).
- **Entities** `[B]`: per-player mob caps trimmed (they multiply by player count), activation/tracking
  ranges, DAB, collision and despawn tuning as in the low-end profile.
- **Staging-only**: `async-entity-tracker`, `async-pathfinding`, and `OptimizeNonFlushPacketSending`
  are listed commented-out. They are high-risk (see [runtime-safety.md](runtime-safety.md)); enable one
  at a time under load testing.

## JVM Flags

`scripts/jvmFlags.sh <low-end|balanced|high-player> [--heap <MB>] [--gc g1|zgc|serial]` prints a full
start command. It reads total RAM and CPU count, preferring cgroup limits inside containers (Docker,
Pterodactyl) where `/proc/meminfo` and `nproc` report the host instead. `--heap` must be at least 768
MB; options without a value are rejected. Warnings (pre-touch, huge pages, unknown JDK) go to stderr,
so stdout is only the command.

```bash
scripts/jvmFlags.sh low-end                      # auto-size from RAM
scripts/jvmFlags.sh high-player --heap 24576      # G1, large-heap tuning
scripts/jvmFlags.sh high-player --heap 24576 --gc zgc
```

**Heap size.** Never give the heap all of the RAM. The JVM also needs metaspace, code cache, thread
stacks, Netty direct buffers, and GC bookkeeping (roughly 600-900 MB on a small server, several GB with
hundreds of connections), and the OS needs page cache for region files. The script reserves
`max(900 MB, 30%)` on low-end (heap capped at 4 GB), `max(1.5 GB, 25%)` on balanced, and
`max(4 GB, 25%)` on high-player, rounding the heap down to 256 MB:

| RAM | Profile | Heap (approx.) |
| --- | --- | --- |
| 2 GB | low-end | 1 GB |
| 3 GB | low-end | 2 GB |
| 4 GB | low-end | 2.5-2.75 GB |
| 32 GB | high-player | 23-24 GB |

`-Xms` equals `-Xmx` so the heap never resizes at runtime.

**Garbage collector.**

- *1 vCPU*: Serial GC. G1's concurrent threads would compete with the main thread for the only core.
- *2+ vCPU, small heap*: G1 with Aikar's flags (`G1NewSizePercent=30`, 8 MB regions). Low-end skips
  `-XX:+AlwaysPreTouch` so boot stays fast and unused heap pages are not committed.
- *Large heap (12 GB+)*: G1 with Aikar's large-heap variant (`G1NewSizePercent=40`, 16 MB regions,
  `InitiatingHeapOccupancyPercent=20`). This is the default for high-player because it is the
  best-known configuration for Paper.
- *Generational ZGC* (`--gc zgc`): near-zero pauses at some throughput cost. Worth an A/B test on
  high-player hosts with spare cores if `/palladium perf gc` or spark shows G1 pauses lining up with
  MSPT spikes. On JDK 21-22 generational mode needs `-XX:+ZGenerational`; JDK 23+ warns about that flag
  and JDK 24+ ignores it. The script runs `$JAVA`, `$JAVA_HOME/bin/java`, or `java` to check the version
  and only adds the flag on 21-22 (it warns and leaves it out if the version cannot be read).

**Other flags the script adds.**

- `-XX:+PerfDisableSharedMem`: avoids hsperfdata writes stalling threads on slow disks.
- `-XX:+AlwaysPreTouch` (balanced/high-player with G1 or ZGC): commits the whole heap at boot, so RSS is
  the full heap immediately. The script warns, because a memory limit that does not also cover off-heap
  memory gets the JVM OOM-killed during boot.
- `-XX:+UseTransparentHugePages` (high-player): fewer TLB misses on a 20+ GB heap. Requires THP set to
  `madvise` (preferred) or `always` in `/sys/kernel/mm/transparent_hugepage/enabled`; the script warns
  when it is `never` or unreadable, and when it is `always`.
- `-DPalladium.disable-vanilla-profiler=true`: skips the vanilla profiler's per-call ThreadLocal lookup
  on every `Profiler.get()`. `/debug start` and `/perf start` become unavailable; use spark instead.

**Optional, not added by the script.**

- `-XX:+UseNUMA` on multi-socket servers.
- `-XX:ActiveProcessorCount=<n>` if you want the JVM (and Palladium's thread pools) to size themselves
  for fewer cores than the machine has.
- `-XX:+UseCompactObjectHeaders` (JDK 25+) shrinks object headers and can cut heap use noticeably, but
  Palladium is built and tested on Java 21; treat JDK 25 as untested.

## Code Changes Behind These Profiles

These Palladium patches apply to every server, not only the profiles (experimental ones only when enabled):

| Patch | Effect | Behaviour |
| --- | --- | --- |
| `0330-Optimize-waypoint-manager` | Skips the per-move O(players) locator-bar scans when the `locator_bar` gamerule is off; snapshots connections without hashing every entry. | Unchanged |
| `0331-Coalesce-move-event-waypoint-updates` | Experimental, **off by default**. When `performance.coalesce-move-waypoint-updates: true`, defers locator-bar updates while CraftBukkit snaps a moving player back for `PlayerMoveEvent`, then flushes once at the final position. Removes two O(players) passes per moving player and the new → old → new waypoint packets on block crossings. | Unchanged unless enabled; when enabled, same final state |
| `0332` / paper `0081` Respect JVM CPU limits | Moonrise chunk workers and the worldgen pool are sized from `min(host cores, availableProcessors())`, so containers with CPU quotas no longer get pools sized for the host. | Unchanged on bare metal and VMs |
| `0333-Fix-playerdata-save-edge-cases` | A failed NBT encode no longer replaces the player's `.dat` with a partial buffer. | Matches vanilla |

`/palladium perf gc` reports per-collector GC counts and time for load-test correlation.
