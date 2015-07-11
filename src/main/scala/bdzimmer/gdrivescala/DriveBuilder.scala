// Copyright (c) 2015 Ben Zimmer. All rights reserved.

// 2015-07-10: Refactored from DriveUtils.

package bdzimmer.gdrivescala

import java.io.FileInputStream
import java.util.Properties

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive



case class GoogleDriveKeys(CLIENT_ID: String, CLIENT_SECRET: String, ACCESS_TOKEN: String, REFRESH_TOKEN: String, REDIRECT_URI: String)



/**
 * Object for creating a Google Drive service from keys.
 */
object DriveBuilder {



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
