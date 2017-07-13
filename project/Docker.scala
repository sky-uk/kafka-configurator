import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{Docker => docker}
import sbt.Def
import sbt.Keys._

object Docker {

  lazy val dockerSettings = Seq(
    packageName in docker := packageName.value,
    dockerBaseImage := "anapsix/alpine-java:8",
    dockerUpdateLatest := updateLatest.value,
    dockerRepository := registryWithRepository("sky").value
  )

  def registryWithRepository(repository: String) = Def.setting {
    if (updateLatest.value) {
      val registry = sys.env.get("DOCKER_REGISTRY_HOST").map(_ + "/").getOrElse("")
      Some(s"$registry$repository")
    } else {
      Some(repository)
    }
  }

  def updateLatest = Def.setting {
    if (!version.value.contains("SNAPSHOT")) true
    else false
  }
}

