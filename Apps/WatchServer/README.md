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

The absolute path (full directory and filename) is passed to the Rapture endpoint via a parameter to allow

The server expects that configuration in a well known place, namely: document://sys.config/watchserver/config. Configuration is covered below.

### System Diagram ###
![System Diagram](/Apps/WatchServer/images/watchservertopology.png)

# Installation and Running #

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
**Start Curtis**
```
docker run -d -p 8080:8080 -p 8665:8665 --link mongo --link rabbit --name curtis incapture/apiserver
```
**Start Rapture UI**
```
docker run -d -p 8000:8000 --link curtis --name rim incapture/rim
```
**Start WatchServer**
```
docker run -d -p 4567:4567 --link mongo --link rabbit --link elasticsearch --link postgres --name restserver incapture/watchserver
```
**Start WatchServer Sample Configuration Plugin**
```
complete when implemented
```
# Configuration #

Configuration is captured in a Rapture document here document://sys.config/watchserver/config.

This allows you to specify:
* Directory to monitor
* What action to take for a create, modify or delete event in that specified directory

A sample (json) document is given as an example:
```
{
  "/Users/jonathanmajor/temp" : [ {
    "event" : "ENTRY_CREATE",
    "action" : "script://watcher/test1"
  }, {
    "event" : "ENTRY_MODIFY",
    "action" : "script://watcher/test1"
  }, {
    "event" : "ENTRY_DELETE",
    "action" : "script://watcher/test2"
  } ],
  "/Users/jonathanmajor/temp1" : [ {
    "event" : "ENTRY_CREATE",
    "action" : "workflow://decisionWorkflow.httpTests/concurrent/test1"
  } ]
}
```
You do not need to specify an event -> action mapping for each event type.
