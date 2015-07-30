import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype.SonatypeKeys._
import xerial.sbt.Sonatype.sonatypeSettings
import org.scalajs.sbtplugin._
import org.scalajs.sbtplugin.cross.CrossProject
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Build extends sbt.Build {
  val buildOrganisation = "io.github.widok"
  val buildScalaVersion = "2.11.6"
  val buildScalaOptions = Seq(
    "-unchecked",
    "-deprecation",
    "-encoding", "utf8"
  )

  lazy val root = project.in(file(".")).
    aggregate(js, jvm).
    settings(
      publish := {},
      publishLocal := {}
    )

  lazy val widok = crossProject.in(file("."))
    .settings(sonatypeSettings: _*)
    .settings(
      name := "widok",
      version := "0.3.0-SNAPSHOT",

      pomExtra :=
        <url>http://widok.github.io/</url>
        <licenses>
          <license>
            <name>Apache-2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.html</url>
          </license>
        </licenses>
        <scm>
          <url>git://github.com/widok/widok.git</url>
        </scm>
        <developers>
          <developer>
            <id>tindzk</id>
            <name>Tim Nieradzik</name>
            <url>http://github.com/tindzk/</url>
          </developer>
        </developers>,

      testFrameworks += new TestFramework("minitest.runner.Framework"),

      organization := buildOrganisation,
      scalaVersion := buildScalaVersion,
      scalacOptions := buildScalaOptions,

      autoAPIMappings := true,
      apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/"))
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.monifu" %%% "minitest" % "0.12" % "test"
      ),

      /* Use io.js for faster compilation of test cases */
      scalaJSStage in Global := FastOptStage
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "org.monifu" %% "minitest" % "0.12" % "test"
      )
    )

  lazy val js = widok.js
  lazy val jvm = widok.jvm
}
