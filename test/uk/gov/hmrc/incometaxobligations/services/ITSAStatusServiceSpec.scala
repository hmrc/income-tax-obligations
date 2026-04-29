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
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.incometaxobligations.connectors.ViewAndChangeConnector
import uk.gov.hmrc.incometaxobligations.connectors.hip.ITSAStatusConnector
import uk.gov.hmrc.incometaxobligations.connectors.itsastatus.OptOutUpdateRequestModel.{OptOutUpdateRequest, OptOutUpdateResponseFailure, OptOutUpdateResponseSuccess}
import uk.gov.hmrc.incometaxobligations.models.itsaStatus.*
import uk.gov.hmrc.incometaxobligations.repositories.ITSAStatusRepository
import uk.gov.hmrc.incometaxobligations.utils.TestSupport
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.Future

class ITSAStatusServiceSpec extends TestSupport with BeforeAndAfterEach{

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

  override def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
    super.beforeEach()
  }

  case class TestCase(futureYears: Boolean, history: Boolean) {
    val dataKeyName: String => String = taxYear => (futureYears, history) match {
      case (true, true) => s"ITSA_Status_${taxYear}_FutureAndHistory"
      case (true, false) => s"ITSA_Status_${taxYear}_Future"
      case (false, true) => s"ITSA_Status_${taxYear}_History"
      case (false, false) => s"ITSA_Status_$taxYear"
    }
  }

  val testCases: List[TestCase] = List(
    TestCase(futureYears = true, history = true),
    TestCase(futureYears = true, history = false),
    TestCase(futureYears = false, history = true),
    TestCase(futureYears = false, history = false)
  )

  "Calling the ITSAStatusController.getITSAStatus method" when {
    "authorised with a valid request" that {
      testCases.foreach(testCase => {
        val futureYears = testCase.futureYears
        val history = testCase.history
        val dataKey = DataKey[List[ITSAStatusResponseModel]](testCase.dataKeyName(taxYear))
        s"has parameters futureYears = $futureYears and history = $history" should {
          "return a list of status details from the cache" when {
            "the cache exists" in {
              await(repository.updateCache(id)(dataKey, List(responseModel)))

              val result = service.getITSAStatus(id, taxYear, futureYears, history)
              await(result) shouldBe Right(List(responseModel))
            }
          }

          "return NOT_FOUND from the cache" when {
            "the cache exists but is an empty list" in {
              await(repository.updateCache(id)(dataKey, List()))

              val result = service.getITSAStatus(id, taxYear, futureYears, history)
              await(result) shouldBe Left(ITSAStatusResponseNotFound(NOT_FOUND, "No ITSA Status found"))
            }
          }
          "return a list of status details from HIP and update the cache" when {
            "no cache exists and hip call is successful" in {
              when(itsaStatusConnector.getITSAStatus(any(), any(), any(), any())(any()))
                .thenReturn(Future.successful(Right(List(responseModel))))

              val result = service.getITSAStatus(id, taxYear, futureYears, history)
              await(result) shouldBe Right(List(responseModel))

              val cacheData = await(repository.getCache[List[ITSAStatusResponseModel]](id)(dataKey))
              cacheData shouldBe Some(List(responseModel))
            }
          }
          "return NOT_FOUND and update the cache with an empty list" when {
            "no cache exists and HIP returns a NOT_FOUND" in {
              when(itsaStatusConnector.getITSAStatus(any(), any(), any(), any())(any()))
                .thenReturn(Future.successful(Left(ITSAStatusResponseNotFound(NOT_FOUND, "No record found"))))
              val result = service.getITSAStatus(id, taxYear, futureYears, history)
              await(result) shouldBe Left(ITSAStatusResponseNotFound(NOT_FOUND, "No record found"))
              val cacheData = await(repository.getCache[List[ITSAStatusResponseModel]](id)(dataKey))
              cacheData shouldBe Some(List())
            }
          }

          "return a list of status details from view and change without updating the cache" when {
            "no cache exists and HIP call fails with an error other than NOT_FOUND" in {
              when(itsaStatusConnector.getITSAStatus(any(), any(), any(), any())(any()))
                .thenReturn(Future.successful(Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "internal error"))))

              when(viewAndChangeConnector.getITSAStatus(any(), any(), any(), any())(any()))
                .thenReturn(Future.successful(Right(List(responseModel))))

              val result = service.getITSAStatus(id, taxYear, futureYears, history)
              await(result) shouldBe Right(List(responseModel))

              val cacheData = await(repository.getCache[List[ITSAStatusResponseModel]](id)(dataKey))
              cacheData shouldBe None
            }
          }
        }
      })
    }
  }

  "Calling the ITSAStatusController.requestOptOutForTaxYear method" when {
    "authorised with a valid request" should {
      "delete the cache and return a successful response from HIP" in {
        val optOutUpdateRequest = OptOutUpdateRequest(taxYear, "Reason")
        when(itsaStatusConnector.requestOptOutForTaxYear(any(), any())(any()))
          .thenReturn(Future.successful(OptOutUpdateResponseSuccess("Successfully opted out")))

        val result = service.requestOptOutForTaxYear(id, optOutUpdateRequest)
        await(result) shouldBe OptOutUpdateResponseSuccess("Successfully opted out")

        val dataKeyNames = List(
          s"ITSA_Status_${taxYear}_FutureAndHistory",
          s"ITSA_Status_${taxYear}_Future",
          s"ITSA_Status_${taxYear}_History",
          s"ITSA_Status_$taxYear"
        )
        dataKeyNames.foreach { keyName =>
          val cacheData = await(repository.getCache[List[ITSAStatusResponseModel]](id)(DataKey[List[ITSAStatusResponseModel]](keyName)))
          cacheData shouldBe None
        }
      }

      "delete the cache and return a successful response from V&C when HIP fails" in {
        val optOutUpdateRequest = OptOutUpdateRequest(taxYear, "Reason")
        when(itsaStatusConnector.requestOptOutForTaxYear(any(), any())(any()))
          .thenReturn(Future.successful(OptOutUpdateResponseFailure("corrId", 400, List.empty)))

        when(viewAndChangeConnector.requestOptOutForTaxYear(any(), any())(any()))
          .thenReturn(Future.successful(OptOutUpdateResponseSuccess("Successfully opted out")))
        
        val result = service.requestOptOutForTaxYear(id, optOutUpdateRequest)
        await(result) shouldBe OptOutUpdateResponseSuccess("Successfully opted out")

        val dataKeyNames = List(
          s"ITSA_Status_${taxYear}_FutureAndHistory",
          s"ITSA_Status_${taxYear}_Future",
          s"ITSA_Status_${taxYear}_History",
          s"ITSA_Status_$taxYear"
        )
        dataKeyNames.foreach { keyName =>
          val cacheData = await(repository.getCache[List[ITSAStatusResponseModel]](id)(DataKey[List[ITSAStatusResponseModel]](keyName)))
          cacheData shouldBe None
        }
      }
    }
  }
}
