A service that connects to Slack and automatically marks certain messages as read, as a way to ignore them.

## Slack authentication

### To get your xoxc auth token:

1. Start Slack in Chrome
2. Look in the dev tools console for a call to websocket call (wss://)
3. It should have the token as a `token` parameter in the URL, which should start with 'xoxc'

### To get your 'd' cookie:

1. Start Slack in Chrome
2. In the dev tools console, go to the Application tab, and on Storage/Cookies on the left pane
3. The 'd' should be there, uncheck "Show URL decoded", and copy its value

## Docker instructions

### Building and pushing the image to Docker Hub

```
docker image rm bodlulu/slack-ignore:latest
DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage
```

### Running the image

```
docker pull bodlulu/slack-ignore
docker run bodlulu/slack-ignore <options>
```
