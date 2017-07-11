import com.typesafe.sbt.packager.docker._

object Docker {


    lazy val dockerSettings = Seq(
//      val confdUrl = "https://github.com/kelseyhightower/confd/releases/download/v$CONFD_VERSION/confd-$CONFD_VERSION-linux-amd64"
      Cmd("FROM", "anapsix/alpine-java:8")
//      env(("CONFD_VERSION", "0.11.0"))
//      runRaw(
//        s"""apk add --update curl && \\
//           |rm -rf /var/cache/apk/* && \\
//           |curl -L $confdUrl > /usr/local/bin/confd && \\
//           |chmod +x /usr/local/bin/confd && \\
//           |apk del curl""".stripMargin
//      )
//      run("chmod", "+x", "/usr/local/bin/confd")
//      copy(baseDirectory(_ / "kafka-topics" / "confd").value, "/etc/confd")
//      env(("CONFD_BACKEND", "env"), ("CONFD_PREFIX", "/"))
//      copy(baseDirectory(_ / "kafka-topics" / "start.sh").value, "start.sh")
//      run("chmod", "+x", "start.sh")
//      entryPoint("./start.sh")
    )
//    imageNames in docker := Seq('v' + version.value, "latest").map(imageNameFrom("sky/kafka-configurator", _)),
//    buildOptions in docker := BuildOptions(cache = false)

//  def imageNameFrom(repository: String, tagName: String) =
//    new ImageName(
//      registry = sys.env.get("DOCKER_REGISTRY_HOST"),
//      repository = repository,
//      tag = Some(tagName)
//    )
}
