package akashic.storage.backend

case class FileAttr(creationTime: Long, length: Long, uniqueKey: Option[String])
