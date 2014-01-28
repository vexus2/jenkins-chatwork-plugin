package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.VariableResolver;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatworkPublisher extends Publisher {

  private final String rid;
  private final String defaultMessage;

  private static final Pattern pattern = Pattern.compile("\\$\\{(.+)\\}|\\$(.+)\\s?");
  private AbstractBuild build;
  //TODO:
  private PrintStream logger;

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public ChatworkPublisher(String rid, String defaultMessage) {
    this.rid = rid;

    this.defaultMessage = (defaultMessage != null) ? defaultMessage : "";
  }

  /**
   * We'll use this from the <tt>config.jelly</tt>.
   */
  public String getRid() {
    return rid;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

    Boolean result = true;
    this.build = build;
    this.logger = listener.getLogger();
    try {

      String message = createMessage();

      ChatworkClient chatworkClient = new ChatworkClient(build, getDescriptor().getApikey(), getRid(), getDefaultMessage());
      chatworkClient.sendMessage(message);
    } catch (Exception e) {
      result = false;
      listener.getLogger().println(e.getMessage());
    }
    return result;
  }

  private String createMessage() throws Exception {
    String message = this.defaultMessage;
    Matcher m = pattern.matcher(message);
    while (m.find()) {
      // If ${VARNAME} match found, return that group, else return $NoWhiteSpace group
      String matches = (m.group(1) != null) ? m.group(1) : m.group(2);

      String globalValue = getValue(matches);
      if (globalValue != null) {
        message = message.replaceAll(matches, globalValue);
      }
    }
    return message;

  }

  private String getValue(String key) {
    if (key == null) {
      return null;
    } else {
      VariableResolver buildVariableResolver = build.getBuildVariableResolver();
      Object defaultValue = buildVariableResolver.resolve(key);
      return (defaultValue == null) ? "" : ("payload".equals(key)) ? analyzePayload(defaultValue.toString()) : defaultValue.toString();
    }
  }

  private String analyzePayload(String parameterDefinition) {

    JSONObject json = JSONObject.fromObject(parameterDefinition);

    //TODO: 設定画面で表示したい項目を選べるようにする
    String action = json.getString("action");
    StringBuilder message;
    if (action != null && "opened".equals(action)) {
      JSONObject pull_request = json.getJSONObject("pull_request");
      String title = pull_request.getString("title");
      String url = pull_request.getString("url");
      String repositoryName = json.getJSONObject("repository").getString("name");
      String pusher = pull_request.getJSONObject("user").getString("login");

      message = new StringBuilder().append(String.format("%s created Pull Request into %s,\n\n", pusher, repositoryName));
      message.append(String.format("\n%s", title));
      message.append(String.format("\n%s", url));
    } else {

      String compareUrl = json.getString("compare");
      String pusher = json.getJSONObject("pusher").getString("name");
      String repositoryName = json.getJSONObject("repository").getString("name");
      message = new StringBuilder().append(String.format("%s pushed into %s,\n", pusher, repositoryName));

      JSONArray commits = json.getJSONArray("commits");
      int size = commits.size();
      for (int i = 0; i < size; i++) {
        JSONObject value = (JSONObject) commits.get(i);
        // コミットメッセージが長くなりすぎることを考慮して文字長を50文字とする
        String s = value.getString("message");
        message.append(String.format("- %s \n", (s.length() > 50) ? s.substring(0, 50) + "..." : s));
      }
      message.append(String.format("\n%s", compareUrl));

    }

    return message.toString();
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  /**
   * Descriptor for {@link ChatworkPublisher}. Used as a singleton.
   * The class is marked as public so that it can be accessed from views.
   * <p/>
   * <p/>
   * See <tt>src/main/resource/com.vexus2.jenkins.chatwork.jenkinschatworkplugin/ChatworkPublisher/*.jelly</tt>
   * for the actual HTML fragment for the configuration screen.
   */
  @Extension
  // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private String apikey;

    public String getApikey() {
      return apikey;
    }

    public DescriptorImpl() {
      load();
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    public String getDisplayName() {
      return "Notify the ChatWork";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      apikey = formData.getString("apikey");
      save();
      return super.configure(req, formData);
    }
  }
}

