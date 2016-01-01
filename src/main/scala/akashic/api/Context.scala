package akashic.api

case class Context(requester: Option[String], resource: Path)
