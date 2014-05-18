package com.vexus2.jenkins.chatwork.jenkinschatworkplugin

import hudson.EnvVars
import hudson.model.AbstractBuild
import hudson.model.TaskListener
import hudson.util.LogTaskListener
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

import java.util.logging.Level
import java.util.logging.Logger

import static org.mockito.Mockito.*

@RunWith(Enclosed)
class BuildVariableUtilTest {
  static class resolve {
    AbstractBuild mockBuild
    TaskListener listener
    Map<String, String> extraVariables;

    static final Logger LOGGER = Logger.getLogger(BuildVariableUtilTest.class.getName());

    @Before
    void setUp(){
      listener = new LogTaskListener(LOGGER, Level.INFO)
      mockBuild = mock(AbstractBuild)
      when(mockBuild.getEnvironment(any(TaskListener))).thenReturn(new EnvVars(['JAVA_HOME': '/Library/Java/JavaVirtualMachines/1.7.0u.jdk/Contents/Home']))
      when(mockBuild.getBuildVariables()).thenReturn(['BUILD_NUMBER': '123'])

      extraVariables = ["BUILD_RESULT": "SUCCESS"]
    }

    @Test
    void 'should resolve build variable'(){
      String source = 'BUILD_NUMBER is $BUILD_NUMBER'
      assert BuildVariableUtil.resolve(source, mockBuild, listener, extraVariables) == "BUILD_NUMBER is 123"
    }

    @Test
    void 'should resolve environment variable'(){
      String source = 'JAVA_HOME is $JAVA_HOME'
      assert BuildVariableUtil.resolve(source, mockBuild, listener, extraVariables) == "JAVA_HOME is /Library/Java/JavaVirtualMachines/1.7.0u.jdk/Contents/Home"
    }

    @Test
    void 'should resolve extra variable'(){
      String source = 'BUILD_RESULT is $BUILD_RESULT'
      assert BuildVariableUtil.resolve(source, mockBuild, listener, extraVariables) == "BUILD_RESULT is SUCCESS"
    }
  }
}
