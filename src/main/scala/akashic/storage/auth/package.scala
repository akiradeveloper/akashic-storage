package akashic.storage

import io.finch.RequestReader

package object auth {
  val reader: RequestReader[String] = for {
    v2Result <- V2.reader
    v2PresignedResult <- V2Presigned.reader
  } yield {
    Seq(v2Result, v2PresignedResult).find(_.isDefined) match {
      case Some(a) => a.get
      case None => "" // not found
    }
  }
}
