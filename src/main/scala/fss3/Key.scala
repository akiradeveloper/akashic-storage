package fss3

import java.nio.file.Path

import scala.util.{Random, Try}

case class Key(parent: Bucket, path: Path) {

  def mk: Unit = {
    path.mkdirp
    path.resolve("versions").mkdirp
  }

  val versions = path.resolve("versions")

  def getKeyName = {
    parent.keys.relativize(path).toString
  }

  def listVersions: Seq[Path] = {
    versions.children

      // exclude multipart uploads
      // complete versions have integer names
      .filter(_.lastName.optInt.isDefined)

      // descending order by Id
      .sortWith(new Version(this, _).id > new Version(this, _).id)
  }

  // no versionId is specified
  // returns the peak of the version stack
  def getCurrentVersion: Option[Version] = {
    // TODO consider delete not only completed
    val ov = listVersions.find(new Version(this, _).completed).map(new Version(this, _))
    if (ov.isEmpty) {
      None
    } else {
      val v = ov.get
      if (!v.metaT.isVersioned && v.metaT.isDeleteMarker) {
        None
      } else {
        ov
      }
    }
  }

  def acquireNewVersion: Version = {
    val latestId = listVersions.headOption match {
      case Some(a) => new Version(this, a).id
      case None => 0
    }
    val newVer = new Version(this, versions.resolve((latestId + 1).toString))
    newVer.mk
    newVer
  }

  def upload(uploadId: String): Upload = {
    Upload(this, versions.resolve(uploadId))
  }

  def findUpload(uploadId: String): Try[Upload] = {
    Try {
      val r = upload(uploadId)
      r.path.exists.orFailWith(Error.NoSuchUpload())
      r
    }
  }

  def acquireNewUpload: Upload = {
    val uploadId = Random.alphanumeric.take(20).mkString
    val newUpload = Upload(this, versions.resolve(uploadId))
    newUpload.mk
    newUpload
  }

  def deleteVersioned(versionId: Int): Option[Int] = {
    // TODO
    // put "delete file"
    // returns if the deleted was a marker
    None
  }

  def deleteUnversioned: Option[Int] = {
    val newVer = acquireNewVersion

    Meta.t(
      isVersioned = parent.versioningEnabled,
      isDeleteMarker = true,
      eTag = "",
      KVList.builder.build,
      KVList.builder.build
    ).write(newVer.meta)

    newVer.commit

    if (newVer.metaT.isVersioned) {
      Some(newVer.id)
    } else {
      None
    }
  }

  def delete(versionId: Option[Int]): Option[Int] = {
    versionId match {
      case None => deleteUnversioned
      case Some(id) => deleteVersioned(id)
    }
  }
}

