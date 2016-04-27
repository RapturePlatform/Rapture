#!/bin/bash

{
  ./gradlew test
} || {
  (echo Directory is `pwd`;
  find .. -name reports;
  tar cf /tmp/reports.tar `find * -name reports`;
  uuencode /tmp/reports.tar 
  echo "======";)
}
