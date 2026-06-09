/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.incometaxobligations.services

import org.mockito.ArgumentMatchers.{any, eq as matches}
import org.mockito.Mockito.{mock, when as stub}
import play.api.test.Helpers.*
import uk.gov.hmrc.incometaxobligations.connectors.ObligationsConnector
import uk.gov.hmrc.incometaxobligations.constants.BaseTestConstants.*
import uk.gov.hmrc.incometaxobligations.constants.ObligationsTestConstants.*
import uk.gov.hmrc.incometaxobligations.models.obligations.ObligationsResponseModel
import uk.gov.hmrc.incometaxobligations.utils.TestSupport

import scala.concurrent.Future
import uk.gov.hmrc.incometaxobligations.models.obligations.ObligationsErrorModel
import uk.gov.hmrc.incometaxobligations.models.obligations.ObligationsModel
import uk.gov.hmrc.http.HeaderCarrier


class ObligationsServiceSpec extends TestSupport {

  val obligationsConnector: ObligationsConnector = mock(classOf[ObligationsConnector])
  val service = ObligationsService(obligationsConnector)

  "getOpenObligations" when {
    "the call to DES is successful should return the success model" in {
        stubSuccessfulObligationsCall()

        val result = await(service.getOpenObligations(testNino))

        result shouldBe a [ObligationsModel]
        result shouldBe testObligations
    }
    "the call to DES fails should return the error model" in {
        stubFailedObligationsCall()

        val result = await(service.getOpenObligations(testNino))

        result shouldBe a [ObligationsErrorModel]
        result shouldBe testReportDeadlinesError
    }
  }

  "getAllObligationsWithinDateRange" when {
    val dateFrom = "2023-01-01"
    val dateTo = "2023-12-31"
    "the call to DES is successful should return the success model" in {
        stubSuccessfulObligationsCall()

        val result = await(service.getAllObligationsWithinDateRange(testNino, dateFrom, dateTo))

        result shouldBe a [ObligationsModel]
        result shouldBe testObligations
    }
    "the call to DES fails should return the error model" in {
        stubFailedObligationsCall()

        val result = await(service.getAllObligationsWithinDateRange(testNino, dateFrom, dateTo))

        result shouldBe a [ObligationsErrorModel]
        result shouldBe testReportDeadlinesError
    }
  }

  "getFulfilledObligations" when {
    "the call to DES is successful should return the success model" in {
        stubSuccessfulObligationsCall()

        val result = await(service.getFulfilledObligations(testNino))

        result shouldBe a [ObligationsModel]
        result shouldBe testObligations
    }
    "the call to DES fails should return the error model" in {
        stubFailedObligationsCall()

        val result = await(service.getFulfilledObligations(testNino))

        result shouldBe a [ObligationsErrorModel]
        result shouldBe testReportDeadlinesError
    }
  }
  private def stubSuccessfulObligationsCall() =
    stub(obligationsConnector.getOpenObligations(matches(testNino))(any[HeaderCarrier]))
      .thenReturn(Future.successful(testObligations))
    stub(obligationsConnector.getAllObligationsWithinDateRange(matches(testNino), any[String], any[String])(any[HeaderCarrier]))
      .thenReturn(Future.successful(testObligations))
    stub(obligationsConnector.getFulfilledObligations(matches(testNino))(any[HeaderCarrier]))
      .thenReturn(Future.successful(testObligations))
  
  private def stubFailedObligationsCall() =
    stub(obligationsConnector.getOpenObligations(matches(testNino))(any[HeaderCarrier]))
      .thenReturn(Future.successful(testReportDeadlinesError))
    stub(obligationsConnector.getAllObligationsWithinDateRange(matches(testNino), any[String], any[String])(any[HeaderCarrier]))
      .thenReturn(Future.successful(testReportDeadlinesError))
    stub(obligationsConnector.getFulfilledObligations(matches(testNino))(any[HeaderCarrier]))
      .thenReturn(Future.successful(testReportDeadlinesError))

}
