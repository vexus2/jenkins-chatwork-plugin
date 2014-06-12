package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

import hudson.model.AbstractBuild;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.Proxy;
import java.net.InetSocketAddress;
import java.lang.Integer;

public class ChatworkClient {

  private final String apiKey;

  private final String proxySv;
  private final String proxyPort;

  private final String channelId;

  private final AbstractBuild build;

  private final String defaultMessage;

  private static final String API_URL = "https://api.chatwork.com/v1";

  public ChatworkClient(AbstractBuild build, String apiKey, String proxySv, String proxyPort, String channelId, String defaultMessage) {
    this.build = build;
    this.apiKey = apiKey;
    this.proxySv = proxySv;
    this.proxyPort = proxyPort;
    this.channelId = channelId;
    this.defaultMessage = defaultMessage;
  }

  public boolean sendMessage(String message) throws Exception {
    if (this.build == null || this.apiKey == null || this.channelId == null) {
      throw new Exception("API Key or Channel ID is null");
    }

    String url = API_URL + "/rooms/" + this.channelId + "/messages";
    URL obj = new URL(url);
    HttpsURLConnection con;

    if (this.proxySv.equals("NOPROXY")) {
      con = (HttpsURLConnection) obj.openConnection();
    }
    else {
      Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxySv, Integer.parseInt(this.proxyPort)));
      con = (HttpsURLConnection) obj.openConnection(proxy);
    }

    con.setRequestMethod("POST");
    con.setRequestProperty("X-ChatWorkToken", this.apiKey);
    con.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");

    String urlParameters = "body=" + message;

    con.setDoOutput(true);

    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    try {
      wr.write(urlParameters.getBytes("utf-8"));
      wr.flush();

    } finally {
      IOUtils.closeQuietly(wr);

    }

    con.connect();

    int responseCode = con.getResponseCode();
    if (responseCode != 200) {
      throw new Exception("Response is not valid. Check your API Key or Chatwork API status. response_code = " + responseCode + ", message = " + con.getResponseMessage());
    }

    BufferedReader in = new BufferedReader(
        new InputStreamReader(con.getInputStream()));
    try {
      String inputLine;
      StringBuilder response = new StringBuilder();

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }

    } finally {
      IOUtils.closeQuietly(in);

    }

    return true;
  }



}
