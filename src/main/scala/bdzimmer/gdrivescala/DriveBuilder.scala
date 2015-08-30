// Copyright (c) 2015 Ben Zimmer. All rights reserved.

// 2015-07-10: Refactored from DriveUtils.
// 2015-08-29: Style fixes. Separating client id and access token; load from JSON files.

package bdzimmer.gdrivescala

import java.io.FileInputStream
import java.util.Properties

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive

import net.liftweb.json.JsonParser
import net.liftweb.json.JsonParser._
import net.liftweb.json.JsonAST._

import org.apache.commons.io.FileUtils

import scala.collection.JavaConverters._


case class GoogleDriveKeys(
    id: GoogleClientId,
    token: GoogleAccessToken
)

case class GoogleClientId(
    clientId: String,
    clientSecret: String,
    redirectUri: String
)

case class GoogleAccessToken(
    accessToken: String,
    refreshToken: String
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
     .setClientSecrets(keys.id.clientId, keys.id.clientSecret)
     .build

    credential.setAccessToken(keys.token.accessToken)
    credential.setRefreshToken(keys.token.refreshToken)

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
      id = GoogleClientId(
        clientId = prop.getProperty("CLIENT_ID"),
        clientSecret = prop.getProperty("CLIENT_SECRET"),
        redirectUri = prop.getProperty("REDIRECT_URI")
      ),
      token = GoogleAccessToken(
        accessToken = prop.getProperty("ACCESS_TOKEN"),
        refreshToken = prop.getProperty("REFRESH_TOKEN")
      )
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
      id = GoogleClientId(
        clientId = scala.util.Properties.envOrElse("GOOGLE_CLIENT_ID", ""),
        clientSecret = scala.util.Properties.envOrElse("GOOGLE_CLIENT_SECRET", ""),
        redirectUri = scala.util.Properties.envOrElse("GOOGLE_REDIRECT_URI", "")
      ),
      token = GoogleAccessToken(
        accessToken = scala.util.Properties.envOrElse("GOOGLE_ACCESS_TOKEN", ""),
        refreshToken = scala.util.Properties.envOrElse("GOOGLE_REFRESH_TOKEN", "")
      )
    )
  }



  /**
   * Load Google Drive client id from a JSON file.
   *
   * @param inputFile      JSON file to read
   * @return client id object
   */
  def getClientIdFromJsonFile(inputFile: java.io.File): GoogleClientId = {

    val json = FileUtils.readLines(inputFile).asScala.mkString("\n")
    val main = JsonParser.parse(json) \ "installed"

    val clientId = extractStr(main \ "client_id")
    val clientSecret = extractStr(main \ "client_secret")
    val redirectUri = (main \ "redirect_uris") match {
      case JArray(x) => x.headOption.map(x => extractStr(x)).getOrElse("")
      case _ => ""
    }

    GoogleClientId(
      clientId = clientId,
      clientSecret = clientSecret,
      redirectUri = redirectUri
    )
  }



  /**
   * Load Google Drive access token from a JSON file.
   *
   * @param inputFile     JSON file to read
   * @return access token object
   */
  def getAccessTokenFromJsonFile(inputFile: java.io.File): GoogleAccessToken = {

    val json = FileUtils.readLines(inputFile).asScala.mkString("\n")
    val main = JsonParser.parse(json)

    val accessToken = extractStr(main \ "access_token")
    val refreshToken = extractStr(main \ "refresh_token")

    GoogleAccessToken(
      accessToken = accessToken,
      refreshToken = refreshToken
    )
  }


  // helper function for parsing JSON
  private def extractStr(jval: JValue, default: String = ""): String = jval match {
    case JString(x) => x
    case _ => default
  }

}
