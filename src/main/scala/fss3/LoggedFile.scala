package fss3

import java.nio.file.Path

// If we use s3fs as the backend
// which doesn't guarantee atomicity in renaming
// so temp and rename technique to emulate atomic write isn't correct.
// LoggedFile has logs underneath it
//
// e.g.
// name/
//   1/
//     data
//   2/
//     creating
//
// For incremental development of sub-resources in this project
// LoggedFile also cares for non-existence of the top directory (name in the above example)
// by returning optional value as the yet-not-implemented sub-resources don't exist.

case class LoggedFile(path: Path) {

  case class Log(path: Path) {
    val INITFLAG = "creating"

    def id = path.lastName.toInt

    def data = path.resolve("data")

    def completed = !(path.children.isEmpty || path.resolve(INITFLAG).exists)

    def mk {
      path.mkdirp
      path.resolve(INITFLAG).touch
    }

    def complete = path.resolve(INITFLAG).delete
  }

  private def listLogs: Seq[Log] = {
    path.children.map(a => Log(a)).sortBy(_.id).reverse
  }

  def get: Option[Path] = {
    if (!path.exists) {
      None
    } else {
      listLogs.find(_.completed).map(_.data)
    }
  }

  private def acquireNewLog: Log = {
    val newId = listLogs.lastOption.map(_.id) match {
      case Some(a) => a + 1
      case None => 1
    }
    val r = Log(path.resolve(newId.toString))
    r.mk
    r
  }

  def put(f: Path => Unit): Unit = {
    path.mkdirp
    val log = acquireNewLog
    f(log.data)
    log.complete
  }
}
