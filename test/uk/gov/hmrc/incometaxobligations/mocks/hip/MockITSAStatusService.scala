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

package uk.gov.hmrc.incometaxobligations.mocks.hip

import uk.gov.hmrc.incometaxobligations.models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import uk.gov.hmrc.incometaxobligations.services.ITSAStatusService

import scala.concurrent.Future

trait MockITSAStatusService extends AnyWordSpecLike with Matchers with OptionValues with BeforeAndAfterEach {

  val mockHIPITSAStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHIPITSAStatusService)
  }

  def mockHIPGetITSAStatus(response: Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]): Unit = {
    when(mockHIPITSAStatusService.getITSAStatus(any, any, any, any)(ArgumentMatchers.any(), any())) thenReturn Future.successful(response)
  }

}