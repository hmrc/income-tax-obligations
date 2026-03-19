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
import uk.gov.hmrc.incometaxobligations.connectors.{RawResponseReads, ViewAndChangeConnector}
import uk.gov.hmrc.incometaxobligations.connectors.hip.ITSAStatusConnector
import uk.gov.hmrc.incometaxobligations.connectors.itsastatus.OptOutUpdateRequestModel.{OptOutUpdateRequest, OptOutUpdateResponse, OptOutUpdateResponseSuccess}
import uk.gov.hmrc.incometaxobligations.models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseModel}
import uk.gov.hmrc.incometaxobligations.repositories.ITSAStatusRepository
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ITSAStatusService @Inject()(itsaRepository: ITSAStatusRepository,
                                       itsaConnector: ITSAStatusConnector,
                                       viewAndChangeConnector: ViewAndChangeConnector) extends RawResponseReads with Logging{

  def getITSAStatus(taxableEntityId: String, taxYear: String, futureYears: Boolean, history: Boolean)
                   (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = {

    val dataKey = DataKey[List[ITSAStatusResponseModel]]("ITSA_Status")
    itsaRepository.getCache[List[ITSAStatusResponseModel]](taxableEntityId)(dataKey).flatMap{
      case Some(value) => Future.successful(Right(value))
      case _ =>
        itsaConnector.getITSAStatus(taxableEntityId, taxYear, futureYears, history).flatMap{
          case Right(success)  => itsaRepository.updateCache(taxableEntityId)(dataKey, success)
            Future.successful(Right(success))
          case _ => viewAndChangeConnector.getITSAStatus(taxableEntityId, taxYear, futureYears, history)
      }
    }
  }

  def requestOptOutForTaxYear(taxableEntityId: String, optOutUpdateRequest: OptOutUpdateRequest)
                             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    val dataKey = DataKey[List[ITSAStatusResponseModel]]("ITSA_Status")
    itsaConnector.requestOptOutForTaxYear(taxableEntityId, optOutUpdateRequest).flatMap{
      case success: OptOutUpdateResponseSuccess => itsaRepository.deleteCache(taxableEntityId)(dataKey)
        Future.successful(success)
      case _ => itsaRepository.deleteCache(taxableEntityId)(dataKey)
        viewAndChangeConnector.requestOptOutForTaxYear(taxableEntityId, optOutUpdateRequest)
    }
  }
}
