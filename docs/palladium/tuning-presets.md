# Palladium Tuning Presets

Palladium should expose documented presets rather than asking administrators to discover every risky optimization by trial and error.

Hardware-specific profiles with ready-to-merge config overlays and JVM flags (low-end hosts and
~500-player hosts) live in [performance-profiles.md](performance-profiles.md).

## Compatibility Mode

Use for plugin-heavy public servers.

- Keep experimental async systems disabled.
- Prefer Paper/Purpur-compatible behavior.
- Keep event-skipping optimizations limited to checks that preserve listener semantics.
- Enable low-risk allocation and lookup optimizations.

## Performance Mode

Use for controlled tame.gg deployments with benchmark coverage.

- Enable async mob spawning.
- Evaluate async chunk send only after queue saturation tests pass.
- Evaluate async pathfinding only with bounded queue and timeout metrics.
- Evaluate multithreaded tracker only with packet-order regression tests.
- Keep parallel world ticking disabled unless the plugin set has been audited.

## Roadmap Link

The full optimization backlog is tracked in [optimization-roadmap.md](optimization-roadmap.md). Do not enable roadmap items by default until their listed benchmark, replay, and rollback gates are satisfied.

## Vanilla Correctness Mode

Use for SMP servers that value mechanic compatibility.

- Keep behavior-changing gameplay optimizations disabled.
- Prefer vanilla hopper behavior.
- Keep secure seed and protocol integrations explicit.
- Document every enabled deviation from vanilla mechanics.
