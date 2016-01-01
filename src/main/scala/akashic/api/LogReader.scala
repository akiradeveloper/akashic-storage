case class LogReader(root: Path) {
  def get: Option[Path] = {
    maxId match {
      case 0 => None
      case a => Some(a)
    }.map(root.resolve(_))
  }
}
