package akashic.storage.backend

import com.typesafe.config.Config

class BackendFactory(config: Config) {
  def build: FileSystemLike = {
    // println(config)
    val klass = Class.forName(config.getString("type") + "$")
    // println(klass)
    val obj = klass.getField("MODULE$").get(null)
    // println(obj)
    val method = klass.getMethod("fromConfig", classOf[Config])
    // println(method)
    val result = method.invoke(obj, config).asInstanceOf[FileSystemLike]
    // println(result)
    result
  }
}
