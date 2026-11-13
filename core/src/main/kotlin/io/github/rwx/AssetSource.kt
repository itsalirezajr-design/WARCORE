package io.github.rwx

/**
 * Read-only access to packaged game assets.
 *
 * This deliberately exposes bytes rather than JVM streams or filesystem paths so
 * browser hosts can provide assets from a bundle, fetch response, or IndexedDB.
 * Platform-specific storage implementations may continue to expose richer APIs
 * through [PlatformStorage].
 */
fun interface AssetSource {
    fun readAssetBytes(path: String): ByteArray?
}
