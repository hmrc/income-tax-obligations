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

package uk.gov.hmrc.incometaxobligations.repositories

import org.mongodb.scala.bson.BsonDocument
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import play.api.test.Helpers.await
import uk.gov.hmrc.incometaxobligations.models.itsaStatus.{ITSAStatusResponseModel, StatusDetail}
import uk.gov.hmrc.incometaxobligations.utils.TestSupport
import uk.gov.hmrc.mongo.cache.DataKey
import org.mongodb.scala.gridfs.SingleObservableFuture
import play.api.test.DefaultAwaitTimeout

class ITSAStatusRepositorySpec extends TestSupport with Matchers with BeforeAndAfterEach with DefaultAwaitTimeout {
  val repository = app.injector.instanceOf[ITSAStatusRepository]
  val dataKey = DataKey[List[ITSAStatusResponseModel]]("ITSA_Status")

  val id = "NX1000000AB"
  val taxYear = "2024"
  val statusDetail = new StatusDetail("12/12/2024", "Completed", "Submitted and finished")
  val responseModel = new ITSAStatusResponseModel(taxYear, Some(List(statusDetail)))

  override def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
    super.beforeEach()
  }

  "ITSAStatusRepository" should {
    "add a model to the cache" in {
      val result = repository.updateCache(id)(dataKey, List(responseModel))
      await(result) shouldBe responseModel
    }
    "find an existing model" in {
      repository.updateCache(id)(dataKey, List(responseModel))
      val result = repository.getCache(id)(dataKey)
      await(result) shouldBe responseModel
    }
    "delete a model" in {
      
    }
  }
}
