package com.vexus2.jenkins.chatwork.jenkinschatworkplugin

import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed)
class ChatworkPublisherTest {
  static class analyzePayload {
    @Test
    void "PullRequestのメッセージが生成されること"(){
      // via. https://developer.github.com/v3/pulls/#get-a-single-pull-request
      String parameterDefinition = """
{
  "action": "opened",
  "repository": {
    "name": "Hello-World"
  },
  "pull_request": {
    "title": "new-feature",
    "html_url": "https://github.com/octocat/Hello-World/pull/1",
    "user": {
      "login": "octocat"
    }
  }
}
"""

      // 末尾に改行コードがあると一致しなくなるのでtrimする
      String expected = """
octocat created Pull Request into Hello-World,

new-feature
https://github.com/octocat/Hello-World/pull/1
""".trim()

      assert ChatworkPublisher.analyzePayload(parameterDefinition) == expected
    }

    @Test
    void "compareのメッセージが生成されること"(){
      String parameterDefinition = """
{
  "compare": "https://github.com/octocat/Hello-World/compare/master...topic",
  "pusher": {
    "name": "octocat",
  },
  "repository": {
    "name": "Hello-World"
  },
  "commits": [
    {"message": "1st commit"},
    {"message": "2nd commit"}
  ]
}
"""

      // 末尾に改行コードがあると一致しなくなるのでtrimする
      String expected = """
octocat pushed into Hello-World,
- 1st commit
- 2nd commit

https://github.com/octocat/Hello-World/compare/master...topic
""".trim()

      assert ChatworkPublisher.analyzePayload(parameterDefinition) == expected
    }

    @Test
    void "PullRequestでもcompareない時はnullが返ること"(){
      // via. https://developer.github.com/v3/pulls/#get-a-single-pull-request
      String parameterDefinition = """
{
}
"""

      assert ChatworkPublisher.analyzePayload(parameterDefinition) == null
    }
  }
}
