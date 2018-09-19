import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype.sonatypeSettings
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}


val scalaVersions = Seq("2.11.12", "2.12.6")

val SharedSettings = Seq(
  name := "MetaRx",
  organization := "pl.metastack",
  scalaVersion := scalaVersions.head,
  crossScalaVersions := scalaVersions,
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-encoding", "utf8"
  ),
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
    </developers>
)

lazy val root = project.in(file("."))
  .aggregate(js, jvm, upickle.js, upickle.jvm)
  .settings(SharedSettings: _*)
  .settings(publishArtifact := false)

lazy val metaRx = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("."))
  .settings(SharedSettings: _*)
  .settings(sonatypeSettings: _*)
  .settings(
    autoAPIMappings := true,
    apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/")),
    libraryDependencies +=
      "org.scalatest" %%% "scalatest" % Dependencies.ScalaTest % "test"
  )
  .jsConfigure(_.enablePlugins(com.thoughtworks.sbtScalaJsMap.ScalaJsMap))
  .jsSettings(
    scalaJSStage in Global := FastOptStage
  )

lazy val upickle = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("upickle"))
  .settings(SharedSettings: _*)
  .settings(name := "metarx-upickle")
  .dependsOn(metaRx)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % Dependencies.Upickle,
      "org.scalatest" %%% "scalatest" % Dependencies.ScalaTest % "test"
    )
  )

lazy val js = metaRx.js
lazy val jvm = metaRx.jvm

lazy val upickleJS = upickle.js
lazy val upickleJVM = upickle.jvm

lazy val manual = project.in(file("manual"))
  .dependsOn(jvm, upickleJVM)
  .enablePlugins(BuildInfoPlugin)
  .settings(SharedSettings: _*)
  .settings(
    publishArtifact := false,
    libraryDependencies ++= Seq(
      "pl.metastack" %% "metadocs" % Dependencies.MetaDocs,
      "org.eclipse.jgit" % "org.eclipse.jgit" % "4.1.1.201511131810-r"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "pl.metastack.metarx",
    name := "MetaRx manual")
