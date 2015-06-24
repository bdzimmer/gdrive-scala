// Copyright (c) 2015 Ben Zimmer. All rights reserved.

// Small Scala library for common functionality with Google Drive.
// Uses the Google Drive Java API.

// 2015-04 to 2015-05: Created.
// 2015-05-26: Better comments.


package bdzimmer.gdrivescala


import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

import scala.collection.JavaConverters._

import org.apache.commons.io.ByteOrderMark
import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.BOMInputStream

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.ParentReference


case class GoogleDriveKeys(CLIENT_ID: String, CLIENT_SECRET: String, ACCESS_TOKEN: String, REFRESH_TOKEN: String, REDIRECT_URI: String)



/**
 * Object with methods for working with Google Drive.
 */
object DriveUtils {



  val FOLDER_TYPE = "application/vnd.google-apps.folder"
  // val FILE_TYPE = "application/vnd.google-apps.file"
  val FILE_TYPE = "application/octet-stream"



  /**
   * Get the root file.
   *
   * @param drive     Drive service
   * @return the root file
   */
  def getRoot(drive: Drive): File = {

    // val about = drive.about.get.execute
    // val rootId = about.getRootFolderId

    val rootId = "root"
    drive.files.get(rootId).execute


  }



  /**
   * Get the files that have a common parent.
   *
   * @param drive     Drive service
   * @param parent    parent file
   * @return list of non-trashed files that have the given parent file as a parent
   */
  def getFilesByParent(drive: Drive, parent: File): List[File] = {

    // TODO: parameter with default value to control whether trashed files are returned

    val request = drive.files.list.setQ("'%s' in parents and trashed = false".format(parent.getId)).setMaxResults(1000)
    request.execute.getItems.asScala.toList

  }



  /**
   * Get a file by parent and title.
   *
   * @param drive     Drive service
   * @param parent    parent file
   * @param title     title of file to get
   * @return the matching file
   */
  def getFileByParentAndTitle(drive: Drive, parent: File, title: String): File = {

    val request = drive.files.list.setQ("'%s' in parents and title = '%s' and trashed = false".format(parent.getId, title))
    request.execute.getItems.asScala.toList.head

  }



  /**
   * Get a file by parent and path.
   *
   * @param drive     Drive service
   * @param parent    parent file
   * @param path      list of path elements below parent to desired file
   * @return the matching file
   */
  def getFileByPath(drive: Drive, parent: File, path: List[String]): File = path match {
    case x :: Nil => getFileByParentAndTitle(drive, parent, x)
    case x :: xs => {
      val result = getFileByParentAndTitle(drive, parent, x)
      getFileByPath(drive, result, xs)
    }
  }



  /**
   * Get a list of all non-folder descendants of a parent file, with path information.
   * Intended to be used to reconstruct Drive file hierarchies locally.
   *
   * @param drive         Drive service
   * @param parent        parent file
   * @param parentPath    list of path elements
   * @return list of tuples of file and list of path elements below parent
   */
  def getDescendantsPaths(drive: Drive, parent: File, parentPath: List[String]): List[(File, List[String])] = {

    val children = DriveUtils.getFilesByParent(drive, parent)
    val leaves = children.filter(!_.getMimeType.equals(DriveUtils.FOLDER_TYPE))

    val folders = children.filter(_.getMimeType.equals(DriveUtils.FOLDER_TYPE))

    // not sure why the path here ends up being of type List[Any]
    // val foldersResult = folders.map(x => (x, (parentPath +: x.getTitle).toList))

    val leavesResult = leaves.map(x => (x, parentPath :+ x.getTitle))

    val foldersChildren = folders.flatMap(x => DriveUtils.getDescendantsPaths(drive, x, parentPath :+ x.getTitle))

    leavesResult ++ foldersChildren

  }



  /**
   * Get the text contents of a file.
   * Experimental.
   *
   * @param drive     Drive service
   * @param file      file to get the contents of
   * @return string of text contents of file
   */
  def getFileText(drive: Drive, file: File): String = {

    // using this method, the resulting string will start with a BOM
    // http://en.wikipedia.org/wiki/Byte_order_mark
    // val fileStream = drive.files.get(file.getId).executeMediaAsInputStream
    // IOUtils.toString(fileStream)

    // 2015-05-17
    // this is better, but not perfect
    // for some reason, I see the UTF-16LE BOM when running locally
    // but the UTF-8 BOM when running on with Heroku Toolbelt
    val fileStream = drive.files.get(file.getId).executeMediaAsInputStream
    val betterStream = new BOMInputStream(fileStream, false, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE)
    IOUtils.toString(betterStream)

    // 2015-05-18
    // val out = new ByteArrayOutputStream
    // drive.files.get(file.getId).executeMediaAndDownloadTo(out)
    // new String(out.toByteArray, java.nio.charset.StandardCharsets.UTF_8)

  }



  /**
   * Download a file from Drive.
   *
   * @param drive     Drive service
   * @param file      file to download
   * @param file      local file name to download to
   */
  def downloadFile(drive: Drive, file: File, filename: String): Unit = {
    val out = new FileOutputStream(filename)
    drive.files.get(file.getId).executeMediaAndDownloadTo(out)
  }



  /**
   * Upload a file to Drive (WIP).
   *
   * @param drive     Drive service
   * @param filename  local file name to upload
   * @param parent    parent folder on Drive to upload to
   * @return          uploaded file
   */
   def uploadFile(drive: Drive, filename: String, parent: File): File = {

     val localFile = new java.io.File(filename)

     val metadata = new File
     metadata.setTitle(localFile.getName)
     metadata.setMimeType(FILE_TYPE)
     metadata.setParents(List(new ParentReference().setId(parent.getId)).asJava)

     val mediaContent = new FileContent(FILE_TYPE, localFile)

     drive.files.insert(metadata, mediaContent).execute

   }



   /**
    * Create a folder in Drive (WIP).
    *
    * @param drive          Drive service
    * @param foldername     name of folder to create
    * @param parent         parent folder on Drive to create in
    * @return               created folder
    */
    def createFolder(drive: Drive, foldername: String, parent: File): File = {

      val metadata = new File
      metadata.setTitle(foldername)
      metadata.setMimeType(FOLDER_TYPE)
      metadata.setParents(List(new ParentReference().setId(parent.getId)).asJava)

      drive.files.insert(metadata).execute

    }



  /**
   * Create a Google Drive service object from keys object
   * and application name.
   *
   * @param keys               keys object
   * @param applicationName    application name
   * @return Drive service object
   */
  def getDrive(keys: GoogleDriveKeys, applicationName: String): Drive = {

    val httpTransport = new NetHttpTransport()
    val jsonFactory = new JacksonFactory()

    val credential = new GoogleCredential.Builder()
     .setTransport(httpTransport)
     .setJsonFactory(jsonFactory)
     .setClientSecrets(keys.CLIENT_ID, keys.CLIENT_SECRET)
     .build

    credential.setAccessToken(keys.ACCESS_TOKEN)
    credential.setRefreshToken(keys.REFRESH_TOKEN)

    new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName).build

  }



  /**
   * Load keys from a properties file somewhere in the file system.
   * This allows the properties to be stored outside of the codebase.
   *
   * @param filename      file name of properties file
   * @return keys object
   */
  def getKeysFromProperties(filename: String): GoogleDriveKeys = {

    val prop = new Properties()
    prop.load(new FileInputStream(filename))

    val CLIENT_ID = prop.getProperty("CLIENT_ID")
    val CLIENT_SECRET = prop.getProperty("CLIENT_SECRET")

    val ACCESS_TOKEN = prop.getProperty("ACCESS_TOKEN")
    val REFRESH_TOKEN = prop.getProperty("REFRESH_TOKEN")

    val REDIRECT_URI = prop.getProperty("REDIRECT_URI")

    new GoogleDriveKeys(CLIENT_ID, CLIENT_SECRET, ACCESS_TOKEN, REFRESH_TOKEN, REDIRECT_URI)

  }



  /**
   * Load Google Drive keys from environment variables
   * (preferred method for Heroku).
   *
   * @return keys object
   */
  def getKeysFromEnvironment(): GoogleDriveKeys = {

    val CLIENT_ID = scala.util.Properties.envOrElse("GOOGLE_CLIENT_ID", "")
    val CLIENT_SECRET = scala.util.Properties.envOrElse("GOOGLE_CLIENT_SECRET", "")

    val ACCESS_TOKEN = scala.util.Properties.envOrElse("GOOGLE_ACCESS_TOKEN", "")
    val REFRESH_TOKEN = scala.util.Properties.envOrElse("GOOGLE_REFRESH_TOKEN", "")

    val REDIRECT_URI = scala.util.Properties.envOrElse("GOOGLE_REDIRECT_URI", "")

    new GoogleDriveKeys(CLIENT_ID, CLIENT_SECRET, ACCESS_TOKEN, REFRESH_TOKEN, REDIRECT_URI)

  }


}
