import sbtunidoc.Plugin.UnidocKeys._
import ScoverageSbtPlugin._

lazy val buildSettings = Seq(
  organization := "com.github.finagle",
  version := "0.9.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint"
)

val testDependencies = Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.5",
  "org.scalatest" %% "scalatest" % "2.2.5"
)

val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.2.5",
    "com.twitter" %% "finagle-httpx" % "6.29.0",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
  ) ++ testDependencies.map(_ % "test"),
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Seq.empty
    }
  ),
  scalacOptions in (Compile, console) := compilerOptions :+ "-Yrepl-class-based",
  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(Wart.NoNeedForMonad, Wart.Null, Wart.Nothing, Wart.DefaultArguments)
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/finagle/finch")),
  autoAPIMappings := true,
  apiURL := Some(url("https://finagle.github.io/finch/docs/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/finagle/finch"),
      "scm:git:git@github.com:finagle/finch.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <id>vkostyukov</id>
        <name>Vladimir Kostyukov</name>
        <url>http://vkostyukov.ru</url>
      </developer>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
        <url>https://meta.plasm.us/</url>
      </developer>
    </developers>
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {}
)

lazy val allSettings = baseSettings ++ buildSettings ++ publishSettings

lazy val docSettings = site.settings ++ ghpages.settings ++ unidocSettings ++ Seq(
  site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "docs"),
  git.remoteRepo := s"git@github.com:finagle/finch.git",
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmarks, petstore, jsonTest)
)

lazy val root = project.in(file("."))
  .settings(moduleName := "finch")
  .settings(allSettings)
  .settings(docSettings)
  .settings(noPublish)
  .settings(
    initialCommands in console :=
      """
        |import io.finch._
        |import io.finch.circe._
        |import io.finch.request._
        |import io.finch.request.items._
        |import com.twitter.util.Future
        |import com.twitter.finagle.Service
        |import com.twitter.finagle.httpx.{Request, Response, Status}
      """.stripMargin
  )
  .aggregate(core, argonaut, jackson, json4s, circe, benchmarks, petstore, test, jsonTest, oauth2)
  .dependsOn(core, circe)

lazy val core = project
  .settings(moduleName := "finch-core")
  .settings(allSettings)

lazy val test = project
  .settings(moduleName := "finch-test")
  .settings(allSettings)
  .settings(coverageExcludedPackages := "io\\.finch\\.test\\..*")
  .settings(
    libraryDependencies ++= testDependencies
  )
  .dependsOn(core)

lazy val jsonTest = project.in(file("json-test"))
  .settings(moduleName := "finch-json-test")
  .settings(allSettings)
  .settings(noPublish)
  .settings(coverageExcludedPackages := "io\\.finch\\.test\\..*")
  .settings(
    libraryDependencies ++= "io.argonaut" %% "argonaut" % "6.1" +: testDependencies
  )
  .dependsOn(core)

lazy val petstore = project
    .settings(moduleName := "finch-petstore")
    .configs(IntegrationTest.extend(Test))
    .settings(allSettings)
    .settings(noPublish)
    .settings(Defaults.itSettings)
    .settings(parallelExecution in IntegrationTest := false)
    .settings(coverageExcludedPackages := "io\\.finch\\.petstore\\.PetstoreApp.*")
    .dependsOn(core, argonaut, test % "test,it")

lazy val argonaut = project
  .settings(moduleName := "finch-argonaut")
  .settings(allSettings)
  .settings(libraryDependencies += "io.argonaut" %% "argonaut" % "6.1")
  .dependsOn(core, jsonTest % "test")

lazy val jackson = project
  .settings(moduleName := "finch-jackson")
  .settings(allSettings)
  .settings(libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.3")
  .dependsOn(core, jsonTest % "test")

lazy val json4s = project
  .settings(moduleName := "finch-json4s")
  .settings(allSettings)
  .settings(libraryDependencies ++= Seq(
    "org.json4s" %% "json4s-jackson" % "3.2.11",
    "org.json4s" %% "json4s-ext" % "3.2.11")
  )
  .dependsOn(core, jsonTest % "test")

lazy val circe = project
  .settings(moduleName := "finch-circe")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.1.1",
      "io.circe" %% "circe-jawn" % "0.1.1",
      "io.circe" %% "circe-generic" % "0.1.1" % "test"
    )
  )
  .dependsOn(core, jsonTest % "test")

lazy val oauth2 = project
  .settings(moduleName := "finch-oauth2")
  .settings(allSettings)
  .settings(libraryDependencies ++= Seq(
    "com.github.finagle" %% "finagle-oauth2" % "0.1.4",
    "org.mockito" % "mockito-all" % "1.10.19" % "test"
  ))
  .dependsOn(core)

lazy val benchmarks = project
  .settings(moduleName := "finch-benchmarks")
  .enablePlugins(JmhPlugin)
  .settings(allSettings)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(parallelExecution in IntegrationTest := false)
  .settings(coverageExcludedPackages := "io\\.finch\\.benchmarks\\.service\\.UserServiceBenchmark")
  .settings(libraryDependencies += "io.circe" %% "circe-generic" % "0.1.1")
  .settings(
    javaOptions in run ++= Seq(
      "-Djava.net.preferIPv4Stack=true",
      "-XX:+AggressiveOpts",
      "-XX:+UseParNewGC",
      "-XX:+UseConcMarkSweepGC",
      "-XX:+CMSParallelRemarkEnabled",
      "-XX:+CMSClassUnloadingEnabled",
      "-XX:ReservedCodeCacheSize=128m",
      "-XX:MaxPermSize=1024m",
      "-Xss8M",
      "-Xms512M",
      "-XX:SurvivorRatio=128",
      "-XX:MaxTenuringThreshold=0",
      "-Xss8M",
      "-Xms512M",
      "-Xmx2G",
      "-server"
    )
  )
  .dependsOn(
    core,
    argonaut,
    jackson,
    json4s,
    circe,
    test % "it"
  )
