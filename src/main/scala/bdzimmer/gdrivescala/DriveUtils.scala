// Copyright (c) 2015 Ben Zimmer. All rights reserved.

// 2015-04 to 2015-05: Created.
// 2015-05-26: Better comments.
// 2015-06-26: Cleaned up formatting. Recently added file upload, folder creation,
//             and file deletion.
// 2015-07-10: Recursive upload, download, and delete. Separated functions for
//             getting Drive service into DriveBuilder object.
// 2015-07-19: Functions that get individual files return Option[File]. Delete
//             trashes rather than permanently deletes for safety.
//             Upload that creates subfolders if they don't already exist.

package bdzimmer.gdrivescala

import java.io.FileOutputStream

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.seqAsJavaListConverter

import org.apache.commons.io.ByteOrderMark
import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.BOMInputStream

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.ParentReference



/**
 * Object with methods for operating on files in Drive.
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
   * @return          list of non-trashed files that have the given parent file as a parent
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
   * @return          the matching file or nothing
   */
  def getFileByParentAndTitle(drive: Drive, parent: File, title: String): Option[File] = {

    val request = drive.files.list.setQ("'%s' in parents and title = '%s' and trashed = false".format(parent.getId, title))
    request.execute.getItems.asScala.toList.headOption

  }



  /**
   * Get a file by parent and path.
   *
   * @param drive     Drive service
   * @param parent    parent file
   * @param path      list of path elements below parent to desired file
   * @return          the matching file or nothing
   */
  def getFileByPath(drive: Drive, parent: File, path: List[String]): Option[File] = path match {
    case x :: Nil => getFileByParentAndTitle(drive, parent, x)
    case x :: xs => {
      val result = getFileByParentAndTitle(drive, parent, x)
      result match {
        case Some(f) => getFileByPath(drive, f, xs)
        case None => None
      }

    }
  }



  /**
   * Get the text contents of a file.
   * Experimental.
   *
   * @param drive     Drive service
   * @param file      file to get the contents of
   * @return          string of text contents of file
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
   * Upload a file to Drive.
   *
   * @param drive     Drive service
   * @param filename  local file name to upload
   * @param parent    parent folder on Drive to upload to
   * @return          uploaded file
   */
  def uploadFile(drive: Drive, filename: String, parent: File): File = {

    val localFile = new java.io.File(filename)

    val metadata =   new File
    metadata.setTitle(localFile.getName)
    metadata.setMimeType(FILE_TYPE)
    metadata.setParents(List(new ParentReference().setId(parent.getId)).asJava)

    val mediaContent = new FileContent(FILE_TYPE, localFile)

    drive.files.insert(metadata, mediaContent).execute

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
   * Create a folder in Drive.
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
   * Delete a file from Drive (move to trash).
   *
   * @param drive       Drive service
   * @param file        file to delete
   */
  def deleteFile(drive: Drive, file: File): Unit = {
    drive.files.trash(file.getId).execute
  }



  /**
   * Upload a file to Drive recursively (works with folders).
   *
   * @param drive     drive service
   * @param file      file to upload to
   * @param parent    parent folder on Drive to upload to
   */
  def uploadFileRecursive(drive: Drive, file: java.io.File, parent: File): Unit = file.isDirectory match {

    case false => {
       // println("uploading file: " + file.getParentFile.getName + " " + file.getName)
       DriveUtils.uploadFile(drive, file.getAbsolutePath, parent)
    }

    case true => {
       // println("uploading directory: " + file.getParentFile.getName + " " + file.getName)
       val newParent = DriveUtils.createFolder(drive, file.getName, parent)
       val childFiles = file.listFiles.toList
       childFiles.foreach(uploadFileRecursive(drive, _, newParent))
    }

  }



  /**
   * Download a file from Drive recursively (works with folders).
   *
   * @param drive     Drive service
   * @param file      file to download
   * @param parent    local file name to download to
   */
  def downloadFileRecursive(drive: Drive, file: File, parent: java.io.File): Unit = file.getMimeType.equals(DriveUtils.FOLDER_TYPE) match {

    case false => {

      // println("downloading file: " + file.getTitle)
      DriveUtils.downloadFile(drive, file, parent.getAbsolutePath + "/" + file.getTitle)
    }

    case true => {
      // println("downloading directory: " + file.getTitle)
      val newParent = new java.io.File(parent.getAbsolutePath + "/" + file.getTitle)
      newParent.mkdir
      val childFiles = DriveUtils.getFilesByParent(drive, file)
      childFiles.foreach(downloadFileRecursive(drive, _, newParent))
    }

  }



  /**
   * Delete a file from Drive recursively (works with folders).
   *
   * @param drive       Drive service
   * @param file        file to delete
   */
  def deleteFileRecursive(drive: Drive, file: File): Unit = file.getMimeType.equals(DriveUtils.FOLDER_TYPE) match {

    case false => {
      // println("deleting file: " + file.getTitle)
      DriveUtils.deleteFile(drive, file)
    }

    case true => {
      // println("deleting directory: " + file.getTitle)
      val childFiles = DriveUtils.getFilesByParent(drive, file)
      childFiles.foreach(deleteFileRecursive(drive, _))
      DriveUtils.deleteFile(drive, file)
    }

  }



  /**
   * Create a path of folders recursively in Drive, doing nothing
   * if they already exist.
   *
   * @param drive         Drive service
   * @param parent        parent of subFolders
   * @param subFolders    list of path elements below parent to desired folder
   * @return              last folder in the list of path elements or nothing
   */
  def createFolders(drive: Drive, parent: File, subFolders: List[String]): Option[File] = subFolders match {

    case Nil => Some(parent)

    case x :: xs => {
      // check if folder named x exists
      val curFolderOption = DriveUtils.getFileByParentAndTitle(drive, parent, x)
      // if it doesn't exist, create it
      val newParent = curFolderOption match {
        case Some(f) => {
          if (f.getMimeType.equals(DriveUtils.FOLDER_TYPE)) {
            // x is a folder under parent
            // println("folder exists: " + x)
            f
          } else {
            // x is a file under parent
            // println("file exists: " + x)
            return None
          }

        }
        case None => {
          // println("created: " + x)
          DriveUtils.createFolder(drive, x, parent)
        }
      }

      // recurse
      createFolders(drive, newParent, xs)
    }

  }



}
