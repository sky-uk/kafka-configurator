import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{Docker => docker}

object Docker {

  lazy val dockerSettings = Seq(
    packageName in docker := packageName.value,
    dockerBaseImage := "anapsix/alpine-java:8",
    dockerUpdateLatest := true,
    dockerRepository := registryWithRepository("sky")
  )

  def registryWithRepository(repository: String) = {
    val registry = sys.env.get("DOCKER_REGISTRY_HOST").map(_ + "/").getOrElse("")
    Some(s"$registry$repository")
  }
}

