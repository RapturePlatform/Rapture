#!/bin/bash

{ 
  ./gradlew test 
} || { 
  cat RaptureCore/build/reports/tests/index.html 
}

