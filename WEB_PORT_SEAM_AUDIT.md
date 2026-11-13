# WARCORE browser port — first seam audit

Audit date: 2026-11-13  
Branch: `web-port`

## Verified CI baseline

GitHub Actions run [43394752988](https://github.com/itsalirezajr-design/WARCORE/actions/runs/43394752988) completed successfully on commit `d5bfbbe9457e42f7f70546541a5bc7c84e643619`.

The job compiled all of:

- `:mod-api:compileKotlin`
- `:core:compileKotlin`
- `:core:compileJava`
- `:ui:compileKotlinJvm`

This proves the current JVM baseline builds in CI. It does not prove a browser target exists.

## Progress since the initial audit

The first asset-loading seam has now been introduced without changing existing renderer call sites:

- `AssetSource` provides a byte-oriented read contract.
- `PlatformStorage` extends `AssetSource`, preserving the existing JVM storage API.
- `SkiaGraphicsEngine` accepts `AssetSource` and decodes supplied bytes directly.
- `GraphicsEngine.loadTextureBytes` provides a compatibility entry point for byte-backed hosts.
- The Slick and Skia renderers override that entry point to decode bytes directly instead of creating a JVM stream first.

These changes are preparatory only. They do **not** make `:core` a Kotlin Multiplatform module, and they do not provide a browser renderer or a playable web build.

## First hard boundary: GraphicsEngine

Source: `core/src/main/kotlin/com/corrodinggames/rts/gameFramework/graphics/GraphicsEngine.kt`

The interface currently exposes three JVM-specific types directly:

- `java.io.File` in `a(texture, file)`
- `java.io.InputStream` in texture creation
- `java.util.concurrent.locks.Lock` in the lock methods

It also exposes the full legacy drawing API, including textures, blend modes, rectangles, text metrics, shader programs, and render-target operations. A browser implementation should not be added until the resource and synchronization contracts are separated from this drawing contract.

### Recommended extraction

Introduce portable value/API types first, without changing game call sites all at once:

1. A resource handle or asset identifier instead of `File`.
2. An asset byte/source abstraction instead of `InputStream`.
3. A small renderer synchronization abstraction instead of `Lock` (or remove locking from the cross-platform contract and keep it inside each backend).
4. Keep the current JVM-facing adapter so Android/Desktop continue to compile while call sites migrate.
5. Add a browser Canvas2D implementation only against the portable contract.

Do not replace the existing abstract methods with browser-specific APIs. Preserve the current renderer behavior for the JVM backends.

## Second hard boundary: GameSession

Source: `core/src/main/kotlin/io/github/rwx/session/GameSession.kt`

The session abstraction itself imports JVM networking and concurrency types, including `Socket`, `InetSocketAddress`, `CompletableFuture`, `AtomicBoolean`, and `AtomicReference`. It also owns multiplayer/map-view behavior and many callbacks.

This is not a safe first file to mechanically move into a multiplatform source set. The safer initial browser milestone is a single-player boot path that reuses the simulation and map/unit logic while keeping multiplayer transport behind a later platform adapter.

## Third boundary: HeadlessGameSession is not a browser host

Source: `desktop/src/main/kotlin/io/github/rwx/headless/HeadlessGameSession.kt`

The class is useful because it advances the real `GameEngine` from a caller-driven `step(deltaSeconds)` loop. However, it still requires:

- `PlatformStorage`
- `DesktopInputHandler`
- `DesktopRenderBackend`
- `SkiaGraphicsEngine`
- JVM shutdown hooks, threads, and system properties

Its frame update explicitly skips drawing. It is therefore a simulation/lifecycle reference, not a playable web implementation.

## Safest implementation order

1. Keep the current JVM baseline green.
2. Define portable asset and synchronization contracts alongside the existing JVM contracts.
3. Adapt the JVM renderer to those contracts and prove Android/Desktop compilation remains green.
4. Extract a minimal single-player session bootstrap with injected storage, input, clock, and renderer.
5. Add a browser Canvas2D renderer and browser animation-frame/input host.
6. Add a browser target only after the engine dependency graph no longer pulls JVM-only classes into that target.
7. Validate the real main menu and a playable skirmish before enabling Pages.

## Non-goals for the first playable web milestone

- JVM mod class loading in the browser
- Steam integration
- Desktop windowing or LWJGL
- Multiplayer parity before single-player is proven

A static landing page or mock canvas is not a WARCORE browser port and must not be published as one.
