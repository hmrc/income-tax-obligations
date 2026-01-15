import play.core.PlayVersion
import play.sbt.PlayImport
import play.sbt.routes.RoutesKeys
import sbt.*
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "income-tax-business-details"
val currentScalaVersion = "3.3.6"
ThisBuild / majorVersion := 0

lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val microservice = Project("income-tax-obligations", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := currentScalaVersion)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    retrieveManaged := true
  )
  .settings(CodeCoverageSettings.settings: _*)
  .settings(defaultSettings(): _*)
  .settings(RoutesKeys.routesImport -= "controllers.Assets.Asset")
  .settings(scalacOptions += "-Xfatal-warnings")
  .settings(scalacOptions += "-deprecation:false")
  .settings(
    Test / Keys.fork := true,
    scalaVersion := currentScalaVersion,
    scalacOptions += "-Wconf:src=routes/.*:s",
    Test / javaOptions += "-Dlogger.resource=logback-test.xml")
  .settings(
    Keys.fork := false)
  .settings(resolvers ++= Seq(
    MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2"),
    Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
  ))
  .settings(ThisBuild / scalacOptions += "-Wconf:msg=Flag.*repeatedly:s")
  .settings(
    scalacOptions --= Seq("-Wunused", "-Wunused:all"),
    scalacOptions += "-deprecation",
    Test / scalacOptions ++= Seq(
      "-Wunused:imports",
      "-Wunused:params",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:privates"
    ),
    PlayKeys.playDefaultPort := 9089
  )
lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .enablePlugins(play.sbt.PlayScala)
  .settings(
    publish / skip := true
  )
  .settings(scalaVersion := currentScalaVersion)
  .settings(
    testForkedParallel := true
  )
  .settings(libraryDependencies ++= AppDependencies.it)
  .settings(ThisBuild / scalacOptions += "-Wconf:msg=Flag.*repeatedly:s")
