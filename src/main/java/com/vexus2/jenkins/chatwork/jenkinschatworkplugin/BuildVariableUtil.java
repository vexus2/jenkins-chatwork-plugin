package com.vexus2.jenkins.chatwork.jenkinschatworkplugin;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.VariableResolver;

import java.util.HashMap;
import java.util.Map;

public class BuildVariableUtil {
  public static String resolve(String source, AbstractBuild build, TaskListener listener) {
    return resolve(source, build, listener, new HashMap<String, String>());
  }

  public static String resolve(String source, AbstractBuild build, TaskListener listener, Map<String, String> extraVariables) {
    VariableResolver<String> buildVariableResolver = build.getBuildVariableResolver();
    String resolved = Util.replaceMacro(source, buildVariableResolver);

    try {
      EnvVars envVars = build.getEnvironment(listener);
      envVars.putAll(extraVariables);
      return envVars.expand(resolved);
    } catch (Exception e) {
      return resolved;
    }
  }
}
