package com.vexus2.jenkins.chatwork.jenkinschatworkplugin

import hudson.EnvVars
import hudson.model.AbstractBuild
import hudson.model.Result
import hudson.model.TaskListener
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

import static org.mockito.Matchers.any
import static org.mockito.Mockito.*

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

  static class createMessage {
    ChatworkPublisher publisher
    AbstractBuild mockBuild

    @Before
    void setUp(){
      // via. https://gist.github.com/gjtorikian/5171861
      String payload = """
{
   "after":"1481a2de7b2a7d02428ad93446ab166be7793fbb",
   "before":"17c497ccc7cca9c2f735aa07e9e3813060ce9a6a",
   "commits":[
      {
         "added":[

         ],
         "author":{
            "email":"lolwut@noway.biz",
            "name":"Garen Torikian",
            "username":"octokitty"
         },
         "committer":{
            "email":"lolwut@noway.biz",
            "name":"Garen Torikian",
            "username":"octokitty"
         },
         "distinct":true,
         "id":"c441029cf673f84c8b7db52d0a5944ee5c52ff89",
         "message":"Test",
         "modified":[
            "README.md"
         ],
         "removed":[

         ],
         "timestamp":"2013-02-22T13:50:07-08:00",
         "url":"https://github.com/octokitty/testing/commit/c441029cf673f84c8b7db52d0a5944ee5c52ff89"
      },
      {
         "added":[

         ],
         "author":{
            "email":"lolwut@noway.biz",
            "name":"Garen Torikian",
            "username":"octokitty"
         },
         "committer":{
            "email":"lolwut@noway.biz",
            "name":"Garen Torikian",
            "username":"octokitty"
         },
         "distinct":true,
         "id":"36c5f2243ed24de58284a96f2a643bed8c028658",
         "message":"This is me testing the windows client.",
         "modified":[
            "README.md"
         ],
         "removed":[

         ],
         "timestamp":"2013-02-22T14:07:13-08:00",
         "url":"https://github.com/octokitty/testing/commit/36c5f2243ed24de58284a96f2a643bed8c028658"
      },
      {
         "added":[
            "words/madame-bovary.txt"
         ],
         "author":{
            "email":"lolwut@noway.biz",
            "name":"Garen Torikian",
            "username":"octokitty"
         },
         "committer":{
            "email":"lolwut@noway.biz",
            "name":"Garen Torikian",
            "username":"octokitty"
         },
         "distinct":true,
         "id":"1481a2de7b2a7d02428ad93446ab166be7793fbb",
         "message":"Rename madame-bovary.txt to words/madame-bovary.txt",
         "modified":[

         ],
         "removed":[
            "madame-bovary.txt"
         ],
         "timestamp":"2013-03-12T08:14:29-07:00",
         "url":"https://github.com/octokitty/testing/commit/1481a2de7b2a7d02428ad93446ab166be7793fbb"
      }
   ],
   "compare":"https://github.com/octokitty/testing/compare/17c497ccc7cc...1481a2de7b2a",
   "created":false,
   "deleted":false,
   "forced":false,
   "head_commit":{
      "added":[
         "words/madame-bovary.txt"
      ],
      "author":{
         "email":"lolwut@noway.biz",
         "name":"Garen Torikian",
         "username":"octokitty"
      },
      "committer":{
         "email":"lolwut@noway.biz",
         "name":"Garen Torikian",
         "username":"octokitty"
      },
      "distinct":true,
      "id":"1481a2de7b2a7d02428ad93446ab166be7793fbb",
      "message":"Rename madame-bovary.txt to words/madame-bovary.txt",
      "modified":[

      ],
      "removed":[
         "madame-bovary.txt"
      ],
      "timestamp":"2013-03-12T08:14:29-07:00",
      "url":"https://github.com/octokitty/testing/commit/1481a2de7b2a7d02428ad93446ab166be7793fbb"
   },
   "pusher":{
      "email":"lolwut@noway.biz",
      "name":"Garen Torikian"
   },
   "ref":"refs/heads/master",
   "repository":{
      "created_at":1332977768,
      "description":"",
      "fork":false,
      "forks":0,
      "has_downloads":true,
      "has_issues":true,
      "has_wiki":true,
      "homepage":"",
      "id":3860742,
      "language":"Ruby",
      "master_branch":"master",
      "name":"testing",
      "open_issues":2,
      "owner":{
         "email":"lolwut@noway.biz",
         "name":"octokitty"
      },
      "private":false,
      "pushed_at":1363295520,
      "size":2156,
      "stargazers":1,
      "url":"https://github.com/octokitty/testing",
      "watchers":1
   }
}
"""

      String roomId = "00000000"
      String defaultMessage = '$PAYLOAD_SUMMARY'
      boolean notifyOnSuccess = true
      boolean notifyOnFail = true
      publisher = new ChatworkPublisher(roomId, defaultMessage, notifyOnSuccess, notifyOnFail)

      mockBuild = mock(AbstractBuild)
      when(mockBuild.getBuildVariables()).thenReturn(['payload': payload])
      when(mockBuild.getEnvironment(any(TaskListener))).thenReturn(new EnvVars(['JAVA_HOME': '/Library/Java/JavaVirtualMachines/1.7.0u.jdk/Contents/Home']))
      when(mockBuild.getResult()).thenReturn(Result.SUCCESS)

      publisher.build = mockBuild
    }

    @Test
    void "When contains payload param, should resolve payload"(){
      String excepted = """
Garen Torikian pushed into testing,
- Test
- This is me testing the windows client.
- Rename madame-bovary.txt to words/madame-bovary.tx...

https://github.com/octokitty/testing/compare/17c497ccc7cc...1481a2de7b2a
""".trim()

      assert publisher.createMessage() == excepted
    }
  }
}
