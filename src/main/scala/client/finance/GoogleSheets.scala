package client.finance

import java.net.URL
import java.util.Collections

import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleCredential}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.drive.DriveScopes
import com.google.gdata.client.spreadsheet.SpreadsheetService
import com.google.gdata.data.spreadsheet.SpreadsheetEntry

object GoogleSheets {
  def APPLICATION_NAME = ""


  def main(args: Array[String]) {
    val accessToken = ""
    new GoogleCredential().setAccessToken(accessToken)
  }

  def initializeFlow() = {
    new GoogleAuthorizationCodeFlow.Builder(
      new NetHttpTransport(), JacksonFactory.getDefaultInstance,
      "745595407424.apps.googleusercontent.com", "o4Ev_ojAAWI45wunn8ARtK4X",
      Collections.singleton(DriveScopes.DRIVE)).setDataStoreFactory(
        MemoryDataStoreFactory.getDefaultInstance).setAccessType("offline").build()
  }

  def old {
    val service = new SpreadsheetService("org.intracer.finance")
    service.setProtocolVersion(SpreadsheetService.Versions.V3)

    val SPREADSHEET_FEED_URL = new URL("https://docs.google.com/spreadsheets/d/1OSiu31XSl8C4w80bmpVpedvk4LFlouIHHof2onT2gR8/edit?usp=sharing")
    val spreadsheet = service.getEntry(SPREADSHEET_FEED_URL, classOf[SpreadsheetEntry])

    val worksheets = spreadsheet.getWorksheets
  }
}
