
# WatchServer

### Overview ###
WatchServer is an application that detects changes in a file directory and then elicits configured actions in a Rapture system.

The WatchService allows you to:
* Register directories to watch
* Detect events: CREATE, MODIFY and DELETE

Under the hood it uses Apache Commons libraries to create
* Create a [Virtual File System](https://commons.apache.org/proper/commons-vfs/api.html) for FTP server
* Monitor a directory using [File Monitor](http://commons.apache.org/proper/commons-io/)

The WatchServer allows you to register multiple directories and map to actions (or endpoints) in another system.

Supported actions are:
* Run a local (to the WatchServer) script
* Run a script using the [event api](http://repo.incapturesolutions.com/apidoc/#_event-api)
* Run a workflow

The absolute path (full directory and filename) is passed to the Rapture endpoint via a parameter to allow that program to complete its task.

The WatchServer expects that configuration in a well known place, namely: document://sys.config/watchserver/config. Configuration is covered below.

### System Diagram ###
![System Diagram](/Apps/WatchServer/images/watchservertopology.png)

# Installation and Running #

This example will illustrate the configuration and usage of WatchServer by detecting when a file (xlsx) is dropped into /test folder.

Any files created in /test will cause WatchServer to call the following workflow: workflow://workflows/incapture/watchserver/wsload

This workflow will do the following:

1. Load the file, in /test, specified by the parameter passed to it from WatchServer
2. Store the file to a blob://archive/yyyyMMdd_HHmmss/filename repository. Step code is [here](https://github.com/RapturePlatform/Rapture/blob/master/Apps/WatchServerPlugin/src/main/java/rapture/dp/invocable/workflow/LoadFile.java)
3. Using blob created in step 2 process the file and write each xls row to a document to a new repo document://data/yyyyMMdd_HHmmss/ROW000001..N Step code is [here](https://github.com/RapturePlatform/Rapture/blob/master/Apps/WatchServerPlugin/src/main/java/rapture/dp/invocable/workflow/ProcessFile.java)

## Using Docker ##

The entire setup can be run using all docker containers.  This is the easiest way to run the stack without doing builds or compiling.  

All images are available on Incapture's public [dockerhub](https://hub.docker.com/u/incapture/dashboard/)

Here are the relevant commands.

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
1. docker volume create --name fileDropTest
```
Download test file from ![here](/Apps/WatchServerPlugin/docker/SamplePriceData.xlsx). You will ftp this later in the process.

**Start a FTP Server**
Make sure you don't have a local ftp server already running.

The FTP server starts an instance of [PyFTPdlib](https://pythonhosted.org/pyftpdlib/tutorial.html#id2)
```
docker run -d -p 21:21 -p 30000-30009:30000-30009 -v fileDropTest:/ftp_root/ --name ftpserver incapture/pyftpserver
```
**Start Rapture API Server**
```
docker run -d -p 8080:8080 -p 8665:8665 --link mongo --link rabbit -v fileDropTest:/tmp --name curtis incapture/apiserver:xESlatest
```
**Start Rapture UI**
```
docker run -d -p 8000:8000 --link curtis --name rim incapture/rim
```
**Checkpoint**

To ensure environment is up login to your local environment on [http://localhost:8000/browser](http://localhost:8000/browser)

Credentials are rapture/rapture.

**Get the IP of the FTP Server**
```
docker inspect --format '{{ .NetworkSettings.IPAddress }}' ftpserver
```
**Start WatchServer Sample Configuration Plugin**
```
docker run --link curtis incapture/watchserverplugin
```
**Update the WatchServer Configuration**

1. Edit the config document: [http://localhost:8000/browser/_~doc~_sys.config/watchserver/config&state=editing](http://localhost:8000/browser/_~doc~_sys.config/watchserver/config&state=editing)
2. In the config json file update the FTP IP: "folder" : "ftp://\<update with FTP Server IP\>"
3. Hit the SAVE button

**Start WatchServer**
```
docker run -d --link mongo --link rabbit --name watchserver incapture/watchserver
docker logs -f watchserver
```

**Checkpoint**

To ensure WatchServer has started successfully the log should state: _"INFO [main] (WatchServer.java:141) <> [] - WatchServer started and ready to process events."_

**Kick off a workflow**

FTP'ing the sample xlsx file to /opt/test will cause a CREATE_EVENT to be fired. This will be picked up by WatchServer and cause a workflow to run. The workflow contains (java) code to load and extract data from the sample xlsx file.
```
1. ftp 0.0.0.0
2. Supply username and password (user/password)
3. cd test
4. put SmallSamplePriceData.xlsx
```
The workflow will start and can be viewed on the UI here [http://localhost:8000/process/workflows/incapture/watchserver/wsload](http://localhost:8000/process/workflows/incapture/watchserver/wsload)

# Configuration #

Configuration is captured in a Rapture document here document://sys.config/watchserver/config.

This allows you to specify:
* Where the WatchServer will be monitoring: local or an ftp server
* Directory/Folder to monitor
* What action to take for a create, modify or delete event in that specified directory
* For FTP the connection details

A sample (json) document is given as an example and is installed to your local environment:
```
{
	"sources": [{
		"type": "local",
		"config": {
			"folder": "/test/subfolder",
			"events": [{
					"event": "ENTRY_CREATE",
					"action": "script://scripts/incapture/watchserver/createaction"
			}, {
					"event": "ENTRY_MODIFY",
					"action": "script://scripts/incapture/watchserver/modifyaction"
			}]
		}
	}, {
		"type": "ftp",
		"config": {
			"folder": "ftp://localhost",
			"connection": {
					"username": "user",
					"password": "password",
					"port": "21",
					"pathtomonitor": "/test",
					"ftppassivemode": "true"
			},
			"events": [{
					"event": "ENTRY_CREATE",
					"action": "workflow://workflows/incapture/watchserver/wsload"
			}, {
					"event": "ENTRY_MODIFY",
					"action": "script://scripts/incapture/watchserver/modifyaction"
			}]
		}
	}]
}
```

Expected Behavior:

1. A file dropped into /Users/<home directory>/subfolder will run script script://scripts/incapture/watchserver/createaction
2. if that file (in step 1) changes this script will be run script://scripts/incapture/watchserver/modifyaction
3. A file dropped into ftp://localhost/tmp will run the following workflow workflow://workflows/incapture/watchserver/wsload

You do not need to specify an event -> action mapping for each event type.
