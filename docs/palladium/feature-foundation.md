# Palladium Feature Foundation

This document maps the 50 requested feature additions to concrete `/palladium features <key>` operator commands. The in-game command is the primary surface: each entry returns live server data or performs a bounded operator action instead of sending admins to external docs.

## Operator Commands

- `/palladium features` lists every runnable feature command.
- `/palladium features list [category]` filters the command list.
- `/palladium features categories` lists available categories.
- `/palladium features <KF-###|key> [args]` runs the selected feature.
- `/palladium features help` shows usage examples.
- `/palladium mspt` reports server and world MSPT without requiring parallel world ticking.
- `/palladium perf queues` shows async pathfinding queue counters.
- `/palladium perf async` shows high-risk async feature state and virtual-thread concurrency caps.
- `/palladium perf memory` shows JVM memory and processor data.
- `/palladium perf gc` shows per-collector GC counts, total and average time, and share of uptime.
- `/palladium perf all` prints all four perf views.

## Feature Commands

| Feature | Surface |
| --- | --- |
| KF-001 Server summary | `/palladium features summary` |
| KF-002 Health score | `/palladium features health` |
| KF-003 TPS report | `/palladium features tps` |
| KF-004 MSPT report | `/palladium features mspt` |
| KF-005 Memory report | `/palladium features memory` |
| KF-006 Request garbage collection | `/palladium features gc` |
| KF-007 Thread summary | `/palladium features threads` |
| KF-008 Thread state sample | `/palladium features thread-states` |
| KF-009 Uptime report | `/palladium features uptime` |
| KF-010 JVM report | `/palladium features jvm` |
| KF-011 Disk report | `/palladium features disk` |
| KF-012 World list | `/palladium features worlds` |
| KF-013 World details | `/palladium features world <world>` |
| KF-014 Chunk report | `/palladium features chunks` |
| KF-015 Chunk hotspots | `/palladium features chunk-hotspots` |
| KF-016 Entity report | `/palladium features entities` |
| KF-017 Entity type top list | `/palladium features entity-types` |
| KF-018 Block entity report | `/palladium features tile-entities` |
| KF-019 Player list | `/palladium features players` |
| KF-020 Player details | `/palladium features player <name>` |
| KF-021 Ping report | `/palladium features pings` |
| KF-022 Plugin report | `/palladium features plugins` |
| KF-023 Plugin details | `/palladium features plugin <name>` |
| KF-024 Scheduler report | `/palladium features scheduler` |
| KF-025 Permission report | `/palladium features permissions` |
| KF-026 Game rule report | `/palladium features gamerules <world>` |
| KF-027 Difficulty report | `/palladium features difficulties` |
| KF-028 Spawn limit report | `/palladium features spawn-limits` |
| KF-029 View distance report | `/palladium features view-distance` |
| KF-030 Recipe count | `/palladium features recipes` |
| KF-031 Advancement count | `/palladium features advancements` |
| KF-032 Scoreboard report | `/palladium features scoreboard` |
| KF-033 Save worlds | `/palladium features save-worlds` |
| KF-034 Config file report | `/palladium features config-files` |
| KF-035 Config search | `/palladium features config-search <text>` |
| KF-036 Async state | `/palladium features async-state` |
| KF-037 Pathfinding queue | `/palladium features path-queue` |
| KF-038 Async chunk send | `/palladium features chunk-send` |
| KF-039 Async playerdata save | `/palladium features playerdata-save` |
| KF-040 Async tracker | `/palladium features tracker` |
| KF-041 Parallel world ticking | `/palladium features parallel-worlds` |
| KF-042 Virtual thread pools | `/palladium features virtual-threads` |
| KF-043 Rollout check | `/palladium features rollout-check` |
| KF-044 Safe mode profile | `/palladium features safe-mode` |
| KF-045 Network report | `/palladium features network` |
| KF-046 Mob density report | `/palladium features mob-density` |
| KF-047 World file report | `/palladium features world-files <world>` |
| KF-048 Plugin author report | `/palladium features plugin-authors` |
| KF-049 Support bundle | `/palladium features support-bundle` |
| KF-050 Command help | `/palladium features command-help` |

## Runtime Safety Gates

The following items are intentionally runtime scaffolds until replay tests, stress tests, and soak tests exist for each behavior:

- Async chunk-send stress harness
- Packet-order replay tests
- Playerdata save ordering tests
- Config migration framework
- Per-feature rollback command
- Per-world async toggles
- Admin web status endpoint
- Sentry breadcrumbs for async failures
- Crash report Palladium section
- Entity tracker contention metrics

Each of these must satisfy the rollout gates in `docs/palladium/runtime-safety.md` before being changed from scaffold to default-enabled runtime behavior.

## Rollback Contract

Any high-risk feature must have one of these rollback paths before production use:

- a boolean config key that disables the feature after restart,
- a hot-reload-safe config path documented by the relevant `/palladium features <key>` output,
- or a safe-mode profile entry emitted by `scripts/safeModeProfile.sh`.

## Generated Catalog

The authoritative in-code catalog lives in `gg.tame.palladium.feature.PalladiumFeatureCatalog`. Tests enforce that all 50 feature entries remain present, have unique IDs, and expose a concrete surface.
