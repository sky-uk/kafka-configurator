import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{Docker => docker}
import sbt.Def
import sbt.Keys._

object Docker {

  lazy val dockerSettings: Seq[Def.Setting[_]] = Seq(
    docker / packageName := packageName.value,
    dockerBaseImage      := "alpine:3.17.1",
    dockerAliases ++= {
      val dockerTag = (tag: String) => Seq(dockerAlias.value.withTag(Option(tag)))
      if (isSnapshot.value) dockerTag("snapshot") else dockerTag("latest")
    },
    dockerRepository     := Some("skyuk"),
    dockerLabels         := Map("maintainer" -> "Sky"),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd("RUN", "apk add --no-cache openjdk11-jre")
    )
  )

}
