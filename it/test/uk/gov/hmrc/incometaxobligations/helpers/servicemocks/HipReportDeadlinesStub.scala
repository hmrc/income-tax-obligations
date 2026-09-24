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
import uk.gov.hmrc.incometaxobligations.constants.HipReportDeadlinesIntegrationTestConstants.*
import uk.gov.hmrc.incometaxobligations.helpers.WiremockHelper

object HipReportDeadlinesStub {

  def url(nino: String, openObligations: Boolean = true): String = {
    s"/etmp/RESTAdapter/obligation-data/nino/$nino/ITSA?status=O"
  }

  def allObligationsUrl(nino: String, from: String, to: String): String = {
    s"/etmp/RESTAdapter/obligation-data/nino/$nino/ITSA?from=$from&to=$to"
  }

  def fulfilledObligationsUrl(nino: String, fromDate: String, toDate: String): String = {
    s"/etmp/RESTAdapter/obligation-data/nino/$nino/ITSA?status=F&from=$fromDate&to=$toDate"
  }
  
  def stubGetHipOpenReportDeadlines(nino: String): Unit = {
    val hipReportDeadlinesResponse = successResponse(nino).toString
    WiremockHelper.stubGet(url(nino), Status.OK, hipReportDeadlinesResponse)
  }

  def stubGetHipOpenReportDeadlinesError(nino: String): Unit = {
    val errorResponse = failureResponse("500", "ISE")
    WiremockHelper.stubGet(url(nino), Status.INTERNAL_SERVER_ERROR, errorResponse.toString)
  }

  def verifyGetOpenHipReportDeadlines(nino: String): Unit =
    WiremockHelper.verifyGet(url(nino))

  def stubGetHipFulfilledReportDeadlines(nino: String): Unit = {
    val hipReportDeadlinesResponse = successResponse(nino).toString
    WiremockHelper.stubGet(url(nino, openObligations = false), Status.OK, hipReportDeadlinesResponse)
  }

  def stubGetHipFulfilledReportDeadlinesError(nino: String): Unit = {
    val errorResponse = failureResponse("500", "ISE")
    WiremockHelper.stubGet(url(nino, openObligations = false), Status.INTERNAL_SERVER_ERROR, errorResponse.toString)
  }

  def verifyGetFulfilledHipReportDeadlines(nino: String): Unit =
    WiremockHelper.verifyGet(url(nino, openObligations = false))

  def stubGetHipAllObligations(nino: String, from: String, to: String): Unit = {
    val hipReportDeadlinesResponse = successResponse(nino).toString
    WiremockHelper.stubGet(allObligationsUrl(nino, from, to), Status.OK, hipReportDeadlinesResponse)
  }

  def stubGetHipAllObligations(nino: String, from: String, to: String, statusCode: String): Unit = {
    val hipReportDeadlinesResponse = successResponseWithStatus(nino, statusCode).toString
    WiremockHelper.stubGet(allObligationsUrl(nino, from, to), Status.OK, hipReportDeadlinesResponse)
  }

  def stubGetHipAllObligationsError(nino: String, from: String, to: String)(status: Int, body: String): Unit = {
    WiremockHelper.stubGet(allObligationsUrl(nino, from, to), status, body)
  }

  def stubGetFulfilledObligations(nino: String, fromDate: String, toDate: String): Unit = {
    val hipReportDeadlinesResponse = successResponse(nino).toString
    WiremockHelper.stubGet(fulfilledObligationsUrl(nino, fromDate, toDate), Status.OK, hipReportDeadlinesResponse)
  }

  def verifyGetFulfilledObligations(nino: String, fromDate: String, toDate: String): Unit = {
    WiremockHelper.verifyGet(fulfilledObligationsUrl(nino, fromDate, toDate))
  }

  def stubGetFulfilledObligationsError(nino: String, fromDate: String, toDate: String)(status: Int, body: String): Unit =
    WiremockHelper.stubGet(fulfilledObligationsUrl(nino, fromDate, toDate), status, body)

  def verifyGetHipAllObligations(nino: String, from: String, to: String): Unit = {
    WiremockHelper.verifyGet(allObligationsUrl(nino, from, to))
  }

}
