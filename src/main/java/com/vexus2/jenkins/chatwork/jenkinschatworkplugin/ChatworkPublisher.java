package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.VariableResolver;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.HashMap;
import java.util.Map;

public class ChatworkPublisher extends Publisher {

  private final String rid;
  private final String defaultMessage;

  private Boolean notifyOnSuccess;
  private Boolean notifyOnFail;
  private AbstractBuild build;
  private BuildListener listener;


  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public ChatworkPublisher(String rid, String defaultMessage, Boolean notifyOnSuccess, Boolean notifyOnFail) {
    this.rid = rid;
    this.notifyOnSuccess = notifyOnSuccess;
    this.notifyOnFail = notifyOnFail;
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
  
  public Boolean getNotifyOnSuccess() {
    return notifyOnSuccess;
  }

  public Boolean getNotifyOnFail() {
    return notifyOnFail;
  }

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    this.build = build;
    this.listener = listener;
    
    if(this.build.getResult() == Result.SUCCESS && !this.notifyOnSuccess) {
      return true;
    }
    if(this.build.getResult() == Result.FAILURE && !this.notifyOnFail) {
      return true;
    }

    try {
      String message = createMessage();

      println("[ChatWork post message]");
      println(message);

      if (message == null) return false;

      ChatworkClient chatworkClient = new ChatworkClient(build, getDescriptor().getApikey(), getRid(), getDefaultMessage());
      chatworkClient.sendMessage(message);

      return true;
    } catch (Exception e) {
      e.printStackTrace(listener.getLogger());
      return false;
    }
  }

  // print to build console
  private void println(String message) {
    this.listener.getLogger().println(message);
  }

  private String createMessage() throws Exception {
    String message = this.defaultMessage;

    if(StringUtils.isBlank(message)){
      return null;
    }

    Map<String, String> extraVariables = createExtraVariables();
    return BuildVariableUtil.resolve(message, build, listener, extraVariables);
  }

  private Map<String, String> createExtraVariables() {
    Map<String, String> variables = new HashMap<String, String>();

    VariableResolver<String> buildVariableResolver = build.getBuildVariableResolver();
    String payloadJson = buildVariableResolver.resolve("payload");
    if(StringUtils.isNotBlank(payloadJson)){
      variables.put("PAYLOAD_SUMMARY", analyzePayload(payloadJson));
    }

    variables.put("BUILD_RESULT", build.getResult().toString());

    return variables;
  }

  private static String analyzePayload(String payloadJson) {
    JSONObject json;
    try{
      json = JSONObject.fromObject(payloadJson);

    } catch (JSONException e){
      // payloadJson is not json
      return payloadJson;
    }

    if (json.has("action") && "opened".equals(json.getString("action"))) {
      JSONObject pullRequest = json.getJSONObject("pull_request");
      String title = pullRequest.getString("title");
      String url = pullRequest.getString("html_url");
      String repositoryName = json.getJSONObject("repository").getString("name");
      String pusher = pullRequest.getJSONObject("user").getString("login");

      StringBuilder message = new StringBuilder().append(String.format("%s created Pull Request into %s,\n", pusher, repositoryName));
      message.append(String.format("\n%s", title));
      message.append(String.format("\n%s", url));

      return message.toString();

    } else if(json.has("compare")){
      String compareUrl = json.getString("compare");

      String pusher = json.getJSONObject("pusher").getString("name");
      String repositoryName = json.getJSONObject("repository").getString("name");
      StringBuilder message = new StringBuilder().append(String.format("%s pushed into %s,\n", pusher, repositoryName));

      JSONArray commits = json.getJSONArray("commits");
      int size = commits.size();
      for (int i = 0; i < size; i++) {
        JSONObject value = (JSONObject) commits.get(i);
        // コミットメッセージが長くなりすぎることを考慮して文字長を50文字とする
        String s = value.getString("message");
        message.append(String.format("- %s\n", (s.length() > 50) ? s.substring(0, 50) + "..." : s));
      }
      message.append(String.format("\n%s", compareUrl));

      return message.toString();
    }

    return null;
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

