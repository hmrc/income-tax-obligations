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

package uk.gov.hmrc.incometaxobligations.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.incometaxobligations.connectors.ViewAndChangeConnector
import uk.gov.hmrc.incometaxobligations.connectors.hip.ITSAStatusConnector
import uk.gov.hmrc.incometaxobligations.models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseModel, ITSAStatusResponseNotFound, StatusDetail}
import uk.gov.hmrc.incometaxobligations.repositories.ITSAStatusRepository
import uk.gov.hmrc.incometaxobligations.utils.TestSupport
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.Future

class ITSAStatusServiceSpec extends TestSupport {
  
  trait Setup {
    val itsaStatusConnector: ITSAStatusConnector = mock(classOf[ITSAStatusConnector])
    val viewAndChangeConnector: ViewAndChangeConnector = mock(classOf[ViewAndChangeConnector])
    val repository: ITSAStatusRepository = mock(classOf[ITSAStatusRepository])
    
    val service: ITSAStatusService = ITSAStatusService(
      repository,
      itsaStatusConnector,
      viewAndChangeConnector
    )
  }
  
  val testNino = "KC432342C"
  val taxYear = "2024"
  val statusDetail = new StatusDetail("12/12/2024", "Completed", "Submitted and finished")
  val responseModel = new ITSAStatusResponseModel(taxYear, Some(List(statusDetail)))
  val dataKey = DataKey[List[ITSAStatusResponseModel]]("ITSA_Status")
  
  "getITSAStatus" when {
    "a model exists in the cache" should {
      "return the model" in new Setup {
        when(repository.getCache[List[ITSAStatusResponseModel]](any())(any())(any()))
          .thenReturn(Future.successful(Some(List(responseModel))))
        val result: Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = service.getITSAStatus(testNino, taxYear, false, false)(hc, ec)
        await(result) shouldBe Right(List(responseModel))
      }
    }
    "no model exists in the cache" should {
      "return a model if call to hip succeeds" in new Setup {
        when(repository.getCache[List[ITSAStatusResponseModel]](any())(any())(any()))
          .thenReturn(Future.successful(None))
        when(itsaStatusConnector.getITSAStatus(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Right(List(responseModel))))
        val result: Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = service.getITSAStatus(testNino, taxYear, false, false)(hc, ec)
        await(result) shouldBe Right(List(responseModel))
      }
      "return a model if call to hip fails" in new Setup {
        when(repository.getCache[List[ITSAStatusResponseModel]](any())(any())(any()))
          .thenReturn(Future.successful(None))
        when(itsaStatusConnector.getITSAStatus(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Left(ITSAStatusResponseNotFound(NOT_FOUND, "No record found"))))
        when(viewAndChangeConnector.getITSAStatus(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Right(List(responseModel))))
        val result: Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = service.getITSAStatus(testNino, taxYear, false, false)(hc, ec)
        await(result) shouldBe Right(List(responseModel))
      }
    }
  }
}
