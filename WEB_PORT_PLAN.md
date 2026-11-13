# WARCORE browser port — engineering plan

## Current finding (2026-11-13)

The repository is not currently browser-buildable. The existing multiplatform boundary is limited:

- `:ui` uses Kotlin Multiplatform but declares only `android()` and `jvm()`; its `commonMain` UI depends on `:core`.
- `:core` is a Kotlin/JVM + Java library (Java 25 toolchain), not a multiplatform module. It contains the game engine and platform-facing APIs.
- The game renderer contract is `io.github.rwx.render.GraphicsEngine` / `com.corrodinggames.rts.gameFramework.graphics.GraphicsEngine`. The current concrete Skia renderer imports `org.jetbrains.skia.*` and JVM APIs including `java.io.File`, `InputStream`, and `java.util.concurrent.locks.Lock`.
- Desktop startup is `DesktopMain`; it configures JVM globals, Koin modules, desktop storage/audio, optional Steam, and either Slick/LWJGL or Skia. Neither desktop host can run inside a browser.
- Android has a separate Android graphics backend and Android platform bridge. Those cannot be reused as a browser host.

## Additional source-level finding (2026-11-13)

The repository already contains a reusable caller-driven simulation session:
`desktop/src/main/kotlin/io/github/rwx/headless/HeadlessGameSession.kt`.
It initializes the real `GameEngine`, loads maps, advances `gameLoop`, and exposes the shared `GameSession` APIs. This is a useful lifecycle/input/session reference, but it is **not** a browser host: it still imports the desktop input handler, `PlatformStorage`, `SkiaGraphicsEngine`, `DesktopRenderBackend`, Java atomics/threads, and sets JVM system properties. Its frame loop explicitly skips drawing. Reusing this class unchanged would therefore not display the actual game in a browser.

The shared `GameSession` also imports JVM networking and concurrency types (including `Socket` and `CompletableFuture`), while the graphics contract imports `File`, `InputStream`, and `Lock`. This confirms that adding only a Kotlin/JS or Wasm target to `:ui` cannot compile the game engine.

## Correct target architecture

Do not publish a static placeholder or attempt to run the desktop entry point in a browser. A real web build needs a browser-specific host and renderer while reusing the game rules, simulation, maps, units, AI, and as much UI/game orchestration as possible.

1. **Platform seam:** split the current `GameSession` and graphics contracts into portable APIs and JVM-specific implementations. The first extraction should preserve the existing headless session behavior while replacing its desktop storage/input/render dependencies with injectable interfaces. Move filesystem, reflection/classloading, networking, audio, timers, and platform-specific APIs behind adapters.
2. **Browser renderer:** after the graphics contract no longer exposes JVM types, implement it on HTML Canvas 2D (initial compatibility backend). Support texture loading, image regions, transforms, clipping, blend modes, text metrics, render targets/layer buffers, and pixel access required by the game. Preserve CPU fallbacks where shader APIs are unavailable.
3. **Browser input and lifecycle:** map pointer, touch, wheel, keyboard, resize, visibility, and animation-frame callbacks to the existing game input/session APIs.
4. **Browser storage:** provide IndexedDB-backed saves/settings/maps/cache and a virtual asset reader. Browser file pickers/downloads replace desktop file APIs.
5. **Web UI/boot:** add a Kotlin/Wasm or Kotlin/JS target only after the shared code boundary is genuinely portable. Compose UI is not assumed to be reusable until its dependencies compile for that target.
6. **Validation:** first compile a minimal web host against the real game engine, then load the real main menu, then launch a skirmish and verify camera, selection, orders, building, AI, save/load, audio, and maps. Only then enable GitHub Pages deployment.

## Explicit blockers to resolve

- `:core` currently uses the Kotlin JVM plugin and contains Java sources; converting it wholesale to KMP is a large migration and likely not the safest first move.
- The graphics interface itself exposes JVM types (`File`, `InputStream`, `Lock`) and therefore cannot simply be implemented in a browser source set without refactoring the contract.
- The current Skia backend is JVM-specific even though its rendering logic is a useful reference for Canvas 2D behavior.
- Desktop startup relies on Swing/AWT, LWJGL/OpenGL, JVM runtime behavior, and optional native integrations.
- Mods currently rely on JVM class loading (URLClassLoader/DexClassLoader); browser mod execution needs a separate, sandboxed format/runtime and is not part of the first playable web milestone.

## Delivery rule

GitHub Pages deployment must remain disabled until a real browser bundle exists and a smoke test confirms that it boots the actual WARCORE game. A landing page is not a browser port.
