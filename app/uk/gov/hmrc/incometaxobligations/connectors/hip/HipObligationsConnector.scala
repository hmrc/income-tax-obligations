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

package uk.gov.hmrc.incometaxobligations.connectors.hip

import play.api.http.Status.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.incometaxobligations.config.AppConfig
import uk.gov.hmrc.incometaxobligations.connectors.RawResponseReads
import uk.gov.hmrc.incometaxobligations.models.hip.EtmpErrorResponse
import uk.gov.hmrc.incometaxobligations.models.obligations.{ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}
import uk.gov.hmrc.incometaxobligations.models.hip.ObligationsHipApi

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipObligationsConnector @Inject()(val http: HttpClientV2,
                                        val appConfig: AppConfig
                                    )(implicit ec: ExecutionContext) extends RawResponseReads:

  private[connectors] def getOpenObligationsUrl(nino: String): String =
    s"${appConfig.hipUrl}/RESTAdapter/obligation-data/nino/$nino/ITSA?status=O"

  private[connectors] def getAllObligationsDateRangeUrl(nino: String, from: String, to: String): String =
    s"${appConfig.hipUrl}/RESTAdapter/obligation-data/nino/$nino/ITSA?from=$from&to=$to"

  private[connectors] def getFulfilledObligationsUrl(nino: String, from: String, to: String): String =
    s"${appConfig.hipUrl}/RESTAdapter/obligation-data/nino/$nino/ITSA?status=F&from=$from&to=$to"

  val hipHeaders: Seq[(String, String)] = appConfig.getHIPHeaders(ObligationsHipApi)

  private def callHipObligationsAPI(url: String)(implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] =
    http.get(url"$url")
      .setHeader(hipHeaders: _*)
      .execute[HttpResponse]
      .map { response =>
        response.status match
          case OK =>
            logger.info(s"RESPONSE status: ${response.status}")
            response.json.validate[ObligationsModel](ObligationsModel.hipReadsApi1330).fold(
              invalid =>
                logger.error(s"Json validation error: $invalid")
                ObligationsErrorModel(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data"),
              valid =>
                logger.info("successfully parsed response to ObligationsModel")
                valid
            )
          case UNPROCESSABLE_ENTITY =>
            response.json.validate[EtmpErrorResponse].fold(
              invalid =>
                logger.error(s"Json validation error: $invalid for RESPONSE status: ${response.status}, body: ${response.body}")
                ObligationsErrorModel(INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Error Response"),
              valid =>
                if(valid.errors.isNotFoundError) {
                  logger.info(s"Data Not Found Code, converting response to 404 for RESPONSE status: ${response.status}, body: ${response.body}")
                  ObligationsErrorModel(NOT_FOUND, response.body)
                } else {
                  logger.error(s"RESPONSE status: ${response.status}, body: ${response.body}")
                  ObligationsErrorModel(response.status, response.body)
                }
            )
          case _ =>
            logger.error(s"RESPONSE status: ${response.status}, body: ${response.body}")
            ObligationsErrorModel(response.status, response.body)
      } recover {
      case ex =>
        logger.error(s"Unexpected failed future, ${ex.getMessage}")
        ObligationsErrorModel(INTERNAL_SERVER_ERROR, s"Unexpected failed future, ${ex.getMessage}")
    }

  def getOpenObligations(nino: String)
                      (implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] =
    val url = getOpenObligationsUrl(nino)

    logger.debug(s"Calling GET $url")
    callHipObligationsAPI(url)

  def getAllObligationsWithinDateRange(nino: String, from: String, to: String)
                                      (implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] =
    val url = getAllObligationsDateRangeUrl(nino, from, to)

    logger.debug(s"Calling GET $url")
    callHipObligationsAPI(url)

  def getFulfilledObligations(nino: String, from: String, to: String)
                             (implicit headerCarrier: HeaderCarrier): Future[ObligationsResponseModel] =
    val url = getFulfilledObligationsUrl(nino, from, to)
    logger.debug(s"Calling GET $url")
    callHipObligationsAPI(url)