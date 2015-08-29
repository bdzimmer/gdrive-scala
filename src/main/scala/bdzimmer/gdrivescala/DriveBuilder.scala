// Copyright (c) 2015 Ben Zimmer. All rights reserved.

// 2015-07-10: Refactored from DriveUtils.
// 2015-08-29: Style fixes.

package bdzimmer.gdrivescala

import java.io.FileInputStream
import java.util.Properties

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive


// TODO: separate this into two case classes
case class GoogleDriveKeys(
    clientId: String,
    clientSecret: String,
    accessToken: String,
    refreshToken: String,
    redirectUri: String
)



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
     .setClientSecrets(keys.clientId, keys.clientSecret)
     .build

    credential.setAccessToken(keys.accessToken)
    credential.setRefreshToken(keys.refreshToken)

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

    GoogleDriveKeys(
      clientId = prop.getProperty("CLIENT_ID"),
      clientSecret = prop.getProperty("CLIENT_SECRET"),

      accessToken = prop.getProperty("ACCESS_TOKEN"),
      refreshToken = prop.getProperty("REFRESH_TOKEN"),

      redirectUri = prop.getProperty("REDIRECT_URI")
    )
  }



  /**
   * Load Google Drive keys from environment variables
   * (preferred method for Heroku).
   *
   * @return keys object
   */
  def getKeysFromEnvironment(): GoogleDriveKeys = {

    GoogleDriveKeys(
      clientId = scala.util.Properties.envOrElse("GOOGLE_CLIENT_ID", ""),
      clientSecret = scala.util.Properties.envOrElse("GOOGLE_CLIENT_SECRET", ""),

      accessToken = scala.util.Properties.envOrElse("GOOGLE_ACCESS_TOKEN", ""),
      refreshToken = scala.util.Properties.envOrElse("GOOGLE_REFRESH_TOKEN", ""),

      redirectUri = scala.util.Properties.envOrElse("GOOGLE_REDIRECT_URI", "")
    )
  }


}
