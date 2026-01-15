import play.sbt.PlayImport
import play.sbt.PlayImport.caffeine
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.2.0"
  private val hmrcMongoVersion = "2.11.0"
  private val mockitoVersion = "5.18.0"
  private val wiremockVersion = "3.0.0-beta-7"
  private val scalaMockVersion = "7.5.0"
  private val jsoupVersion = "1.21.1"

  val compile = Seq(
    PlayImport.ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion
  )

  val test = Seq(
    "org.scalamock" %% "scalamock" % scalaMockVersion % Test,
    "org.jsoup" % "jsoup" % jsoupVersion % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion            % Test,
    "org.mockito" % "mockito-core" % mockitoVersion % Test,
    "com.github.tomakehurst" % "wiremock" % wiremockVersion % Test,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.1",
    "org.scalatest"       %% "scalatest"              % "3.2.19" % Test,
    caffeine
  )

  val it = Seq.empty
}
