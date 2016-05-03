package akashic.storage.backend

import java.io.File
import java.nio.file.{Files, Paths}

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils

class LocalTest extends BALTraitTestTemplate(ConfigFactory.parseResources("backend-local.conf").getConfig("backend")) {
  override def beforeEach() = {
    FileUtils.cleanDirectory(new File("/mnt/akashic-storage"))
    super.beforeEach()
  }
}

class MemoryTest extends BALTraitTestTemplate(ConfigFactory.parseResources("backend-memory.conf").getConfig("backend"))
