#!/bin/bash

{
  ./gradlew test
} || {
  (echo Directory is `pwd`;
  find .. -name reports;
  zip -r - `find .. -name reports` | uuencode - 2>/dev/null;
  echo "======";)
}
