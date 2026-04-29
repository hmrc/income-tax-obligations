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

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.incometaxobligations.connectors.hip.ITSAStatusConnector
import uk.gov.hmrc.incometaxobligations.connectors.itsastatus.OptOutUpdateRequestModel.{OptOutUpdateRequest, OptOutUpdateResponse, OptOutUpdateResponseSuccess}
import uk.gov.hmrc.incometaxobligations.connectors.{RawResponseReads, ViewAndChangeConnector}
import uk.gov.hmrc.incometaxobligations.models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseModel, ITSAStatusResponseNotFound}
import uk.gov.hmrc.incometaxobligations.repositories.ITSAStatusRepository
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ITSAStatusService @Inject()(itsaRepository: ITSAStatusRepository,
                                       itsaConnector: ITSAStatusConnector,
                                       viewAndChangeConnector: ViewAndChangeConnector) extends RawResponseReads with Logging {

  def itsaStatusDataKey(taxyear: String,
                        futureYears: Boolean, history: Boolean): DataKey[List[ITSAStatusResponseModel]] = {
    val keyName = (futureYears, history) match {
      case (true, true) => s"ITSA_Status_${taxyear}_FutureAndHistory"
      case (true, false) => s"ITSA_Status_${taxyear}_Future"
      case (false, true) => s"ITSA_Status_${taxyear}_History"
      case (false, false) => s"ITSA_Status_$taxyear"
    }
    DataKey[List[ITSAStatusResponseModel]](keyName)
  }

  lazy val allITSAStatusKeyNames: String => List[String] = taxyear => List(
    s"ITSA_Status_${taxyear}_FutureAndHistory",
    s"ITSA_Status_${taxyear}_Future",
    s"ITSA_Status_${taxyear}_History",
    s"ITSA_Status_$taxyear"
  )

  def getITSAStatus(taxableEntityId: String, taxYear: String, futureYears: Boolean, history: Boolean)
                   (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = {

    val dataKey = itsaStatusDataKey(taxYear, futureYears, history)
    itsaRepository.getCache[List[ITSAStatusResponseModel]](taxableEntityId)(dataKey).flatMap{
      case Some(itsaStatus) if itsaStatus.isEmpty => Future.successful(Left(ITSAStatusResponseNotFound(404, "No ITSA Status found")))
      case Some(itsaStatus) => Future.successful(Right(itsaStatus))
      case optItsaStatus =>
        itsaConnector.getITSAStatus(taxableEntityId, taxYear, futureYears, history).flatMap{
          case Right(success) =>
            itsaRepository.updateCache(taxableEntityId)(dataKey, success)
              .map(_ => Right(success))
          case Left(error: ITSAStatusResponseNotFound) =>
            itsaRepository.updateCache(taxableEntityId)(dataKey, List())
              .map(_ => Left(error))
          case _ => viewAndChangeConnector.getITSAStatus(taxableEntityId, taxYear, futureYears, history)
      }
    }
  }

  def requestOptOutForTaxYear(taxableEntityId: String, optOutUpdateRequest: OptOutUpdateRequest)
                             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    val taxYear = optOutUpdateRequest.taxYear
    deleteCache(taxableEntityId, taxYear).flatMap { _ =>
      itsaConnector.requestOptOutForTaxYear(taxableEntityId, optOutUpdateRequest).flatMap {
        case success: OptOutUpdateResponseSuccess => Future.successful(success)
        case _ => viewAndChangeConnector.requestOptOutForTaxYear(taxableEntityId, optOutUpdateRequest)
      }
    }
  }

  def deleteCache(taxableEntityId: String, taxYear: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[List[Unit]] = {
    Future.sequence(allITSAStatusKeyNames(taxYear).map { keyName =>
      itsaRepository.deleteCache(taxableEntityId)(DataKey[List[ITSAStatusResponseModel]](keyName))
  })
  }
}
