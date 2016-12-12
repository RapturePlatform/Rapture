#!/bin/bash

gradle clean build installDist

docker build -t incapture/watchserver .
