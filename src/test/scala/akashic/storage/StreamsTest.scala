package akashic.storage

import java.io.StringWriter
import java.nio.file.Files

import akashic.storage.backend.Streams
import org.apache.commons.io.{IOUtils, FileUtils}
import org.scalatest.FunSuite

class StreamsTest extends FunSuite {
  test("InputStream from Chunks") {
    val data = Stream(
      Some("akira".getBytes),
      Some("developer".getBytes)) #::: Streams.eod
    val inp = Streams.inputStreamFromStream(data)
    val writer = new StringWriter()
    IOUtils.copy(inp, writer)
    assert(writer.toString === "akiradeveloper")
  }
}
