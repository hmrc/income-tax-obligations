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

import play.api.http.Status.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.incometaxobligations.config.AppConfig
import uk.gov.hmrc.incometaxobligations.models.obligations.{ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ViewAndChangeConnector @Inject()(val http: HttpClientV2,
                                       val appConfig: AppConfig
                                    )(implicit ec: ExecutionContext) extends RawResponseReads {

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
}
