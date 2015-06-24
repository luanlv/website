package models

import reactivemongo.bson._

import play.api.libs.json._
import play.api.libs.json.Json._

import play.api.libs.json.Writes._


object Formats {

  val objectIDRegExFormat = "^[0-9a-fA-F]{24}$".r
  def isObjectIDValid(input: String): Boolean = (objectIDRegExFormat findFirstIn input).nonEmpty

  implicit object ObjectIdReads extends Format[BSONObjectID] {
    def reads(json: JsValue): JsResult[BSONObjectID] = json.asOpt[JsObject] map { oid =>
      (oid \ "$oid" ).asOpt[String] map { str =>
        if (isObjectIDValid(str))
          JsSuccess(BSONObjectID(str))
        else
          JsError("Invalid ObjectId %s".format(str))
      } getOrElse (JsError("Value is not an ObjectId"))
    } getOrElse (JsError("Value is not an ObjectId"))

    def writes(oid: BSONObjectID): JsValue = Json.obj("$oid" -> JsString(oid.stringify))
  }

}