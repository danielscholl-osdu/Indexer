# Indexer Service
indexer-aws is a [Spring Boot](https://spring.io/projects/spring-boot) service that provides a set of APIs to index storage records against Elasticsearch. It's not user-facing, all APIs are used internally by the platform.

## Running Locally

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites
Pre-requisites

* JDK 8 (https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html)
* Maven 3.8.3 or later
* Lombok 1.16 or later
* OSDU Instance deployed on AWS

### Service Configuration
In order to run the service locally or remotely, you will need to have the following environment variables defined.

| name | example value | required | description | sensitive? |
| ---  | ---   | ---         | ---        | ---    |
| `LOCAL_MODE` | `true` | yes | Set to 'true' to use env vars in place of the k8s variable resolver | no |
| `APPLICATION_PORT` | `8080` | yes | The port the service will be hosted on. | no |
| `AWS_REGION` | `us-east-1` | yes | The region where resources needed by the service are deployed | no |
| `AWS_ACCESS_KEY_ID` | `ASIAXXXXXXXXXXXXXX` | yes | The AWS Access Key for a user with access to Backend Resources required by the service | yes |
| `AWS_SECRET_ACCESS_KEY` | `super-secret-key==` | yes | The AWS Secret Key for a user with access to Backend Resources required by the service | yes |
| `AWS_SESSION_TOKEN` | `session-token-xxxxxxxxxx` | no | AWS Session token needed if using an SSO user session to authenticate | yes |
| `ENVIRONMENT` | `osdu-prefix` | yes | The Resource Prefix defined during deployment | no |
| `LOG_LEVEL` | `DEBUG` | yes | The Log Level severity to use (https://www.tutorialspoint.com/log4j/log4j_logging_levels.htm) | no |
| `SSM_ENABLED` | `true` | yes | Set to 'true' to use SSM to resolve config properties, otherwise use env vars | no |
| `SSL_ENABLED` | `false` | no | Set to 'false' to disable SSL for local development | no |
| `ENTITLEMENTS_BASE_URL` | `http://localhost:8081` or `https://some-hosted-url` | yes | Specify the base url for an entitlements service instance. Can be run locally or remote | no |
| `PARTITION_BASE_URL` | `http://localhost:8082` or `https://some-hosted-url` | yes | Specify the base url for a partition service instance. Can be run locally or remote | no | 
| `STORAGE_BASE_URL` | `http://localhost:8082` or `https://some-hosted-url` | yes | Specify the base url for a partition service instance. Can be run locally or remote | no | 
| `SCHEMA_BASE_URL` | `http://localhost:8082` or `https://some-hosted-url` | yes | Specify the base url for a partition service instance. Can be run locally or remote | no |
| `STORAGE_RECORDS_BATCH_SIZE` | 20 | Batch size for storage API `POST {endpoint}/query/records:batch` | no | - |
| `STORAGE_RECORDS_BY_KIND_BATCH_SIZE` | - | Batch size for storage API `GET {endpoint}/query/records`. If this is not present, defaults to value of `STORAGE_RECORDS_BATCH_SIZE` | no | - | 


### Run Locally
Check that maven is installed:

example:
```bash
$ mvn --version
Apache Maven 3.8.3 (ff8e977a158738155dc465c6a97ffaf31982d739)
Maven home: /usr/local/Cellar/maven/3.8.3/libexec
Java version: 1.8.0_312, vendor: Amazon.com Inc., runtime: /Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home/jre
...
```

You may need to configure access to the remote maven repository that holds the OSDU dependencies. Copy one of the below files' content to your .m2 folder
* For development against the OSDU GitLab environment, leverage: `<REPO_ROOT>~/.mvn/community-maven.settings.xml`
* For development in an AWS Environment, leverage: `<REPO_ROOT>/provider/indexer-aws/maven/settings.xml`

* Navigate to the service's root folder and run:

```bash
mvn clean package -pl indexer-core,provider/indexer-aws
```

* If you wish to build the project without running tests

```bash
mvn clean package -pl indexer-core,provider/indexer-aws -DskipTests
```

After configuring your environment as specified above, you can follow these steps to run the application. These steps should be invoked from the *repository root.*
<br/>
<br/>
NOTE: If not on osx/linux: Replace `*` with version numbers as defined in the provider/indexer-aws/pom.xml file

```bash
java -jar provider/indexer-aws/target/indexer-aws-*.*.*-SNAPSHOT-spring-boot.jar
```

## Running Elasticsearch locally
For indexer to index anything, it needs to have access to an Elasticsearch cluster. The easiest way to do this is to spin one up locally.
You can spin one up locally using Docker or Kubernetes Helm. What's detailed below is simply downloading executable and running directly.

Instructions copied from here for longevity: https://www.elastic.co/guide/en/elasticsearch/reference/6.8/getting-started-install.html

1. Download a distribution from here: https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.8.20.tar.gz

2. Extract the archive
Linux and macOS: `tar -xvf elasticsearch-6.8.20.tar.gz`
Windows PowerShell: `Expand-Archive elasticsearch-6.8.20-windows-x86_64.zip`

3. Start Elasticsearch from the bin directory:
Linux and macOS:
   ```
   cd elasticsearch-6.8.20/bin
   ./elasticsearch
   ```
   
   Windows:
   
   ```
   cd %PROGRAMFILES%\Elastic\Elasticsearch\bin
   .\elasticsearch.exe
    ```
   
You should see in the logs that pop up what url and port it runs on. By default you should see http://localhost with port 9300

## Testing
 
 ### Running Integration Tests 
 This section describes how to run OSDU Integration tests (testing/indexer-test-aws).
 
 IMPORTANT: You need to setup indexer queue locally first using README in indexer-queue-aws.
 
 You will need to have the following environment variables defined.
 export AWS_COGNITO_AUTH_FLOW=USER_PASSWORD_AUTH
 export AWS_COGNITO_AUTH_PARAMS_PASSWORD=$ADMIN_PASSWORD
 export AWS_COGNITO_AUTH_PARAMS_USER=$ADMIN_USER
 export DEFAULT_DATA_PARTITION_ID_TENANT1=opendes
 export DEFAULT_DATA_PARTITION_ID_TENANT2=common
 export ENTITLEMENTS_DOMAIN=example.com
 export OTHER_RELEVANT_DATA_COUNTRIES=US
 export STORAGE_HOST=$STORAGE_URL
 export HOST=$SCHEMA_URL
 export ELASTIC_HOST=$ELASTIC_HOST
 export ELASTIC_PORT=$ELASTIC_PORT
 export ELASTIC_PASSWORD=$ELASTIC_PASSWORD
 export ELASTIC_USER_NAME=$ELASTIC_USERNAME
 
 | name | example value | description                                                                            | sensitive?
 | ---  |----------------------------------------------------------------------------------------| ---         | ---        |
 | `AWS_ACCESS_KEY_ID` | `ASIAXXXXXXXXXXXXXX` | The AWS Access Key for a user with access to Backend Resources required by the service | yes |
 | `AWS_SECRET_ACCESS_KEY` | `super-secret-key==` | The AWS Secret Key for a user with access to Backend Resources required by the service | yes |
 | `AWS_SESSION_TOKEN` | `session-token-xxxxxxxxx` | AWS Session token needed if using an SSO user session to authenticate                  | yes |
 | `AWS_COGNITO_USER_POOL_ID` | `us-east-1_xxxxxxxx` | User Pool Id for the reference cognito                                                 | no |
 | `AWS_COGNITO_CLIENT_ID` | `xxxxxxxxxxxx` | Client ID for the Auth Flow integrated with the Cognito User Pool                      | no |
 | `AWS_COGNITO_AUTH_FLOW` | `USER_PASSWORD_AUTH` | Auth flow used by reference cognito deployment                                         | no |
 | `DEFAULT_DATA_PARTITION_ID_TENANT1` | `opendes` | Partition used to create and index record                                              | no |
 | `DEFAULT_DATA_PARTITION_ID_TENANT2` | `common` | Another needed partition                                                               | no |
 | `AWS_COGNITO_AUTH_PARAMS_USER` | `int-test-user@testing.com` | Int Test Username                                                                      | no |
 | `AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS` | `noaccess@testing.com` | No Access Username                                                                     | no |
 | `AWS_COGNITO_AUTH_PARAMS_PASSWORD` | `some-secure-password` | Int Test User/NoAccessUser Password                                                    | yes |
 | `ENTITLEMENTS_DOMAIN` | `example.com` | Domain for user's groups                                                               | no |
 | `OTHER_RELEVANT_DATA_COUNTRIES` | `US` | Used to create demo legal tag                                                          | no |
 | `STORAGE_HOST` | `http://localhost:8080/api/storage/v2/` | The url where the storage API is hosted                                                | no |
 | `HOST` | `http://localhost:8080` | Base url for deployment                                                                | no |
 | `ELASTIC_HOST` | `localhost` | Url for elasticsearch                                                                  | no |
 | `ELASTIC_PORT` | `9300` | Port for elasticsearch                                                                 | no |
 | `ELASTIC_PASSWORD` | `xxxxxxxxxxxxxxx` | Password for user to access elasticsearch                                              | yes |
 | `ELASTIC_USER_NAME` | `xxxxxxxxxxxxxxxx` | Username for user to access elasticsearch                                              | yes |
 | `CUCUMBER_OPTIONS` | `--tags '~@indexer-extended'` OR `--tags '~@* and @indexer-extended'` | By default `--tags '~@* and @indexer-extended'` to enable experimental feature testing | no |


 **Creating a new user to use for integration tests**
 ```
 aws cognito-idp admin-create-user --user-pool-id ${AWS_COGNITO_USER_POOL_ID} --username ${AWS_COGNITO_AUTH_PARAMS_USER} --user-attributes Name=email,Value=${AWS_COGNITO_AUTH_PARAMS_USER} Name=email_verified,Value=True --message-action SUPPRESS

 aws cognito-idp initiate-auth --auth-flow ${AWS_COGNITO_AUTH_FLOW} --client-id ${AWS_COGNITO_CLIENT_ID} --auth-parameters USERNAME=${AWS_COGNITO_AUTH_PARAMS_USER},PASSWORD=${AWS_COGNITO_AUTH_PARAMS_PASSWORD}
 ```
 
 **Entitlements group configuration for integration accounts**
 <br/>
 In order to add user entitlements, run entitlements bootstrap scripts in the entitlements project
 
 | AWS_COGNITO_AUTH_PARAMS_USER |
 | ---  | 
 | service.indexer.admin |
 | service.legal.admin |
 | service.storage.admin |
 
 Execute following command to build code and run all the integration tests:

### Run Tests simulating Pipeline

* Prior to running tests, scripts must be executed locally to generate pipeline env vars

```bash
testing/indexer-test-aws/build-aws/prepare-dist.sh

#Set Neccessary ENV Vars here as defined in run-tests.sh

dist/testing/integration/build-aws/run-tests.sh 
```

### Run Tests using mvn
Set required env vars and execute the following:
```
mvn clean package -f testing/pom.xml -pl indexer-test-core,indexer-test-aws -DskipTests
mvn test -f testing/indexer-test-aws/pom.xml
```



## License
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
