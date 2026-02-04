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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ITSAStatusService @Inject()(itsaConnector: ITSAStatusConnector,
                                       viewAndChangeConnector: ViewAndChangeConnector) extends RawResponseReads with Logging{

  def getITSAStatus(taxableEntityId: String, taxYear: String, futureYears: Boolean, history: Boolean)
                   (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]] = {
    itsaConnector.getITSAStatus(taxableEntityId, taxYear, futureYears, history).flatMap{
      case Right(success)  => Future.successful(Right(success))
      case _ => viewAndChangeConnector.getITSAStatus(taxableEntityId, taxYear, futureYears, history)
    }
  }

  def requestOptOutForTaxYear(taxableEntityId: String, optOutUpdateRequest: OptOutUpdateRequest)
                             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    itsaConnector.requestOptOutForTaxYear(taxableEntityId, optOutUpdateRequest).flatMap{
      case success: OptOutUpdateResponseSuccess => Future.successful(success)
      case _ => viewAndChangeConnector.requestOptOutForTaxYear(taxableEntityId, optOutUpdateRequest)
    }
  }
}
