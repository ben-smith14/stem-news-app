# stem-news-app

This is an Android app that uses The Guardian Web API to display STEM (Science, Technology, Engineering and Mathematics) new stories to the user that they can click on to open the story in a web browser. The app includes infinite scrolling capabilities (until no more results are returned from the HTTP request), so new entries are constantly loaded in as the user reaches the bottom of the currently displayed list. The entries can also be refreshed by pulling the list down when at the top or selecting the option from the overflow menu in the toolbar if elsewhere in the list.

I used my personal API key from the project gradle.properties file, but this is not included in the GitHub repo, so other users will need to get their own API Key from:

https://open-platform.theguardian.com/access/on
     
To include your personal key in the app, add it to the project's gradle.properties file and use the following link as a guide to include it in your build.gradle (Module:app) file under the name GuardianAPIKey:

https://medium.com/code-better/hiding-api-keys-from-your-android-repository-b23f5598b906

For test purposes, you can simply replace the GuardianAPIKey call in the build.gradle (Module:app) file with the String "test", but this only gives you a limited number of calls to the servers.

This project was another of the significant tasks assigned to us in the Android Basics Nanodegree on udacity.com.
