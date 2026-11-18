/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.incometaxobligations.config

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.incometaxobligations.models.hip.{ITSAStatusHipApi, ObligationsHipApi}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.{Base64, UUID}

class AppConfigSpec extends AnyWordSpec with Matchers {

  private val appConfig = new AppConfig(
    new ServicesConfig(
      Configuration(
        ConfigFactory.parseString(
          """
            |appName = income-tax-obligations-test
            |microservice.services.des.url = "https://des.example"
            |microservice.services.des.environment = "test"
            |microservice.services.des.authorization-token = "des-token"
            |microservice.services.hip.host = "hip.example"
            |microservice.services.hip.port = 1234
            |microservice.services.hip.clientId = "client-id"
            |microservice.services.hip.secret = "client-secret"
            |auth.confidenceLevel = 200
            |mongodb.ttlMinutes = 5
            |microservice.feature-switch.useGetObligationsHipPlatform = true
            |""".stripMargin
        )
      )
    )
  )

  "AppConfig" should {
    "load configured values" in {
      appConfig.appName shouldBe "income-tax-obligations-test"
      appConfig.desUrl shouldBe "https://des.example"
      appConfig.desEnvironment shouldBe "test"
      appConfig.desAuthHeaders shouldBe Seq(
        "Environment" -> "test",
        "Authorization" -> appConfig.desToken
      )
      appConfig.hipUrl shouldBe "http://hip.example:1234"
      appConfig.confidenceLevel shouldBe 200
      appConfig.ttlMinutes shouldBe 5
      appConfig.useGetObligationsHipPlatform shouldBe true
    }

    "include the message type, HIP credentials, correlation ID and obligations headers" in {
      val headers = appConfig.getHIPHeaders(ObligationsHipApi, Some("obligations-request"))
      val headersByName = headers.toMap

      headers.head shouldBe ("X-Message-Type" -> "obligations-request")
      headersByName(HeaderNames.authorisation) shouldBe
        s"Basic ${Base64.getEncoder.encodeToString("client-id:client-secret".getBytes("UTF-8"))}"
      UUID.fromString(headersByName("correlationId")).toString shouldBe headersByName("correlationId")
      headersByName("X-Transmitting-System") shouldBe "HIP"
      headersByName("X-Originating-System") shouldBe "MDTP"
      headersByName("X-Receipt-Date") should fullyMatch regex """\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""".r
    }

    "omit the message type when none is provided" in {
      val headers = appConfig.getHIPHeaders(ObligationsHipApi)

      headers.map(_._1) should not contain "X-Message-Type"
    }

    "omit obligations-specific headers for the ITSA status API" in {
      val headers = appConfig.getHIPHeaders(ITSAStatusHipApi, Some("status-request"))

      headers.toMap.apply("X-Message-Type") shouldBe "status-request"
      headers.map(_._1) should not contain "X-Transmitting-System"
      headers.map(_._1) should not contain "X-Originating-System"
      headers.map(_._1) should not contain "X-Receipt-Date"
    }

    "generate a new correlation ID for each HIP request" in {
      val firstCorrelationId = appConfig.getHIPHeaders(ITSAStatusHipApi).toMap.apply("correlationId")
      val secondCorrelationId = appConfig.getHIPHeaders(ITSAStatusHipApi).toMap.apply("correlationId")

      firstCorrelationId should not be secondCorrelationId
    }
  }
}
