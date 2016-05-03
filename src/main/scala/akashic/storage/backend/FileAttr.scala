package akashic.storage.backend

/**
 * Attributes on a file
 * @param creationTime The creation time (in millisecond)
 * @param length The length (or size) of the file
 */
case class FileAttr(creationTime: Long, length: Long)
