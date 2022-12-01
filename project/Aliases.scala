import sbt._

object Aliases {

  lazy val defineCommandAliases: Seq[Def.Setting[State => State]] =
    addCommandAlias("checkFix", "scalafixAll --check OrganizeImports; scalafixAll --check") ++
      addCommandAlias("runFix", "scalafixAll OrganizeImports; scalafixAll") ++
      addCommandAlias("checkFmt", "scalafmtCheckAll; scalafmtSbtCheck") ++
      addCommandAlias("runFmt", "scalafmtAll; scalafmtSbt") ++
      addCommandAlias("ciBuild", "clean; checkFmt; checkFix; test")
}
