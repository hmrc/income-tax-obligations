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
import org.mockito.Mockito.{mock, when}
import play.api.test.Helpers.*
import uk.gov.hmrc.incometaxobligations.connectors.{ObligationsConnector, ViewAndChangeConnector}
import uk.gov.hmrc.incometaxobligations.constants.BaseTestConstants.*
import uk.gov.hmrc.incometaxobligations.constants.ObligationsTestConstants.*
import uk.gov.hmrc.incometaxobligations.models.obligations.ObligationsResponseModel
import uk.gov.hmrc.incometaxobligations.utils.TestSupport

import scala.concurrent.Future


class ObligationsServiceSpec extends TestSupport {

  trait Setup {
    val obligationsConnector: ObligationsConnector = mock(classOf[ObligationsConnector])
    val viewAndChangeConnector: ViewAndChangeConnector = mock(classOf[ViewAndChangeConnector])

    val service = new ObligationsService(
      obligationsConnector,
      viewAndChangeConnector
    )
  }

  "getOpenObligations" when {
    s"the call to DES is successful" should {
      s"return the success model" in new Setup {
        when(obligationsConnector.getOpenObligations(matches(testNino))(any()))
          .thenReturn(Future.successful(testObligations))

        val result: Future[ObligationsResponseModel] = service.getOpenObligations(testNino)(hc, ec)

        await(result) shouldBe testObligations
      }
    }

    "the call to DES is unsuccessful" should {
      "call the view and change connector and return its response" in new Setup {
        when(obligationsConnector.getOpenObligations(matches(testNino))(any()))
          .thenReturn(Future.successful(testReportDeadlinesError))
        when(viewAndChangeConnector.getOpenObligations(matches(testNino))(any()))
          .thenReturn(Future.successful(testObligations))

        val result: Future[ObligationsResponseModel] = service.getOpenObligations(testNino)(hc, ec)

        await(result) shouldBe testObligations
      }
    }
  }

  "getAllObligationsWithinDateRange" when {
    val dateFrom = "2023-01-01"
    val dateTo = "2023-12-31"
    s"the call to DES is successful" should {
      s"return the success model" in new Setup {
        when(obligationsConnector.getAllObligationsWithinDateRange(matches(testNino), matches(dateFrom), matches(dateTo))(any()))
          .thenReturn(Future.successful(testObligations))

        val result: Future[ObligationsResponseModel] = service.getAllObligationsWithinDateRange(testNino, dateFrom, dateTo)(hc, ec)

        await(result) shouldBe testObligations
      }
    }

    "the call to DES is unsuccessful" should {
      "call the view and change connector and return its response" in new Setup {
        when(obligationsConnector.getAllObligationsWithinDateRange(matches(testNino), matches(dateFrom), matches(dateTo))(any()))
          .thenReturn(Future.successful(testReportDeadlinesError))
        when(viewAndChangeConnector.getAllObligationsWithinDateRange(matches(testNino), matches(dateFrom), matches(dateTo))(any()))
          .thenReturn(Future.successful(testObligations))

        val result: Future[ObligationsResponseModel] = service.getAllObligationsWithinDateRange(testNino, dateFrom, dateTo)(hc, ec)

        await(result) shouldBe testObligations
      }
    }
  }

}
