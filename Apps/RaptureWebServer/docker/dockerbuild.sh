#!/bin/bash
DIRECTORY="CurtisWebServer/"
if [ $# -eq 0 ]
  then
    echo "Please supply an image tag (ex: latest, 1.0.3 etc...)"
    exit
fi
echo "Using supplied image tag: $1"
if [ -d "$DIRECTORY" ]; then
  rm -rf "$DIRECTORY"
fi
cp -R ../../RaptureAPIServer/docker/rootfs/ rootfs
cp -R ../../RaptureAPIServer/docker/config/*.cfg ./RaptureWebServer/etc/rapture/config/
cp -R ../build/install/RaptureWebServer/ RaptureWebServer/
docker build -t incapture/webserver:"$1" .
PUSHED=false
OPT="push"
if [ $# -eq 2 ]; then
  if [ "$2" == "$OPT" ]; then
      docker push incapture/webserver:"$1"
      PUSHED=true
  fi
fi
if [ "$PUSHED" == true ]; then
  echo "Successfully built & pushed docker image: incapture/webserver:$1"
else
  echo "Successfully built incapture/webserver:$1 *** did not push ***"
fi
