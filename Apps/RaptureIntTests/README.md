## Overview

The RaptureIntTests project contains integration API tests for both Java and Reflex. Reflex is a scripting language developed at Incapture Technologies for use with Rapture developed systems.

The integration tests are run against a live Rapture environment consisting of a RaptureAPIServer and underlying datastores and messaging components.

This set of tests uses the following reference Rapture implementation:
 1. RaptureAPIServer
 2. MongoDB
 3. Cassandra
 4. RabbitMQ
 5. ElasticSearch

Please note Rapture supports other:
 1. Datastores and messaging infrastructure
 2. Languages for API access

The objectives of this README are to:
 1. Setup a local RaptureAPIServer environment
 2. Setup your environment to run tests
 3. Run tests either using Gradle command line or Eclipse IDE
 4. Add your own tests!

## Setup

Make sure you have:
 1. Java 1.8 installed
 2. Successfully built both projects in the RapturePlatform project: /Libs and /Apps

### Java
* Java 7 or 8 JRE is installed
* Gradle (latest version)

### Reflex
* If Java works then Reflex will to!

## Start your Rapture Environment

We will cover two methods to standup a local RaptureAPIServer environment:
 1. Docker (much quicker!)
 2. From mac command line  

### Docker
Please see here for Docker setup and running instructions:
* /Rapture/Apps/RaptureAPIServer/docker/README.md

### Local

Follow this install and startup process for the mac.

1. Install [homebrew](http://brew.sh/)
2. Run the following commands:
   * brew install gradle
   * brew install homebrew/versions/mongodb24
   * brew install homebrew/versions/cassandra12
   * brew install rabbitmq
3. Startup process:
   * Start mongo from command line, replace ${dbpath} to a real db path:
     ```
     $ mongod --bind_ip 127.0.0.1 --dbpath ${dbpath}
     ```
   * Add user rapture to rapture mongo db from command line:
     ```
     $ mongo

     > use RaptureMongoDB;
     > db.addUser( { user: "rapture",
              pwd: "rapture",
              roles: [ "readWrite", "dbAdmin" ]
       } );
     ```  
   * Run RabbitMQ
     ```
     $ rabbitmq-server  
     ```
     Alternatively, if the command is not recognized, try this instead:
     ```
     /usr/local/sbin/rabbitmq-server
     ```
   * Run RaptureAPIServer from command line:
     ```
     $ cd RaptureAPIServer/build/install/RaptureAPIServer/bin/
     $ ./RaptureAPIServer
     ```
   * Or run RaptureAPIServer from Eclipse
     * Right click on project RaptureAPIServer
     * Select Run As -> Java Application
     * Type RaptureAPIServer in the Select Java Application pop out window    

At this point the API server should be up and available to respond to requests.

Lets run some tests!
## Run the Tests

Tests are run on the command line using gradle or you can use your favorite IDE to also run tests.

### Gradle - Java

Tests are implemented using the Testng framework with associated gradle tasks in /RaptureIntTests/build.gradle to run them.

Lets get started by running a small set of smoke tests to check out your environment:
1. From a command line:
   ```
   $ cd /Rapture/Apps/RaptureIntTests
   ```
2. Run smoke tests against a local Rapture Environment:
   ```
   $ gradle smokeTests
   ```
   Or against a docker Rapture Environment:
   ```
   $ gradle smokeTests -Penv=docker
   ```
3. View test results in /Rapture/Apps/RaptureIntTests/build/reports/tests

### Gradle - Reflex

To be added.

## Add Tests
Lets add a new java test using the testng test class skeleton.

Test objective:
   1. create a versioned document repository
   2. write a document (json) to it
   3. update the document with modified json i.e. write to the same document as step 2.
   4. Assert the version number has been incremented
   5. Get the difference between document version using [jsonpatch](http://jsonpatch.com/)
   6. Assert on the actual and expected difference between documents (json)

Learning objectives:
   1. How a Rapture system uses URIs to address data
   2. Usage of Rapture's native _Document_ datatype and associated api
   3. Running tests against a Rapture system using Testng

Notes:
   1. Testng is added as a testCompile dependency in RaptureIntTests/build.gradle
   2. The skeleton test class contains _deep dive_ commentary on each line of code

### Document API tutorial
A skeleton Testng test class has been added to project along with a suite.xml and build.grade task:
* /Rapture/Apps/RaptureIntTests/src/test/java/rapture/api/document/_TutorialTests.java_
* /Rapture/Apps/RaptureIntTests/src/test/resources/\<env\>\_testng.xml
* /Rapture/Apps/RaptureIntTests/build.gradle: _docTests()_

Open the TutorialTests.java in your favorite IDE and lets proceed.

Below sets out the high-level tasks and Rapture API calls involved. The test class has extended commentary on each line of code.

Task | Rapture API | Comment
:---  | :--- | :---
Define the Document repository with versioning | None | The configuration is String based
Define the URI (or address) of the document repository | None | The URI is string based
Call createDocRepo(String docRepoUri, String config) method to create the repository | 1. createDocRepo(String docRepoUri, String config) |
Check the repo exists and Assert on the Boolean response | 1. docRepoExists(String docRepoUri) |
Load a json file (from /resources) from disk and create a Rapture document | 1. putDoc(String docUri, String content) 2. getDocAndMeta(String docUri) |
Load a slightly different json and write to same document URI and check the version was updated | 1. putDoc(String docUri, String content) 2. getDocAndMeta(String docUri) |
use JsonPatch to get the difference between the two documents | 1. getDoc(String docUri) |
