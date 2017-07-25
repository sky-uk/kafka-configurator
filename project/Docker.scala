import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{Docker => docker}
import sbt.Def
import sbt.Keys._

object Docker {

  lazy val dockerSettings = Seq(
    packageName in docker := packageName.value,
    dockerBaseImage := "openjdk:8u131-jre-alpine",
    dockerUpdateLatest := updateLatest.value,
    dockerRepository := Some("skyuk"),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd("RUN", "apk update && apk add bash")
    )
  )

  def updateLatest = Def.setting {
    if (!version.value.contains("SNAPSHOT")) true
    else false
  }
}

