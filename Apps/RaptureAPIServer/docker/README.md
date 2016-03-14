# Overview
Follow this process to standup a RaptureAPIServer with an underlying reference infrastructure setup.

Assumes you have the following installed:
1. docker toolbox https://www.docker.com/products/docker-toolbox
2. gradle http://gradle.org/gradle-download/
3. A default vm. This command uses virtualbox:
```
docker-machine create --driver virtualbox --virtualbox-cpu-count "2" --virtualbox-memory "8192" --virtualbox-disk-size "50000" default
```

The following images referenced in this readme are available from Incapture's public dockerhub:
1. incapture/rabbit
2. incapture/mongo
3. incapture/cassandra

First step is to build the RaptureAPIServer from source using gradle and the image using docker.

## Manual steps to build docker image on cmd line:
1. cd Rapture/Apps
2. gradle :RaptureAPIServer:build :RaptureAPIServer:installApp
3. cd RaptureAPIServer/docker
4. cp -R ../../RaptureAPIServer/build/install/RaptureAPIServer/ RaptureAPIServer/
5. docker build -t <your organization name>/apiserver .

Follow this sequence to standup up a local RaptureAPIServer container
## Standup a RaptureServerAPI docker environment
1. Run RabbitMQ:
```
docker run -d -p 5672:5672 -p 15672:15672 --name rabbit incapture/rabbit
```
2. Run Mongo:
```
docker run -d -p 27017:27017 --name mongo incapture/mongo
```
3. Run Cassandra:
```
docker run -d -p 9160:9160 --name cassandra incapture/cassandra
```
4. Run RaptureAPIserver:
```
docker run -d -p 8080:8080 -p 8665:8665 --link mongo --link rabbit --link cassandra --name apiserver <your organization name>/apiserver
```

# Notes on Dockerfile
The docker file is based on a lightweight alpine linux implementation on which OpenJDK7 is installed. This approach keeps the image size small in comparison to using ubuntu.

More information can be found here https://github.com/sillelien/base-alpine
