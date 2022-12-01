import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{Docker => docker}
import sbt.Def
import sbt.Keys._

object Docker {

  lazy val dockerSettings = Seq(
    docker / packageName := packageName.value,
    dockerBaseImage      := "alpine:3.13.0",
    dockerUpdateLatest   := updateLatest.value,
    dockerRepository     := Some("skyuk"),
    dockerLabels         := Map("maintainer" -> "Sky"),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd("RUN", "apk add --no-cache openjdk11-jre")
    )
  )

  def updateLatest = Def.setting {
    !version.value.contains("SNAPSHOT")
  }
}
