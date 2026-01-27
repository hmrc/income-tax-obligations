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

package uk.gov.hmrc.incometaxobligations.helpers.servicemocks

import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.incometaxobligations.constants.ReportDeadlinesIntegrationTestConstants.*
import uk.gov.hmrc.incometaxobligations.helpers.WiremockHelper
import uk.gov.hmrc.incometaxobligations.models.obligations.ObligationsErrorModel

object ViewAndChangeStub {

  def url(nino: String): String = {
    s"/income-tax-view-change/$nino/open-obligations"
  }

  def allObligationsUrl(nino: String, from: String, to: String): String = {
    s"/income-tax-view-change/$nino/obligations/from/$from/to/$to"
  }

  def stubGetAllObligations(nino: String, from: String, to: String, statusCode: String, responseBody: String): Unit = {
    val desReportDeadlinesResponse = successResponseWithStatus(nino, statusCode).toString
    WiremockHelper.stubGet(allObligationsUrl(nino, from, to), Status.OK, responseBody)
  }

  def stubGetAllObligationsError(nino: String, from: String, to: String)(status: Int, body: String): Unit = {
    WiremockHelper.stubGet(allObligationsUrl(nino, from, to), status, Json.toJson(ObligationsErrorModel(status, body)).toString)
  }

}
