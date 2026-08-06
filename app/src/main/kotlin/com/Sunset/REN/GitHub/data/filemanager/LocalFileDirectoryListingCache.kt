package com.Sunset.REN.GitHub.data.filemanager

import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry

/**
 * Process-local warm cache for local directory listings.
 *
 * This is intentionally small and short lived: it exists to remove the cold-click
 * feeling when the file manager is opened from the app shell, not to replace an
 * explicit refresh or file-system truth.
 */
object LocalFileDirectoryListingCache {
    private val cachedListings = LinkedHashMap<String, CachedListing>()

    @Synchronized
    fun get(path: String, nowMillis: Long = System.currentTimeMillis()): List<FileManagerEntry>? {
        val cached = cachedListings[path] ?: return null
        if (nowMillis - cached.cachedAtMillis > MaxAgeMillis) {
            cachedListings.remove(path)
            return null
        }
        return cached.entries
    }

    @Synchronized
    fun put(path: String, entries: List<FileManagerEntry>, nowMillis: Long = System.currentTimeMillis()) {
        cachedListings[path] = CachedListing(entries = entries, cachedAtMillis = nowMillis)
        while (cachedListings.size > MaxCachedDirectories) {
            val eldestKey = cachedListings.keys.firstOrNull() ?: break
            cachedListings.remove(eldestKey)
        }
    }

    @Synchronized
    fun invalidate(path: String) {
        cachedListings.remove(path)
    }

    @Synchronized
    fun clear() {
        cachedListings.clear()
    }

    private data class CachedListing(
        val entries: List<FileManagerEntry>,
        val cachedAtMillis: Long
    )

    private const val MaxCachedDirectories = 6
    private const val MaxAgeMillis = 30_000L
}
