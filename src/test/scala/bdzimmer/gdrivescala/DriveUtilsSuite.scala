// Copyright (c) 2015 Ben Zimmer. All rights reserved.

// Tests for DriveUtils.

// 2015-08-29: Created.

package bdzimmer.gdrivescala

import org.scalatest.FunSuite
import com.google.api.services.drive.model.File
import org.apache.commons.io.FileUtils

import scala.collection.JavaConverters._
import scala.util.control.Exception._


class DriveUtilsSuite extends FunSuite {


  // To prepare for running this suite, the following directory
  // structure must exist in Google Drive:

  //  (drive root)/
  //      gdrive-scala-testing/
  //          test.txt (starts with the text "test 1")
  //          empty.txt
  //      subdir/
  //          test.txt (starts with the text "test 2")

  // Extra files under these directories may cause incorrect test
  // results.


  // for now, get the drive keys from a properties file.
  val drivePropertiesFile = "C:/Ben/tmp/googledrive.properties"
  val keys = DriveBuilder.getKeysFromProperties(drivePropertiesFile)
  val drive = DriveBuilder.getDrive(keys, "DriveTesting")

  val root = DriveUtils.getRoot(drive)
  val testingDirname = "gdrive-scala-testing"
  val resourceDir = getClass.getResource("/testfiles").getPath


  def getTestingParent(): Option[File] = {
    val result = DriveUtils.getFileByParentAndTitle(drive, root, testingDirname)
    assert(result.isDefined)
    result
  }

  test("getFilesByParent") {
    val filesAtRoot = DriveUtils.getFilesByParent(drive, root)
    assert(filesAtRoot.length > 0)
    assert(filesAtRoot map (_.getTitle) contains(testingDirname))
  }


  test("getFileByParentAndTitle") {
    val testingDir = getTestingParent
    testingDir foreach (x => assert(x.getTitle.equals(testingDirname)))
  }


  test("getFileByPath") {

    // a couple files that exist, then some that don't
    val test1 = DriveUtils.getFileByPath(drive, root, List(testingDirname, "test.txt"))
    assert(test1.isDefined)
    val test2 = DriveUtils.getFileByPath(drive, root, List(testingDirname, "subdir", "test.txt"))
    assert(test2.isDefined)

    // file doesn't exist
    val test3 = DriveUtils.getFileByPath(drive, root, List(testingDirname, "junk.txt"))
    assert(test3.isEmpty)
    // folder doesn't exist
    val test4 = DriveUtils.getFileByPath(drive, root, List(testingDirname, "junkdir", "test.txt"))
    assert(test4.isEmpty)

  }


  test("getFileText") {
    val testFile1 = DriveUtils.getFileByPath(drive, root, List(testingDirname, "test.txt"))
    assert(testFile1.isDefined)
    testFile1 foreach (x => {
      val contents = DriveUtils.getFileText(drive, x)
      assert(contents.equals("test 1"))
    })
  }


  test("uploadFile / downloadFile / deleteFile") {

    val testingDirOption = getTestingParent

    testingDirOption foreach (testingDir => {

      val testFilename = "test3.txt"
      val testContents = "test 3"

      // upload
      val driveFile = DriveUtils.uploadFile(
          drive, resourceDir + "/" + testFilename, testingDir)

      // confirm file exists by name
      assert(DriveUtils.getFileByParentAndTitle(drive, testingDir, testFilename).isDefined)

      // confirm valid return file by checking contents
      assert(DriveUtils.getFileText(drive, driveFile).equals(testContents))

      // download to local
      val localFilename = resourceDir + "/" + testFilename + "_downloaded"
      DriveUtils.downloadFile(drive, driveFile, localFilename)
      val localFile = new java.io.File(localFilename)

      // verify existence / contents of local file
      val contents = FileUtils.readLines(localFile).asScala.mkString("\n")
      assert(contents.equals(testContents))

      // delete drive file and verify no longer present
      DriveUtils.deleteFile(drive, driveFile)
      assert(DriveUtils.getFileByParentAndTitle(drive, testingDir, testFilename).isEmpty)

      // delete local file
      localFile.delete


    })

  }


  test("download empty file exception") {

    // currently, downloadFile doesn't seem to work with empty files.

    val testFilename = "empty.txt"

    val testingDirOption = getTestingParent
    testingDirOption foreach (testingDir => {
      val emptyFileOption = DriveUtils.getFileByParentAndTitle(drive, testingDir, testFilename)
      assert(emptyFileOption.isDefined)

      // even though the empty file exists, there's an exception when we try to download it.
      emptyFileOption foreach (emptyFile => {
        val result = allCatch opt DriveUtils.downloadFile(drive, emptyFile, resourceDir + "/" + testFilename)
        assert(result.isEmpty)
      })

    })

  }


  test("createFolder") {

    val testingDirOption = getTestingParent

    testingDirOption foreach (testingDir => {

      val testFoldername = "junk"

      val folder = DriveUtils.createFolder(drive, testFoldername, testingDir)
      assert(folder.getMimeType.equals(DriveUtils.FOLDER_TYPE))

      // confirm file exists by name
      assert(DriveUtils.getFileByParentAndTitle(drive, testingDir, testFoldername).isDefined)

      // delete and verify no longer present
      DriveUtils.deleteFile(drive, folder)
      assert(DriveUtils.getFileByParentAndTitle(drive, testingDir, testFoldername).isEmpty)

    })

  }


  test("recursive upload / download / delete") {

    // this test covers the code, but could test usage / correctness much more thoroughly.

    val testingDirOption = getTestingParent

    testingDirOption foreach (testingDir => {

      val testDirname = "alpha"
      val testDirFile = new java.io.File(resourceDir + "/" + testDirname)

      val filesToCheck = List(
          (List("alpha", "beta", "notempty.txt"), "test"),
          (List("alpha", "beta", "test4.txt"), "test 4"),
          (List("alpha", "gamma", "test5.txt"), "test 5"))


      // upload
      DriveUtils.uploadFileRecursive(drive, testDirFile, testingDir)

      // TODO: nesting could be avoided if uploadRecursive returned a File
      val driveFolderOption = DriveUtils.getFileByParentAndTitle(drive, testingDir, testDirname)
      assert(driveFolderOption.isDefined)

      driveFolderOption foreach (driveFolder => {

        // confirm files exists by name and contents
        filesToCheck map ({case (path, contents) => {
          // println("verifying uploaded")
          val curFileOption = DriveUtils.getFileByPath(drive, testingDir, path)
          assert(curFileOption.isDefined)

          curFileOption foreach (curFile => {
            assert(DriveUtils.getFileText(drive, curFile).equals(contents))
          })

        }})


        // download to local
        val localDirname = resourceDir + "/" + testDirname + "_downloaded"
        val localDirFile = new java.io.File(localDirname)
        localDirFile.mkdir()
        DriveUtils.downloadFileRecursive(drive, driveFolder, localDirFile)

        // verify existence / contents of local files
        filesToCheck map ({case (path, contents) => {
          // println("verifying downloaded")
          val curFile = new java.io.File(localDirname + "/" + path.mkString("/"))
          assert(curFile.exists)
          assert(FileUtils.readLines(curFile).asScala.mkString("\n").equals(contents))
        }})


        // delete drive folder and verify no longer present
        DriveUtils.deleteFileRecursive(drive, driveFolder)
        val driveFolderDeleted = DriveUtils.getFileByParentAndTitle(drive, testingDir, testDirname)
        assert(driveFolderDeleted.isEmpty)

        filesToCheck map ({case (path, contents) => {
          // println("verifying deleted")
          val curFileOption = DriveUtils.getFileByPath(drive, testingDir, path)
          assert(curFileOption.isEmpty)
        }})

        // delete local directory
        FileUtils.deleteDirectory(localDirFile)

      })

    })

  }


  test("createFolders") {

    val testingDirOption = getTestingParent

    testingDirOption foreach (testingDir => {

      val test1 = DriveUtils.createFolders(
          drive, testingDir, List("alpha"))
      assert(test1.isDefined)

      val test2 = DriveUtils.createFolders(
          drive, testingDir, List("alpha", "beta", "delta"))
      assert(test2.isDefined)

      val test3 = DriveUtils.createFolders(
          drive, testingDir, List("alpha", "beta", "delta", "epsilon", "gamma"))
      assert(test3.isDefined)

      // this one will fail since "test.txt" is a file under subdir
      val test4 = DriveUtils.createFolders(
          drive, testingDir, List("subdir", "test.txt", "delta", "epsilon", "gamma"))
      assert(test4.isEmpty)

      // delete drive folder
      test1 foreach (x => DriveUtils.deleteFileRecursive(drive, x))

    })
  }

}
