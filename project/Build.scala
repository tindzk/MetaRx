import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype.sonatypeSettings
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._

object Build extends sbt.Build {
  val SharedSettings = Seq(
    name := "MetaRx",
    organization := "pl.metastack",
    scalaVersion := "2.11.7",
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-encoding", "utf8"
    )
  )

  lazy val root = project.in(file("."))
    .aggregate(js, jvm)
    .settings(SharedSettings: _*)
    .settings(publishArtifact := false)

  lazy val metaRx = crossProject.in(file("."))
    .settings(SharedSettings: _*)
    .settings(sonatypeSettings: _*)
    .settings(
      pomExtra :=
        <url>https://github.com/MetaStack-pl/MetaRx</url>
        <licenses>
          <license>
            <name>Apache-2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.html</url>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:MetaStack-pl/MetaRx.git</url>
        </scm>
        <developers>
          <developer>
            <id>tindzk</id>
            <name>Tim Nieradzik</name>
            <url>http://github.com/tindzk/</url>
          </developer>
        </developers>,

      testFrameworks += new TestFramework("minitest.runner.Framework"),
      autoAPIMappings := true,
      apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/"))
    )
    .jsSettings(
      libraryDependencies += "org.monifu" %%% "minitest" % "0.13" % "test",

      /* Use io.js for faster compilation of test cases */
      scalaJSStage in Global := FastOptStage
    )
    .jvmSettings(
      libraryDependencies += "org.monifu" %% "minitest" % "0.13" % "test"
    )

  lazy val upickle = crossProject
    .crossType(CrossType.Pure)
    .in(file("upickle"))
    .settings(SharedSettings: _*)
    .settings(name := "metarx-upickle")
    .dependsOn(metaRx)
    .settings(
      testFrameworks += new TestFramework("minitest.runner.Framework")
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "upickle" % "0.3.6",
        "org.monifu" %%% "minitest" % "0.13" % "test"
      )
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "upickle" % "0.3.6",
        "org.monifu" %% "minitest" % "0.13" % "test"
      )
    )

  lazy val js = metaRx.js
  lazy val jvm = metaRx.jvm

  lazy val upickleJS = upickle.js
  lazy val upickleJVM = upickle.jvm

  lazy val manual = project.in(file("manual"))
    .dependsOn(jvm)
    .enablePlugins(BuildInfoPlugin)
    .settings(SharedSettings: _*)
    .settings(
      publishArtifact := false,
      libraryDependencies ++= Seq(
        "pl.metastack" %% "metadocs" % "0.1.1-SNAPSHOT",
        "org.eclipse.jgit" % "org.eclipse.jgit" % "4.1.1.201511131810-r"),
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "pl.metastack.metarx",
      name := "MetaRx manual")
}
