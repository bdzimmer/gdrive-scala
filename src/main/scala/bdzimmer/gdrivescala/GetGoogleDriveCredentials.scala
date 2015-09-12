// Copyright (c) 2015 Ben Zimmer. All rights reserved.

// Maually get Google Drive credentials for an account.

// Based on code from this website:
// https://developers.google.com/drive/web/quickstart/quickstart-java

// Ben Zimmer

// 2015-04: Created.
// 2015-08-29: Style fixes. Reads / writes JSON files.

package bdzimmer.gdrivescala

import java.awt.Desktop
import java.io.{BufferedReader, InputStreamReader}
import java.net.URI
import java.util.Arrays
// import java.nio.charset.StandardCharsets

import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleCredential}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.DriveScopes

import org.apache.commons.io.FileUtils


object GetGoogleDriveCredentials {

  def main(args: Array[String]): Unit = {

    // Get these by creating a new OAuth client ID in Google Developers Console.
    // Set the application type to "Installed Application"
    // Click "Download JSON" to get JSON file.
    // First argument is path to location of this file.

    val inputDirname = args.head
    val inputDir = new java.io.File(inputDirname)

    val inputFileOption = for {
      inputDir <- inputDir.exists match {
        case true => {
          println("input directory exists")
          Some(inputDir)}
        case false => None
      }

      inputFile <- (inputDir
        .listFiles
        .filter(x => x.getName.startsWith("client_secret") && x.getName.endsWith(".json"))
        .headOption)

    } yield inputFile

    inputFileOption match {
      case Some(inputFile) => {

        val clientId = DriveBuilder.getClientIdFromJsonFile(inputFile)

        val httpTransport = new NetHttpTransport()
        val jsonFactory = new JacksonFactory()

        val flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory,
            clientId.clientId, clientId.clientSecret,
            Arrays.asList(DriveScopes.DRIVE)).build

        val url = flow.newAuthorizationUrl().setRedirectUri(clientId.redirectUri)
          .setAccessType("offline")
          .setApprovalPrompt("force").build()

        // open default web browser with URL
        Desktop.getDesktop.browse(new URI(url))

        println("Enter the authorization code:")
        val br = new BufferedReader(new InputStreamReader(System.in))
        val code = br.readLine()

        val response = flow.newTokenRequest(code).setRedirectUri(clientId.redirectUri).execute

        // I don't think that this is necessary here. I'm after the access / refresh tokens,
        // and those are in the response JSON.
        /*
        val credential = new GoogleCredential.Builder()
          .setTransport(httpTransport)
          .setJsonFactory(jsonFactory)
          .setClientSecrets(clientId.clientId, clientId.clientSecret)
          .build
          .setFromTokenResponse(response)


        println("Access token: " + credential.getAccessToken)
        println("Refresh token: " + credential.getRefreshToken)
        */

        // save the response to a file
        val outputFilename = inputDir + "/access_token.json"
        val outputFile = new java.io.File(outputFilename)
        FileUtils.writeStringToFile(
            outputFile, response.toPrettyString,
            "UTF-8")

        println("created " + outputFilename)
      }

      case None => println("client_secret*.json not found!")
    }

  }
}
