# Additional performance

Measured on October 14, 2026 in the local Windows client with JDK 25, Minecraft 1.8.9, OneConfig 1.2.18 and PolyNametag 1.2.4.

## Controlled before/after results

Each scene used 600 warmup passes and 1,200 measured passes, the same short synthetic player names, and PolyNametag without text shadows. Timings cover `NameTagESP.onRenderWorld`, including its GL calls; they exclude terrain, entity models, the HUD and most other mods. KB below means 1,000 bytes of allocation, not retained memory.

| Scene | Before median | After median | Before allocation/pass | After allocation/pass |
|---|---:|---:|---:|---:|
| 2 visible labels | 0.0594 ms | 0.0446 ms | 4.9 KB | 1.3 KB |
| 40 visible labels | 0.1789 ms | 0.1393 ms | 76.6 KB | 15.1 KB |
| 120 visible labels | 0.4604 ms | 0.3861 ms | 229.5 KB | 43.9 KB |
| 120 players outside range | 0.0025 ms | 0.0018 ms | 0.0 KB | 0.0 KB |
| ESP disabled | 0.0003 ms | 0.0002 ms | 0.0 KB | 0.0 KB |

For 120 visible labels, the median fell from 0.4604 ms to 0.3861 ms (about 16%), and allocations fell from 229.5 KB to 43.9 KB (about 81%). The corresponding 95th-percentile pass times were 0.7482 ms and 0.6772 ms. Repeated runs varied with JVM warmup and driver/system activity; these results are not a guaranteed FPS increase.

The comparison fixes the name-text cache cadence to one refresh per twelve render passes, representing a 20 Hz client tick at 240 FPS. Real allocation per frame depends on FPS and text changes. Dedicated thread CPU clocks were too coarse on this Windows JVM to resolve individual passes, so this report uses elapsed render-pass time rather than claiming precise processor-only time or GPU frame time.

## Changes

- Store formatted name text on its player entity and refresh once per client tick. Name/team/prefix/suffix changes invalidate it immediately; position, visibility and rendering continue every frame. The original display-name method still supplies the text, while unused hover/click component graphs are not retained. Temporary renderer decorations reset between calls.
- Store bot classification on the entity, using a world generation to invalidate it. Missing tab entries remain retryable. This removes repeated UUID hashing and concurrent-map reads and lets departed-player caches be reclaimed with their entities.
- Filter and render in one scan; prepare the GL pass only for a visible, in-range player. Reuse existing frustum/dispatcher state, buffers and GL state restoration. Cache duplicate font-width calculations only within the pass.
- Remove callback-result allocation from the optional PolyNametag sneaking-style hook, preserving the shift fix and the read-only ESP depth guard.
- Share in-flight UUID loads, reuse validated provider JSON across stats/profile parsers, reuse the immutable request coordinator, and remove redundant synchronization from lookup reference counts already protected by a monitor.
- Reuse fixed fallback-provider regexes and compare autocomplete prefixes without allocating lowercase copies for each tab-list name.

Names produced by another mod's per-frame animation are sampled at client-tick rate. PolyNametag's renderer-level colors, scale, backgrounds and shadows remain live. Glyph drawing still scales with the number and length of visible labels; batching or replacing that renderer would require a separate compatibility design.

## Validation

- 28 regression tests pass, including overlapping UUID requests, retry/error behavior, shared parsing, text formatting, copies and per-frame decorations.
- Fourteen render cases pass both with and without PolyNametag: standing/sneaking labels, master/feature toggles, ranges and text shadows.
- Runtime checks cover same-tick team/prefix updates, tick refresh, bot-cache invalidation, late tab arrival and unchanged depth-buffer values.
- The shift option preserves the sneaking label's height, with and without PolyNametag, in both normal and ESP rendering. The PolyNametag hooks change only its visibility checks, leaving its height adjustment intact.
- `build` succeeds; profiling fixtures and PolyNametag are not bundled in the distributable.
