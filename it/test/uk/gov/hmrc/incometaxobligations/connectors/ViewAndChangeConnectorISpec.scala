/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.incometaxobligations.connectors

import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.incometaxobligations.connectors.itsastatus.OptOutUpdateRequestModel.{OptOutUpdateRequest, OptOutUpdateResponseFailure, OptOutUpdateResponseSuccess, optOutUpdateReason}
import uk.gov.hmrc.incometaxobligations.constants.BaseIntegrationTestConstants.testNino
import uk.gov.hmrc.incometaxobligations.constants.ITSAStatusIntegrationTestConstants.{successITSAStatusResponseModel, taxYear, taxableEntityId}
import uk.gov.hmrc.incometaxobligations.constants.ReportDeadlinesIntegrationTestConstants.*
import uk.gov.hmrc.incometaxobligations.helpers.servicemocks.ViewAndChangeStub.updateUrl
import uk.gov.hmrc.incometaxobligations.helpers.{ComponentSpecBase, WiremockHelper}
import uk.gov.hmrc.incometaxobligations.models.itsaStatus.ITSAStatusResponseError
import uk.gov.hmrc.incometaxobligations.models.obligations.ObligationsErrorModel

class ViewAndChangeConnectorISpec extends ComponentSpecBase {

  val connector: ViewAndChangeConnector = app.injector.instanceOf[ViewAndChangeConnector]

  val dateFrom = "2020-04-06"
  val dateTo = "2021-04-05"
  val getOpenObligationsUrl = s"/income-tax-view-change/$testNino/open-obligations"
  val getAllObligationsDateRangeUrl = s"/income-tax-view-change/$testNino/obligations/from/$dateFrom/to/$dateTo"
  val getITSAStatusUrl = s"/income-tax-view-change/itsa-status/status/$taxableEntityId/$taxYear?futureYears=true&history=true"

  val request: OptOutUpdateRequest = OptOutUpdateRequest(taxYear = "19-20", updateReason = "ITSA status update reason")

  ".getOpenObligations() is called" when {

    "the response is a 200 - OK" should {

      "return a valid obligations model when successfully retrieved" in {
        val responseBody = Json.toJson(obligationsModel).toString()
        WiremockHelper.stubGet(getOpenObligationsUrl, OK, responseBody)
        val result = connector.getOpenObligations(testNino).futureValue

        result shouldBe obligationsModel
      }

      "return an ObligationsErrorModel when an ObligationsErrorModel is returned" in {
        val errorModel = ObligationsErrorModel(NOT_FOUND, "No record found")
        val responseBody = Json.toJson(errorModel).toString()
        WiremockHelper.stubGet(getOpenObligationsUrl, NOT_FOUND, responseBody)
        val result = connector.getOpenObligations(testNino).futureValue

        result shouldBe errorModel
      }

      "return an ObligationsErrorModel when an none ObligationsErrorModel is returned" in {
        val responseBody = testDeadlineFromJson().toString()
        WiremockHelper.stubGet(getOpenObligationsUrl, INTERNAL_SERVER_ERROR, responseBody)
        val result = connector.getOpenObligations(testNino).futureValue

        result shouldBe ObligationsErrorModel(INTERNAL_SERVER_ERROR, "Unexpected failed future")
      }
    }
  }

  ".getAllObligationsWithinDateRange() is called" when {

    "the response is a 200 - OK" should {

      "return a valid obligations model when successfully retrieved" in {

        val responseBody: String = Json.toJson(obligationsModel).toString()
        WiremockHelper.stubGet(getAllObligationsDateRangeUrl, OK, responseBody)
        val result = connector.getAllObligationsWithinDateRange(testNino, dateFrom, dateTo).futureValue

        result shouldBe obligationsModel
      }

      "return an ObligationsErrorModel when an ObligationsErrorModel is returned" in {
        val errorModel =ObligationsErrorModel(NOT_FOUND, "No record found")
        val responseBody = Json.toJson(errorModel).toString()
        WiremockHelper.stubGet(getAllObligationsDateRangeUrl, NOT_FOUND, responseBody)
        val result = connector.getAllObligationsWithinDateRange(testNino, dateFrom, dateTo).futureValue

        result shouldBe errorModel
      }

      "return an ObligationsErrorModel when an none ObligationsErrorModel is returned" in {
        val responseBody = testDeadlineFromJson().toString()
        WiremockHelper.stubGet(getAllObligationsDateRangeUrl, NOT_FOUND, responseBody)
        val result = connector.getAllObligationsWithinDateRange(testNino, dateFrom, dateTo).futureValue

        result shouldBe ObligationsErrorModel(INTERNAL_SERVER_ERROR, "Unexpected failed future")
      }
    }
  }

  ".getITSAStatus() is called" when {
    "the response is a 200 - OK" should {
      "return a valid ITSAStatus model" in {
        val responseBody = Json.toJson(List(successITSAStatusResponseModel)).toString()
        WiremockHelper.stubGet(getITSAStatusUrl, OK, responseBody)
        val result = connector.getITSAStatus(taxableEntityId, taxYear, true, true).futureValue

        result shouldBe Right(List(successITSAStatusResponseModel))
      }
      "return an ITSAStatusErrorModel when an ITSAStatusErrorModel is returned" in {
        val errorModel = ITSAStatusResponseError(NOT_FOUND, "No record found")
        val responseBody = Json.toJson(errorModel).toString()
        WiremockHelper.stubGet(getITSAStatusUrl, NOT_FOUND, responseBody)
        val result = connector.getITSAStatus(taxableEntityId, taxYear, true, true).futureValue

        result shouldBe Left(errorModel)
      }
      "return an ITSAStatusErrorModel when an none ITSAStatusErrorModel is returned" in {
        val responseBody = testDeadlineFromJson().toString()
        WiremockHelper.stubGet(getITSAStatusUrl, NOT_FOUND, responseBody)
        val result = connector.getITSAStatus(taxableEntityId, taxYear, true, true).futureValue

        result shouldBe Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Unexpected failed future"))
      }
    }
  }
  ".requestOptOutForTaxYear() is called" when {
    "return an OptOutUpdateResponseSuccess if the response is a 204 - NO_CONTENT" in {
      val response = OptOutUpdateResponseSuccess(taxableEntityId)
      val optOutUpdateRequest = OptOutUpdateRequest(taxYear, optOutUpdateReason)
      WiremockHelper.stubPutWithHeaders(updateUrl,  NO_CONTENT, Json.toJson(response).toString, Map("correlationId" -> taxableEntityId))
      val result = connector.requestOptOutForTaxYear(taxableEntityId, request).futureValue
      result shouldBe response
    }
    "return an OptOutUpdateResponseFailure if the response is not 204 - NO_CONTENT" in {
      val expectedResponse = Json.toJson(OptOutUpdateResponseFailure.defaultFailure(taxableEntityId)).toString()
      val headers = Map("correlationId" -> taxableEntityId)
      WiremockHelper.stubPutWithHeaders(updateUrl, INTERNAL_SERVER_ERROR, expectedResponse, headers)
      val result = connector.requestOptOutForTaxYear(taxableEntityId, request).futureValue

      result shouldBe OptOutUpdateResponseFailure.defaultFailure(taxableEntityId)
    }
  }
}
