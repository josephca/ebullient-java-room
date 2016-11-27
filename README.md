# Microservices with a Game On! Room

    
## Building the app locally

1. `cd gameon-room-java`
2. `mvn install`
3. `mvn liberty:run-server`
  After running this, you will have the server running locally at [http://localhost:9080/](http://localhost:9080/).
  You can use a browser extension to play with the WebSocket according to the
  [Game On! WebSocket protocol](https://book.game-on.org/microservices/WebSocketProtocol.html).

## Building locally with docker 

Once docker is installed, then you deploy your room with

* `mvn package` to build your room.
* Create a file called `docker-compose.override.yml` which contains the folllowing
```
gojava:
 volumes:
   - './target/wlp/usr/servers/gojava-room:/opt/ibm/wlp/usr/servers/defaultServer'
```
* `docker-compose build`
* `docker-compose up`

Note: you can optionally use `docker-compose up -d` to run the container as a background process. Use `docker-compose stop` to stop the container.

After this you will have a docker container with your room, running Liberty, and listening on port 9080.
* If you’re running a \*nix variant, you can access it at http://127.0.0.1:9080
* If you’re running Mac or Windows, access it using the [IP of the host](https://gameontext.gitbooks.io/gameon-gitbook/content/walkthroughs/local-docker.html#dockerhost)

A note about `docker-compose.override.yml`, this is an override file that can be used to change, or add to, an existing docker build file. In this case, it maps the file system on the local machine into the dropins directory for the Liberty server running inside the container. The end result is that if you make some changes to your code and run `mvn package` again to rebuild your war file, then Liberty will see that the file has changed and automatically reload your room without having to build or restart the container.

#### Debugging your room

It is possible to attach a debugger to your room so that you can set breakpoints and step through code. Add the following lines to the `docker-compose.override.yml` file

```
ports:
 - "7777:7777"
environment:
 - LIBERTY_MODE: debug
```

The `ports` section instructs docker to expose the port 7777 from inside the container, so that the debugger can attach. The `environment` statement sets an environment variable called `LIBERTY_MODE` to debug. This variable is read by the Liberty startup script and controls how the server is started, in this case in debug mode.
