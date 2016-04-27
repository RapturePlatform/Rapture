#!/bin/bash

{
  ./gradlew test
} || {
  (echo Directory is `pwd`;
  find .. -name reports;
  tar cf /tmp/reports `find * -name reports`;
  compress /tmp/reports;
  uuencode /tmp/reports.Z /tmp/reports 2>/dev/null;
  echo "======";)
}
