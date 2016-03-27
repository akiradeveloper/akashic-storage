package akashic.storage.backend

/**
 * Attributes on a file
 * @param creationTime The creation time (in millisecond)
 * @param length The length (or size) of the file
 * @param cacheKey This could be None if you don't use metadata caching.
 *                 The Some value should be different after the file is overwritten otherwise
 *                 the old metadata is continue to be used. (thus the path of the file isn't
 *                 the appropriate choice for the cache key)
 *                 Use metadata caching carefully. Read the Wiki
 */
case class FileAttr(creationTime: Long, length: Long, cacheKey: Option[String] = None)
