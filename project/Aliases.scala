import sbt._

object Aliases {

  lazy val defineCommandAliases = {
    addCommandAlias("ciBuild", ";clean; test; docker:publish") ++
      addCommandAlias("ciRelease", ";clean; release with-defaults")
  }
}
