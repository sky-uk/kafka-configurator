import sbt.Keys._
import sbt.{Project, State, ThisBuild, settingKey, taskKey}
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations.{runTest, setReleaseVersion => _, _}
import sbtrelease._

object Release {
  lazy val releaseSettings = Seq(
    releaseUseGlobalVersion := false,
    releaseVersionBump := sbtrelease.Version.Bump.Minor,
    releaseTagName := s"${name.value}-${version.value}",
    releaseTagComment := s"Releasing ${version.value} of module: ${name.value}",
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      releaseStepCommand(ExtraReleaseCommands.initialVcsChecksCommand),
      inquireVersions,
      setReleaseVersion,
      runClean,
      runTest,
      tagRelease,
      // TODO: build the artifact and publish it somewhere
      //      ReleaseStep(releaseStepTask(sbt.Keys.packageBin)),
      //      publishArtifacts,
      pushChanges
    )
  )

  // Override the default implementation of sbtrelease.ReleaseStateTransformations.setReleaseVersion,
  // so it doesn't write to a version.sbt file.
  lazy val setReleaseVersion: ReleaseStep = setVersionOnly(_._1)

  def setVersionOnly(selectVersion: Versions => String): ReleaseStep = { st: State =>
    val vs = st.get(ReleaseKeys.versions).getOrElse(sys.error   ("No versions are set! Was this release part executed before inquireVersions?"))
    val selected = selectVersion(vs)

    st.log.info("Setting version to '%s'." format selected)
    val useGlobal = Project.extract(st).get(releaseUseGlobalVersion)
    val versionStr = (if (useGlobal) globalVersionString else versionString) format selected

    reapply(Seq(
      if (useGlobal) version in ThisBuild := selected
      else version := selected
    ), st)
  }
}
