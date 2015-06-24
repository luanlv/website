package controllers

import javax.inject.{Inject, Singleton}

import com.sksamuel.scrimage.{Format, Image}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{Json, _}
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import reactivemongo.api.gridfs.DefaultFileToSave
import reactivemongo.api.{Cursor, QueryOpts}
import reactivemongo.bson._
import services.UUIDGenerator
import reactivemongo.api.gridfs.Implicits._
import play.modules.reactivemongo.json.BSONFormats._


//----send email----------------------------------

//----send email----------------------------------


import models.Formats._

import scala.concurrent.Future

@Singleton
class ImageCtrl @Inject()(uuidGenerator: UUIDGenerator)
    extends Controller
    with MongoController
{
  private final val logger = Logger

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //

  import models.Models._
  import models._

  def upload = Action.async(gridFSBodyParser(gridFS)) { request =>
    // here is the future file!
    val futureFile = request.body.files.head.ref

    val groupImg = java.util.UUID.randomUUID().toString()
    // when the upload is complete, we add the article id to the file entry (in order to find the attachments of the article)
      val futureUpdate = for {
        file <- futureFile
        // here, the file is completely uploaded, so it is time to update the user
        updateResult <- {
          collectionFiles.update(
            BSONDocument("_id" -> file.id),
            BSONDocument("$set" -> BSONDocument("metadata" -> BSONDocument("UUID" -> groupImg, "size" -> "normal")))
          )
          val iterator = gridFS.enumerate(file).run(Iteratee.consume[Array[Byte]]())
          iterator.flatMap {
            bytes => {
              // Create resized image
              val enumerator: Enumerator[Array[Byte]] = Enumerator.outputStream(
                out => {
                  Image(bytes).bound(80, 80).writer(Format.JPEG).withCompression(90).write(out)
                }
              )
              val data = DefaultFileToSave(
                filename = file.filename,
                contentType = file.contentType,
                //uploadDate = Some(DateTime.now().getMillis),
                metadata = file.metadata ++ BSONDocument(
                  "UUID" -> groupImg,
                  "size" -> "thumb"
                )
              )
              gridFS.save(enumerator, data).map {
                image => Some(image)
              }
            }
          }

        }
      } yield (updateResult, file.filename, file.length)
      futureUpdate.map {
        case (_, fn, fl) => {

          val jsonString = "{\"files\": [ { \"name\": \"" + fn + "\", \"size\":" + fl +
              ", \"url\": \"" +
              "/image/get/"+ groupImg + "/normal" +
              "\", \"thumbnailUrl\": \"" +
              "/image/get/"+ groupImg + "/thumb" +
              "\", \"deleteUrl\": \"http:\\/\\/example.org\\/files\\/picture1.jpg\", \"deleteType\": \"DELETE\" } ]}"
          val json: JsValue = Json.parse(jsonString)
          Ok(json)
        }
      }.recover {
        case e => InternalServerError(e.getMessage())
      }
  }


  def saveAttachment(id: String) = Action.async(gridFSBodyParser(gridFS)) { request =>
    // here is the future file!
    val futureFile = request.body.files.head.ref

    val groupImg = java.util.UUID.randomUUID().toString()
    // when the upload is complete, we add the article id to the file entry (in order to find the attachments of the article)
    if (!request.body.files.head.contentType.toString.contains("image")) {
      Future.successful(BadRequest("Not is image"))
    } else {
      val futureUpdate = for {
        file <- futureFile
        // here, the file is completely uploaded, so it is time to update the user
        updateResult <- {
          val iterator = gridFS.enumerate(file).run(Iteratee.consume[Array[Byte]]())
          iterator.flatMap {
            bytes => {
              // Create resized image
              val enumerator: Enumerator[Array[Byte]] = Enumerator.outputStream(
                out => {
                  Image(bytes).bound(150, 185).writer(Format.JPEG).withCompression(90).write(out)
                }
              )
              val data = DefaultFileToSave(
                filename = file.filename,
                contentType = file.contentType,
                //uploadDate = Some(DateTime.now().getMillis),
                metadata = file.metadata ++ BSONDocument(
                  "user" -> id,
                  "group" -> groupImg,
                  "size" -> "thumb"
                )
              )

              gridFS.save(enumerator, data).map {
                image => Some(image)
              }
            }
          }

        }
      } yield updateResult
      futureUpdate.map {
        case _ => Ok(groupImg)
      }.recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  }

  def get(id: String, size: String) = Action.async { request =>
    // find the matching attachment, if any, and streams it to the client
    val file = gridFS.find(BSONDocument("metadata.UUID" -> id, "metadata.size" -> size))
//    request.getQueryString("inline") match {
//      case Some("true") => serve(gridFS, file, CONTENT_DISPOSITION_INLINE)
//      case _ => serve(gridFS, file)
//    }
    serve(gridFS, file, CONTENT_DISPOSITION_INLINE)
  }

}
