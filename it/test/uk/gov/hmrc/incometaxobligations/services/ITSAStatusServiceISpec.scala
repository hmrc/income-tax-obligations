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
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers.await
import uk.gov.hmrc.incometaxobligations.connectors.ViewAndChangeConnector
import uk.gov.hmrc.incometaxobligations.connectors.hip.ITSAStatusConnector
import uk.gov.hmrc.incometaxobligations.helpers.ComponentSpecBase
import uk.gov.hmrc.incometaxobligations.models.itsaStatus.{ITSAStatusResponseModel, ITSAStatusResponseNotFound, StatusDetail}
import uk.gov.hmrc.incometaxobligations.repositories.ITSAStatusRepository
import org.mongodb.scala.gridfs.SingleObservableFuture
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.mongo.cache.DataKey
import play.api.test.Helpers.defaultAwaitTimeout

import scala.concurrent.Future

class ITSAStatusServiceISpec extends ComponentSpecBase with BeforeAndAfterEach {

  val itsaStatusConnector: ITSAStatusConnector = mock(classOf[ITSAStatusConnector])
  val viewAndChangeConnector: ViewAndChangeConnector = mock(classOf[ViewAndChangeConnector])
  val repository: ITSAStatusRepository = app.injector.instanceOf[ITSAStatusRepository]

  val service: ITSAStatusService = ITSAStatusService(
    repository,
    itsaStatusConnector,
    viewAndChangeConnector
  )

  val id = "NX1000000AB"
  val taxYear = "2024"
  val updatedTaxYear = "2025"
  val statusDetail = new StatusDetail("12/12/2024", "Completed", "Submitted and finished")
  val responseModel = new ITSAStatusResponseModel(taxYear, Some(List(statusDetail)))
  val dataKey = DataKey[List[ITSAStatusResponseModel]]("ITSA_Status")

  override def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
    super.beforeEach()
  }

  "Calling the ITSAStatusController.getITSAStatus method" when {
    "authorised with a valid request" when {
      "a model exists in the cache" should {
        "return a List of status details" in {
          isAuthorised(true)
          await(repository.updateCache(id)(dataKey, List(responseModel)))

          val result = service.getITSAStatus(id, taxYear, true, true)
          await(result) shouldBe Right(List(responseModel))
        }
      }
      "no model exists in the cache" should {
        "return a List of status details if call to hip succeeds and add to cache" in {
          isAuthorised(true)
          when(itsaStatusConnector.getITSAStatus(any(), any(), any(), any())(any()))
            .thenReturn(Future.successful(Right(List(responseModel))))

          val result = service.getITSAStatus(id, taxYear, true, true)
          await(result) shouldBe Right(List(responseModel))

          val cacheData = await(repository.getCache[List[ITSAStatusResponseModel]](id)(dataKey))
          cacheData shouldBe Some(List(responseModel))
        }
        "return a list of status details if call to hip fails without adding to cache" in {
          isAuthorised(true)
          when(itsaStatusConnector.getITSAStatus(any(), any(), any(), any())(any()))
            .thenReturn(Future.successful(Left(ITSAStatusResponseNotFound(NOT_FOUND, "No record found"))))

          when(viewAndChangeConnector.getITSAStatus(any(), any(), any(), any())(any()))
            .thenReturn(Future.successful(Right(List(responseModel))))

          val result = service.getITSAStatus(id, taxYear, true, true)
          await(result) shouldBe Right(List(responseModel))

          val cacheData = await(repository.getCache[List[ITSAStatusResponseModel]](id)(dataKey))
          cacheData shouldBe None
        }
      }
    }
  }
}
