/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.incometaxobligations.controllers

import uk.gov.hmrc.incometaxobligations.constants.BaseIntegrationTestConstants.*
import uk.gov.hmrc.incometaxobligations.constants.ReportDeadlinesIntegrationTestConstants.*
import uk.gov.hmrc.incometaxobligations.helpers.ComponentSpecBase
import uk.gov.hmrc.incometaxobligations.helpers.servicemocks.{DesReportDeadlinesStub, ViewAndChangeStub}
import uk.gov.hmrc.incometaxobligations.models.obligations.ObligationStatus.Open
import uk.gov.hmrc.incometaxobligations.models.obligations.{ObligationsErrorModel, ObligationsModel}
import play.api.http.Status.*
import play.api.libs.json.Json

class ObligationsControllerISpec extends ComponentSpecBase {

  val from: String = "2020-04-06"
  val to: String = "2021-04-05"

  s"Calling GET ${routes.ObligationsController.getAllObligations(testNino, from, to)}" when {
    "the user is authenticated" when {
      "the request is valid" should {
        s"return $OK" when {
          "valid obligations are retrieved" in {
            isAuthorised(true)

            DesReportDeadlinesStub.stubGetDesAllObligations(testNino, from, to)

            val res = IncomeTaxViewChange.getAllObligations(testNino, from, to)

            DesReportDeadlinesStub.verifyGetDesAllObligations(testNino, from, to)

            res should have(
              httpStatus(OK),
              jsonBodyAs[ObligationsModel](obligationsModel)
            )
          }

          "valid obligations are retrieved when status is Open" in {
            isAuthorised(true)

            DesReportDeadlinesStub.stubGetDesAllObligations(testNino, from, to, Open.code)

            val res = IncomeTaxViewChange.getAllObligations(testNino, from, to)

            DesReportDeadlinesStub.verifyGetDesAllObligations(testNino, from, to)

            res should have(
              httpStatus(OK),
              jsonBodyAs[ObligationsModel](obligationsModelWithStatus(testNino, Open.name))
            )
          }

          "the call to DES fails but viewAndChange return status is Open" in {
            isAuthorised(true)
            val successResponse = obligationsModelWithStatus(testNino, Open.name)
            DesReportDeadlinesStub.stubGetDesAllObligationsError(testNino, from, to)(OK, "{}")
            ViewAndChangeStub.stubGetAllObligations(testNino, from, to, "200", Json.toJson(successResponse).toString)

            val res = IncomeTaxViewChange.getAllObligations(testNino, from, to)

            DesReportDeadlinesStub.verifyGetDesAllObligations(testNino, from, to)

            res should have(
              httpStatus(OK),
              jsonBodyAs[ObligationsModel](successResponse)
            )
          }
        }
        s"return $INTERNAL_SERVER_ERROR" when {
          "the response retrieved is invalid" in {
            isAuthorised(true)

            val error = ObligationsErrorModel(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data")
            DesReportDeadlinesStub.stubGetDesAllObligationsError(testNino, from, to)(OK, "{}")
            ViewAndChangeStub.stubGetAllObligationsError(testNino, from, to)(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data")

            val res = IncomeTaxViewChange.getAllObligations(testNino, from, to)

            DesReportDeadlinesStub.verifyGetDesAllObligations(testNino, from, to)

            res should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              jsonBodyAs[ObligationsErrorModel](error)
            )
          }
        }
        s"return the status retrieved from the call to DES when not $OK" in {
          isAuthorised(true)

          DesReportDeadlinesStub.stubGetDesAllObligationsError(testNino, from, to)(NOT_FOUND, "Error, not found")
          ViewAndChangeStub.stubGetAllObligationsError(testNino, from, to)(NOT_FOUND, "Error, not found")

          val res = IncomeTaxViewChange.getAllObligations(testNino, from, to)

          DesReportDeadlinesStub.verifyGetDesAllObligations(testNino, from, to)

          res should have(
            httpStatus(NOT_FOUND),
            jsonBodyAs[ObligationsErrorModel](ObligationsErrorModel(NOT_FOUND, "Error, not found"))
          )
        }
      }
    }
    "the user is not authenticated" should {
      s"return $UNAUTHORIZED" in {
        isAuthorised(false)

        val res = IncomeTaxViewChange.getAllObligations(testNino, from, to)

        res should have(
          httpStatus(UNAUTHORIZED),
          emptyBody
        )
      }
    }
  }

  "Calling the ReportDeadlinesController" when {
    "authorised with a valid request" should {
      "return a valid ObligationsModel for open obligations" in {

        isAuthorised(true)

        And("I wiremock stub a successful Get Report Deadlines response")
        DesReportDeadlinesStub.stubGetDesOpenReportDeadlines(testNino)

        When(s"I call GET /income-tax-obligations/$testNino/report-deadlines")
        val res = IncomeTaxViewChange.getOpenObligations(testNino)

        DesReportDeadlinesStub.verifyGetOpenDesReportDeadlines(testNino)

        Then("a successful response is returned with the correct model")

        res should have(
          httpStatus(OK),
          jsonBodyAs[ObligationsModel](obligationsModel)
        )
      }
    }

    "authorised with a invalid request" should {
      "return a ReportDeadlinesErrorModel for open obligations" in {
        isAuthorised(true)

        And("I wiremock stub an unsuccessful Get Report Deadlines response")
        DesReportDeadlinesStub.stubGetDesOpenReportDeadlinesError(testNino)

        When(s"I call GET /income-tax-obligations/$testNino/report-deadlines")
        val res = IncomeTaxViewChange.getOpenObligations(testNino)

        DesReportDeadlinesStub.verifyGetOpenDesReportDeadlines(testNino)

        Then("a unsuccessful response is returned with an error model")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
    "unauthorised" should {
      "return an error for open obligations" in {

        isAuthorised(false)

        When(s"I call GET /income-tax-obligations/$testNino/report-deadlines")
        val res = IncomeTaxViewChange.getOpenObligations(testNino)

        res should have(
          httpStatus(UNAUTHORIZED),
          emptyBody
        )
      }
    }
  }

}
