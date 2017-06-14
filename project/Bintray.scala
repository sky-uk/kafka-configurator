import bintray.BintrayKeys._
import sbt.Keys._
import sbt.{ThisBuild, url}

object Bintray {
  lazy val bintraySettings = Seq(
    bintrayOrganization := Some("sky-uk"),
    bintrayReleaseOnPublish in ThisBuild := false,
    bintrayRepository := "oss-maven",
    bintrayVcsUrl := Some("https://github.com/sky-uk/kafka-configurator"),
    licenses += ("BSD New", url("https://opensource.org/licenses/BSD-3-Clause"))
  )
}
