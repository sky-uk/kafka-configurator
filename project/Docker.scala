import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{Docker => docker}

object Docker {

  lazy val dockerSettings = Seq(
    packageName in docker := packageName.value,
    dockerBaseImage := "anapsix/alpine-java:8",
    dockerUpdateLatest := true,
    dockerRepository := Some("sky")
  )
}

