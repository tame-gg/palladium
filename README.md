<p align="center">
</p>

<div align="center">

# Palladium

The tame.gg Minecraft server fork for performance-heavy Paper networks.

[![Build](https://img.shields.io/github/actions/workflow/status/tame-gg/palladium/build.yml?style=for-the-badge&label=build&colorA=151a18&colorB=2e8b57)](https://github.com/tame-gg/palladium/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/actions/workflow/status/tame-gg/palladium/release.yml?style=for-the-badge&label=release&colorA=151a18&colorB=3b82f6)](https://github.com/tame-gg/palladium/actions/workflows/release.yml)
[![Java 21](https://img.shields.io/badge/java-21-ef4444?style=for-the-badge&colorA=151a18)](https://adoptium.net/temurin/releases/?version=21)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.11-f59e0b?style=for-the-badge&colorA=151a18)](https://www.minecraft.net/)

**English** | [中文](public/readme/README_CN.md)

</div>

Palladium is the tame.gg fork of [Leaf](https://github.com/Winds-Studio/Leaf), built on top of [Purpur](https://github.com/PurpurMC/Purpur), [Paper](https://papermc.io/), and the performance work of the wider Paper fork ecosystem. It keeps compatibility with Paper-style server operations while adding Palladium-owned runtime safety, benchmark, release, and operator tooling around high-risk performance changes.

> [!WARNING]
> Palladium is performance-oriented server software. Back up worlds and configs before switching, test plugins in a staging environment, and treat experimental async features as opt-in until they have passed your workload.

## Why Palladium

- **Paper-compatible base**: keeps the familiar Paper/Purpur administration and plugin model.
- **Performance-first roadmap**: tracks async pathfinding, chunk send, player-data save, mob spawning, entity tracking, packet flow, IO, and benchmark work in one place.
- **Operator visibility**: `/palladium perf` exposes queue, async, and JVM memory views without requiring a profiler for basic triage.
- **Runtime safety culture**: risky systems are expected to ship with metrics, rollback paths, and staged rollout documentation.
- **Release-ready CI**: GitHub Actions can build, verify, rename, checksum, and publish a `palladium-<version>.jar` artifact.
- **Upstream attribution**: Palladium builds on Paper, Purpur, Leaf, Gale, Pufferfish, and other fork work instead of hiding that history.

## Current Identity

| Field | Value |
| --- | --- |
| Project | `palladium` |
| Authors | `tame.gg, asianrizz, koels` |
| Release version | `0.1.1` |
| Maven group | `gg.tame.palladium` |
| Java package | `gg.tame.palladium` |
| Minecraft target | `1.21.11` |
| Java runtime | Temurin 21 LTS |

## Download

Use the **palladium** workflow from [GitHub Actions](https://github.com/tame-gg/palladium/actions/workflows/release.yml) or create a `v*` tag to build a release artifact. The release workflow produces:

- `palladium-0.1.1.jar`
- `palladium-0.1.1.jar.sha256`
- `palladium-0.1.1.yml`
- `palladium-0.1.1-notes.md`

Until tame.gg-owned download endpoints are configured, GitHub Actions artifacts are the release source of truth.

## Quick Start

```bash
git clone https://github.com/tame-gg/palladium.git
cd palladium

./gradlew applyAllPatches
./gradlew check
./gradlew createMojmapPaperclipJar
scripts/prepareRelease.sh dist
```

The release-ready jar will be written to:

```text
dist/palladium-0.1.1.jar
```

## Local Verification

Use the one-command verifier when you want the same basic checks CI runs:

```bash
scripts/verifyLocal.sh
```

This runs the patch audit, applies all patches, executes Gradle checks, builds the Mojmap paperclip jar, and verifies the release artifact. Use Java 21 LTS. If you switch Java runtimes, rerun paperweight tasks with `--rerun-tasks` or clear stale `.gradle/caches/paperweight` output.

## Operator Commands

| Command | Purpose |
| --- | --- |
| `/palladium version` | Show the Palladium server identity and version path. |
| `/palladium mspt` | Inspect server tick timing. |
| `/palladium perf queues` | Inspect async pathfinding queue counters. |
| `/palladium perf async` | Inspect high-risk async feature state. |
| `/palladium perf memory` | Inspect JVM memory and processor data. |
| `/palladium perf gc` | Inspect GC collection counts and time per collector. |
| `/palladium features` | List and run 50 live operator feature commands. |

## Documentation

- [Palladium identity](docs/palladium/identity.md)
- [Runtime safety gates](docs/palladium/runtime-safety.md)
- [Performance profiles and JVM flags](docs/palladium/performance-profiles.md)
- [Optimization roadmap](docs/palladium/optimization-roadmap.md)
- [Roadmap implementation status](docs/palladium/roadmap-implementation-status.md)
- [Feature foundation](docs/palladium/feature-foundation.md)
- [Release process](docs/release/release-process.md)
- [Upstream sync policy](docs/upstream/sync-policy.md)
- [Patch risk index](docs/upstream/patch-risk-index.md)
- [Benchmark plans](docs/benchmarks/macrobench.md)

## API

Palladium keeps inherited Paper/Purpur/Leaf API compatibility and reserves `gg.tame.palladium` for Palladium-owned API surfaces.

### Maven

```xml
<repository>
    <id>palladium</id>
    <url>https://maven.tame.gg/snapshots/</url>
</repository>

<dependency>
    <groupId>gg.tame.palladium</groupId>
    <artifactId>leaf-api</artifactId>
    <version>1.21.11-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### Gradle Kotlin DSL

```kotlin
repositories {
    maven("https://maven.tame.gg/snapshots/")
}

dependencies {
    compileOnly("gg.tame.palladium:leaf-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
```

## Repository Map

| Path | Purpose |
| --- | --- |
| `leaf-api/` | API module and inherited API patch surface. |
| `leaf-server/` | Server module, patch queues, and Palladium-owned server code. |
| `docs/palladium/` | Palladium-specific identity, runtime safety, roadmap, and tuning docs. |
| `docs/upstream/` | Upstream sync and patch-risk maintenance docs. |
| `scripts/` | Patch audit, release, benchmark, and verification helpers. |
| `.github/workflows/` | Build, patch audit, and release automation. |

## Release Automation

The [palladium workflow](.github/workflows/release.yml) compiles the server, verifies the patch stack, renames the paperclip jar, writes release metadata, uploads artifacts, and publishes a GitHub Release when run manually or from a `v*` tag.

Default release metadata:

```yaml
name: palladium
authors: tame.gg, asianrizz, koels
version: 0.1.1
```

## Credits

Palladium exists because the Paper fork ecosystem has carried years of difficult server work forward. Palladium inherits, adapts, or learns from:

- [Paper](https://papermc.io/)
- [Purpur](https://github.com/PurpurMC/Purpur)
- [Leaf](https://github.com/Winds-Studio/Leaf)
- [Gale](https://github.com/GaleMC/Gale)
- [Pufferfish](https://github.com/pufferfish-gg/Pufferfish)
- [Leaves](https://github.com/LeavesMC/Leaves)
- [SparklyPaper](https://github.com/SparklyPower/SparklyPaper)
- [Kaiiju](https://github.com/KaiijuMC/Kaiiju)
- [Luminol](https://github.com/LuminolMC/Luminol)
- [Sakura](https://github.com/Samsuik/Sakura)
- [Moonrise](https://github.com/Tuinity/Moonrise)

See [LICENSE.md](LICENSE.md) for licensing and inherited project obligations.
