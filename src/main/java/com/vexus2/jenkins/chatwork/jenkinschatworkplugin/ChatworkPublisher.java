package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class ChatworkPublisher extends Publisher {

  private final String rid;
  private final String defaultMessage;

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
    try {
      ChatworkClient chatworkClient = new ChatworkClient(build, getDescriptor().getApikey(), getRid(), getDefaultMessage());
      chatworkClient.sendMessage();
    }catch (Exception e) {
      result = false;
      listener.getLogger().println(e.getMessage());
    }
    return result;
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
      return "Jenkins Chatwork Plugin";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      apikey = formData.getString("apikey");
      save();
      return super.configure(req, formData);
    }
  }
}

