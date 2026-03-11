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
import org.mongodb.scala.gridfs.ObservableFuture
import play.api.libs.json
import play.api.libs.json.{JsObject, Json}
import play.api.test.DefaultAwaitTimeout

import java.time.{LocalDateTime, ZoneOffset}

class ITSAStatusRepositorySpec extends TestSupport with Matchers with BeforeAndAfterEach with DefaultAwaitTimeout {
  val repository = app.injector.instanceOf[ITSAStatusRepository]
  val dataKey = DataKey[List[ITSAStatusResponseModel]]("ITSA_Status")

  val id = "NX1000000AB"
  val taxYear = "2024"
  val updatedTaxYear = "2025"
  val statusDetail = new StatusDetail("12/12/2024", "Completed", "Submitted and finished")
  val responseModel = new ITSAStatusResponseModel(taxYear, Some(List(statusDetail)))
  val updatedResponseModel = new ITSAStatusResponseModel(updatedTaxYear, Some(List(statusDetail)))
  override def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
    super.beforeEach()
  }

  "ITSAStatusRepository" should {
    "add a model to the cache if nothing already exists" in {
      val timeBeforeTest = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC)
      await(repository.updateCache(id)(dataKey, List(responseModel)))
      val timeAfterTest = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC)
      val updatedRecord = await(repository.collection.find[BsonDocument](BsonDocument()).toFuture()).head

      val resultParsedToJson = Json.parse(updatedRecord.toJson).as[JsObject]

      val resultId = (resultParsedToJson \ "_id").as[String]
      val lastUpdated = (resultParsedToJson \ "modifiedDetails" \ "lastUpdated" \ "$date").as[LocalDateTime].toEpochSecond(ZoneOffset.UTC)
      resultId shouldBe id

      assert(lastUpdated > timeBeforeTest || lastUpdated == timeBeforeTest)
      assert(lastUpdated < timeAfterTest || lastUpdated == timeAfterTest)

    }
    "update a model successfully" in {

      await(repository.collection.countDocuments().head()) shouldBe 0
      await(repository.updateCache(id)(dataKey, List(responseModel)))
      
      await(repository.collection.countDocuments().head()) shouldBe 1
      await(repository.updateCache(id)(dataKey, List(updatedResponseModel)))
      
      val result = repository.getCache[List[ITSAStatusResponseModel]](id)(dataKey)
      
      await(result) shouldBe Some(List(updatedResponseModel))
    }
    "find an existing model" in {
      repository.updateCache(id)(dataKey, List(responseModel))
      val result = repository.getCache[List[ITSAStatusResponseModel]](id)(dataKey)
      await(result) shouldBe Some(List(responseModel))
    }
    "delete a model" in {
      await(repository.collection.countDocuments().head()) shouldBe 0
      await(repository.updateCache(id)(dataKey, List(responseModel)))
      await(repository.collection.countDocuments().head()) shouldBe 1
      await(repository.deleteCache(id)(dataKey))
      
      val result = repository.get(id)(dataKey)
      await(result) shouldBe None
    }
  }
}
