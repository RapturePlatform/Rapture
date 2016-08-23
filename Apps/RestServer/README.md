# RestServer

### Overview ###
This is a Rapture application that exposes various document, blob, and series Rapture API calls using a REST interface.  It listens on port 4567.

### System Diagram ###
![System Diagram](/Apps/RestServer/images/restserver.png)

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
**Start ElasticSearch**
```
docker run -d -p 9300:9300 -p 9200:9200 --name elasticsearch incapture/elasticsearch
```
**Start Postgres**
```
docker run -d -p 5432:5432 --name postgres incapture/postgres
```
**Start RestServer**
```
docker run -d -p 4567:4567 --link mongo --link rabbit --link elasticsearch --link postgres --name restserver incapture/restserver
```
