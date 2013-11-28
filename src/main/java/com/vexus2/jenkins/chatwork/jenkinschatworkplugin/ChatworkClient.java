package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

import hudson.model.AbstractBuild;
import hudson.model.CauseAction;
import hudson.scm.ChangeLogSet;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ChatworkClient {

  private final String API_KEY;

  private final String CHANNEL_ID;

  private final AbstractBuild BUILD;

  private final String DEFAULT_MESSAGE;

  public static final String API_URL = "https://api.chatwork.com/v1";


  public ChatworkClient(AbstractBuild build, String apiKey, String channelId, String defaultMessage) {
    this.BUILD = build;
    this.API_KEY = apiKey;
    this.CHANNEL_ID = channelId;
    this.DEFAULT_MESSAGE = defaultMessage;
  }

  public boolean sendMessage() throws Exception {
    if (this.BUILD == null || this.API_KEY == null || this.CHANNEL_ID == null) {
      throw new Exception("API Key or Channel ID is null");
    }

    String message = createMessage();

    String url = new StringBuffer(API_URL).append("/").append(this.CHANNEL_ID).append("/messages").toString();
    URL obj = new URL(url);
    HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

    con.setRequestMethod("POST");
    con.setRequestProperty("X-ChatWorkToken", this.API_KEY);

    String urlParameters = "data=" + DEFAULT_MESSAGE + message;

    con.setDoOutput(true);
    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    wr.writeBytes(urlParameters);
    wr.flush();
    wr.close();

    int responseCode = con.getResponseCode();
    if(responseCode != 200) {
      throw new Exception("Response is not valid. Check your API Key or Chatwork API status. response_code = " + responseCode);
    }
    System.out.println("Response Code : " + responseCode);

    BufferedReader in = new BufferedReader(
        new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer response = new StringBuffer();

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();

    return true;
  }

  private String createMessage() throws Exception {
    StringBuffer message = new StringBuffer();
    String changes = getChanges();
    CauseAction cause = this.BUILD.getAction(CauseAction.class);

    if (changes != null) {
      message.append(changes + "¥n");
    } else if (cause != null) {
      message.append(cause + "¥n");
    }

    return message.toString();
  }

  String getChanges() throws Exception {
    if (!this.BUILD.hasChangeSetComputed()) {
      throw new Exception("No change set computed...");
    }
    ChangeLogSet changeSet = this.BUILD.getChangeSet();
    List<ChangeLogSet.Entry> entries = new LinkedList<ChangeLogSet.Entry>();
    Set<ChangeLogSet.AffectedFile> files = new HashSet<ChangeLogSet.AffectedFile>();
    for (Object o : changeSet.getItems()) {
      ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
      entries.add(entry);
      files.addAll(entry.getAffectedFiles());
    }
    Set<String> authors = new HashSet<String>();
    if (!entries.isEmpty()) {
      for (ChangeLogSet.Entry entry : entries) {
        authors.add(entry.getAuthor().getDisplayName());
      }
    }

    StringBuffer message = new StringBuffer();
    message.append("Started by changes from ");
    message.append(StringUtils.join(authors, ", "));
    message.append(" (");
    message.append(files.size());
    message.append(" file(s) changed)");
    return message.toString();
  }
}
