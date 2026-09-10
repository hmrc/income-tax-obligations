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

package uk.gov.hmrc.incometaxobligations.models.hip

import play.api.libs.json.{Json, OFormat}

//Current EPID implies this is a single object not an array, subject to change once HIP spec is released as this goes against existing error patterns
case class EtmpErrorResponse(errors: EtmpErrorModel)

object EtmpErrorResponse {
  implicit val format: OFormat[EtmpErrorResponse] = Json.format[EtmpErrorResponse]
}

case class EtmpErrorModel(processingDate: String, code: String, text: String) {
  lazy val isNotFoundError: Boolean = code == "025"
}

object EtmpErrorModel {
  implicit val format: OFormat[EtmpErrorModel] = Json.format[EtmpErrorModel]
}
