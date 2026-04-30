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

import play.api.libs.json.{JsPath, JsResultException, Reads, Writes}
import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, ReturnDocument, Updates}
import uk.gov.hmrc.incometaxobligations.config.AppConfig
import uk.gov.hmrc.incometaxobligations.models.itsaStatus.ITSAStatusResponseModel
import uk.gov.hmrc.mongo.play.json.Codecs

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, FiniteDuration, MINUTES}

class ITSAStatusRepository @Inject() (
    mongoComponent: MongoComponent,
    timestampSupport: TimestampSupport,
    appConfig: AppConfig
    )(implicit ec: ExecutionContext
    ) extends MongoCacheRepository(
      mongoComponent   = mongoComponent,
      collectionName   = "itsaStatusRepository",
      ttl              = Duration.apply(appConfig.ttlMinutes, MINUTES),
      timestampSupport = timestampSupport,
      cacheIdType      = CacheIdType.SimpleCacheId
    ) {

  def getCache[A: Reads](cacheId: String)(dataKey: DataKey[List[ITSAStatusResponseModel]]): Future[Option[A]] = {
    def dataPath: JsPath = dataKey.unwrap.split('.').foldLeft[JsPath](JsPath)(_ \ _)
    findById(cacheId).map(
      _.flatMap(cache =>
        dataPath.asSingleJson(cache.data)
          .validateOpt[A]
          .fold(e => throw JsResultException(e), identity)
      )
    )
  }

  def updateCache[A: Writes](cacheId: String)(dataKey: DataKey[A], data: A) = {
    val timestamp = timestampSupport.timestamp()
    this.collection
      .findOneAndUpdate(
        filter = Filters.equal("_id", cacheId),
        update = Updates.combine(
          Updates.set("data." + dataKey.unwrap, Codecs.toBson(data)),
          Updates.set("modifiedDetails.lastUpdated", timestamp),
          Updates.setOnInsert("_id", cacheId),
          Updates.setOnInsert("modifiedDetails.createdAt", timestamp)
        ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()
  }

  def deleteCache[A](cacheId: String)(dataKey: DataKey[A]): Future[Unit] = {
    this.collection
      .findOneAndUpdate(
        filter = Filters.equal("_id", cacheId),
        update = Updates.combine(
          Updates.unset("data." + dataKey.unwrap),
          Updates.set("modifiedDetails.lastUpdated", timestampSupport.timestamp())
        )
      )
      .toFuture()
      .map(_ => ())
  }

}
