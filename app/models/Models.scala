package models

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._
import play.modules.reactivemongo.{ReactiveMongoPlugin, MongoController}
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.gridfs.GridFS
import reactivemongo.api.indexes.{IndexType, Index}



object Models extends Controller with MongoController  {
    private final val logger = Logger
    val gridFS = new GridFS(db)

    // let's build an index on our gridfs chunks collection if none
    gridFS.ensureIndex().onComplete {
        case index =>
            Logger.info(s"Checked index, result is $index")
    }

    /*
     * Get a JSONCollection (a Collection implementation that is designed to work
     * with JsObject, Reads and Writes.)
     * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
     * the collection reference to avoid potential problems in development with
     * Play hot-reloading.
     */



    def collectionFiles: JSONCollection = db.collection[JSONCollection]("fs.files")

}
