// Copyright (c) 2015 Ben Zimmer. All rights reserved.

// Maually get Google Drive credentials for an account.

// Based on code from this website:
// https://developers.google.com/drive/web/quickstart/quickstart-java

// Ben Zimmer

// 2015-04: Created.
// 2015-08-29: Style fixes.

package bdzimmer.gdrivescala

import java.awt.Desktop
import java.io.{BufferedReader, InputStreamReader}
import java.net.URI
import java.util.Arrays

import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleCredential}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.DriveScopes


object GetGoogleDriveCredentials {

    def main(args: Array[String]): Unit = {

      // Get these by creating a new OAuth client ID in Google Developers Console.
      // Set the application type to "Installed Application"
      // Click "Download JSON" to get JSON containing these.

      val clientId = ""
      val clientSecret = ""
      val redirectUri = ""

      val httpTransport = new NetHttpTransport()
      val jsonFactory = new JacksonFactory()

      val flow = new GoogleAuthorizationCodeFlow.Builder(
          httpTransport, jsonFactory, clientId, clientSecret, Arrays.asList(DriveScopes.DRIVE)).build


      val url = flow.newAuthorizationUrl().setRedirectUri(redirectUri)
        .setAccessType("offline")
        .setApprovalPrompt("force").build()


      // open default web browser with URL
      Desktop.getDesktop.browse(new URI(url))

      println("Enter the authorization code:")
      val br = new BufferedReader(new InputStreamReader(System.in))
      val code = br.readLine()

      val response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute
      val credential = new GoogleCredential.Builder()
        .setTransport(httpTransport)
        .setJsonFactory(jsonFactory)
        .setClientSecrets(clientId, clientSecret)
        .build
        .setFromTokenResponse(response)


      println("Access token: " + credential.getAccessToken)
      println("Refresh token: " + credential.getRefreshToken)

     }


}
