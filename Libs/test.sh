#!/bin/bash

{
  env && ./gradlew test
} || {
  cat RaptureCore/build/reports/tests/index.html &&\
  cat RaptureCore/build/reports/tests/classes/rapture.dp.NestedSplitStepTest.html &&\
  cat RaptureCore/build/reports/tests/classes/rapture.dp.SimpleForkStepTest.html &&\
  cat RaptureCore/build/reports/tests/classes/rapture.dp.MemoryIndexHandlerTest.html
}
