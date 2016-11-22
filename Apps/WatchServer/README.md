# WatchServer

### Overview ###
WatchServer is a Rapture based application that detects changes in a file directory and then elicits actions in a Rapture system.

Under the hood it uses Java [WatchService](https://docs.oracle.com/javase/8/docs/api/java/nio/file/WatchService.html) and was introduced in Java 1.7.
The WatchService allows you to:
* Register directories to watch
* Detect events: ENTRY_CREATE, ENTRY_MODIFY and ENTRY_DELETE

The WatchServer allows you to register multiple directories and map to actions (or endpoints) in a Rapture system.

Supported actions are:
* Run a local (to the WatchServer) script
* Run a script using the [event api](http://repo.incapturesolutions.com/apidoc/#_event-api)
* Run a workflow

The absolute path (full directory and filename) is passed to the Rapture endpoint via a parameter to allow that program to process or enrich the file.

The server expects that configuration in a well known place, namely: document://sys.config/watchserver/config. Configuration is covered below.

### System Diagram ###
![System Diagram](/Apps/WatchServer/images/watchservertopology.png)

# Installation and Running #

This example will illustrate the configuration and usage of WatchServer.

Any files created in /opt/test will cause WatchServer to call the following workflow: workflow://workflows/incapture/watchserver/wsload

This workflow will do the following:
1. Load the file, in /opt/test/, specified by the parameter passed to it from WatchServer
2. Store the unprocessed file to a blob://archive/<filename>_<date> repository
3. Using blob created in step 2 process the file and write to a new blob://processed/<newfile>_<date>


## Using Docker ##
The entire setup can be run using all docker containers.  This is the easiest way to run the stack without doing builds or compiling.  Here are the relevant commands.

**Start RabbitMQ**
```
docker run -d -p 5672:5672 -p 15672:15672 --name rabbit incapture/rabbit
```
**Start MongoDB**
```
docker run -d -p 27017:27017 --name mongo incapture/mongo
```
**Create a Local Data Volume**
```
docker volume create --name fileDropVolume
docker run -v fileDropVolume:/opt/test alpine mkdir /opt/test/subfolder
```
**Start API Server**
```
docker run -d -p 8080:8080 -p 8665:8665 --link mongo --link rabbit -v fileDropVolume:/opt/test  --name curtis incapture/apiserver:xESlatest
```
**Start Rapture UI**
```
docker run -d -p 8000:8000 --link curtis --name rim incapture/rim
```
Checkpoint: To ensure environment is up login to your local environment on http://localhost:8000/browser

**Start WatchServer Sample Configuration Plugin**
```
docker run --link curtis incapture/watchserverplugin
```
**Start WatchServer**
```
docker run -d --link mongo --link rabbit -v fileDropVolume:/opt/test --name watchserver incapture/watchserver
```

Checkpoint: To ensure WatchServer has started log should have "20:00:11,899  INFO [main] (WatchServer.java:98) <> [] - WatchServer started and ready."

# Configuration #

Configuration is captured in a Rapture document here document://sys.config/watchserver/config.

This allows you to specify:
* Directory to monitor
* What action to take for a create, modify or delete event in that specified directory

A sample (json) document is given as an example and is installed to your local environment:
```
{
  "/opt/test/subfolder" : [ {
    "event" : "ENTRY_CREATE",
    "action" : "script://scripts/incapture/watchserver/createaction"
  }, {
    "event" : "ENTRY_MODIFY",
    "action" : "script://scripts/incapture/watchserver/modifyaction"
  } ],
  "/opt/test" : [ {
    "event" : "ENTRY_CREATE",
    "action" : "workflow://workflows/incapture/watchserver/wsload"
  } ]
}
```

Expected Behavior:
1. A file dropped into /opt/test/subfolder will run script script://scripts/incapture/watchserver/createaction
2. if that file (in step 1) changes this script will be run script://scripts/incapture/watchserver/modifyaction
3. A file dropped into /opt/test will run the following workflow workflow://workflows/incapture/watchserver/wsload

You do not need to specify an event -> action mapping for each event type.
