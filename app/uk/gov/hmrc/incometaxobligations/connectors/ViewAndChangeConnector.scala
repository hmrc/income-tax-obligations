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

package uk.gov.hmrc.incometaxobligations.connectors

import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.incometaxobligations.config.AppConfig
import uk.gov.hmrc.incometaxobligations.connectors.hip.ITSAStatusConnector.CorrelationIdHeader
import uk.gov.hmrc.incometaxobligations.connectors.itsastatus.OptOutUpdateRequestModel.*
import uk.gov.hmrc.incometaxobligations.models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusResponseModel}
import uk.gov.hmrc.incometaxobligations.models.obligations.{ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ViewAndChangeConnector @Inject()(val http: HttpClientV2,
                                       val appConfig: AppConfig
                                    )(implicit ec: ExecutionContext) extends RawResponseReads with Logging {

  def getOpenObligations(nino: String)(implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] = {
    val url = s"${appConfig.viewAndChangeBaseUrl}/income-tax-view-change/$nino/open-obligations"
    http.get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            logger.info(s"RESPONSE status: ${response.status}, body: ${response.body}")
            response.json.validate[ObligationsModel](ObligationsModel.format).fold(
              invalid => {
                logger.error(s"Json validation error: $invalid")
                ObligationsErrorModel(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data")
              },
              valid => {
                logger.info("successfully parsed response to ObligationsModel")
                valid
              }
            )
          case _ =>
            logger.error(s"RESPONSE status: ${response.status}, body: ${response.body}")
            response.json.as[ObligationsErrorModel]
        }
      } recover {
      case ex =>
        logger.error(s"Unexpected failed future, ${ex.getMessage}")
        ObligationsErrorModel(INTERNAL_SERVER_ERROR, s"Unexpected failed future")
    }
  }

  def getAllObligationsWithinDateRange(nino: String, from: String, to: String)
                                      (implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] = {
    val url = s"${appConfig.viewAndChangeBaseUrl}/income-tax-view-change/$nino/obligations/from/$from/to/$to"

    logger.info(s"Calling GET $url")
    http.get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            logger.info(s"RESPONSE status: ${response.status}, body: ${response.body}")
            response.json.validate[ObligationsModel](ObligationsModel.format).fold(
              invalid => {
                logger.error(s"Json validation error: $invalid")
                ObligationsErrorModel(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data")
              },
              valid => {
                logger.info("successfully parsed response to ObligationsModel")
                valid
              }
            )
          case _ =>
            logger.error(s"RESPONSE status: ${response.status}, body: ${response.body}")
            response.json.as[ObligationsErrorModel]
        }
      } recover {
      case ex =>
        logger.error(s"Unexpected failed future, ${ex.getMessage}")
        ObligationsErrorModel(INTERNAL_SERVER_ERROR, s"Unexpected failed future")
    }
  }

  def getITSAStatusUrl(taxableEntityId: String, taxYear: String, futureYears: String, history: String): String =
    s"${appConfig.viewAndChangeBaseUrl}/income-tax-view-change/itsa-status/status/$taxableEntityId/$taxYear?futureYears=$futureYears&history=$history"

  def updateItsaStatusUrl(taxableEntityId: String): String =
    s"${appConfig.viewAndChangeBaseUrl}/income-tax-view-change/itsa-status/update/$taxableEntityId"

  def getITSAStatus(taxableEntityId: String, taxYear: String, futureYears: Boolean, history: Boolean)
                   (implicit headerCarrier: HeaderCarrier): Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = {
    
    val url = getITSAStatusUrl(taxableEntityId, taxYear, futureYears.toString, history.toString)
    http.get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            logger.debug(s"RESPONSE status:${response.status}, body:${response.body}")
            response.json.validate[List[ITSAStatusResponseModel]].fold(
              invalid => {
                logger.error(s"Validation Errors: $invalid")
                Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing ITSA Status Response"))
              },
              valid => {
                logger.info("successfully parsed response to getITSAStatus")
                Right(valid)
              }
            )
          case _ =>
            logger.error(s"RESPONSE status: ${response.status}, body: ${response.body}, hc: ${headerCarrier}")
            Left(response.json.as[ITSAStatusResponseError])
        }
      } recover {
      case ex =>
        logger.error(s"Unexpected failed future, ${ex.getMessage}")
        Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Unexpected failed future"))
    }
  }

  def requestOptOutForTaxYear(taxableEntityId: String, optOutUpdateRequest: OptOutUpdateRequest)
                             (implicit headerCarrier: HeaderCarrier): Future[OptOutUpdateResponse] = {

    http.put(url"${updateItsaStatusUrl(taxableEntityId)}")
      .withBody(Json.toJson[OptOutUpdateRequest](optOutUpdateRequest))
      .execute[HttpResponse]
      .map { response =>
        val correlationId = response.headers.get(CorrelationIdHeader).map(_.head).getOrElse(s"Unknown_$CorrelationIdHeader")
        response.status match {
          case NO_CONTENT =>
            logger.info("ITSA status successfully updated")
            OptOutUpdateResponseSuccess(correlationId)

          case status =>
            val errors = response.json.as[OptOutErrorModel]
            OptOutUpdateResponseFailure(correlationId, status, errors.failures)
        }
      }
  }
  
  def getFulfilledObligationsUrl(nino: String) = {
    s"${appConfig.viewAndChangeBaseUrl}/income-tax-view-change/$nino/fulfilled-obligations"
  }

  def headers: Seq[(String, String)] = appConfig.desAuthHeaders
  
  private def callObligationsAPI(url: String)(implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] = {
    http.get(url"$url")
      .setHeader(headers: _*)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            logger.info(s"RESPONSE status: ${response.status}, body: ${response.body}")
            response.json.validate[ObligationsModel](ObligationsModel.desReadsApi1330).fold(
              invalid => {
                logger.error(s"Json validation error: $invalid")
                ObligationsErrorModel(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data")
              },
              valid => {
                logger.info("successfully parsed response to ObligationsModel")
                valid
              }
            )
          case _ =>
            logger.error(s"RESPONSE status: ${response.status}, body: ${response.body}")
            ObligationsErrorModel(response.status, response.body)
        }
      } recover {
      case ex =>
        logger.error(s"Unexpected failed future, ${ex.getMessage}")
        ObligationsErrorModel(INTERNAL_SERVER_ERROR, s"Unexpected failed future, ${ex.getMessage}")
    }
  }
  
  def getFulfilledObligations(nino: String)(implicit headerCarrier: HeaderCarrier) = {
    val url = getFulfilledObligationsUrl(nino)

    logger.info(s"URL - $url ")
    logger.debug(s"Calling GET $url \n\nHeaders: $headerCarrier \nAuth Headers: ${appConfig.desAuthHeaders}")
    callObligationsAPI(url)
  }
}
