# This script executes the test and copies reports to the provided output directory
# To call this script from the service working directory
# ./dist/testing/integration/build-aws/run-tests.sh "./reports/"


SCRIPT_SOURCE_DIR=$(dirname "$0")
echo "Script source location"
echo "$SCRIPT_SOURCE_DIR"
(cd "$SCRIPT_SOURCE_DIR"/../bin && ./install-deps.sh)

#### ADD REQUIRED ENVIRONMENT VARIABLES HERE ###############################################
# The following variables are automatically populated from the environment during integration testing
# see os-deploy-aws/build-aws/integration-test-env-variables.py for an updated list

***REMOVED***_COGNITO_CLIENT_ID
# ELASTIC_HOST
# ELASTIC_PORT
# FILE_URL
# LEGAL_URL
# SEARCH_URL
# STORAGE_URL
export AWS_COGNITO_AUTH_FLOW=USER_PASSWORD_AUTH
export AWS_COGNITO_AUTH_PARAMS_PASSWORD=$ADMIN_PASSWORD
export AWS_COGNITO_AUTH_PARAMS_USER=$ADMIN_USER
export DEFAULT_DATA_PARTITION_ID_TENANT1=opendes
export DEFAULT_DATA_PARTITION_ID_TENANT2=common
export ENTITLEMENTS_DOMAIN=testing.com
export OTHER_RELEVANT_DATA_COUNTRIES=US
export STORAGE_HOST=$STORAGE_URL
export HOST=$SCHEMA_URL
export ELASTIC_HOST=$ELASTIC_HOST
export ELASTIC_PORT=$ELASTIC_PORT
export ELASTIC_PASSWORD=$ELASTIC_PASSWORD
export ELASTIC_USER_NAME=$ELASTIC_USERNAME

#### RUN INTEGRATION TEST #########################################################################

mvn -ntp test -f "$SCRIPT_SOURCE_DIR"/../pom.xml -Dcucumber.options="--plugin junit:target/junit-report.xml"
# mvn -Dmaven.surefire.debug test -f "$SCRIPT_SOURCE_DIR"/../pom.xml -Dcucumber.options="--plugin junit:target/junit-report.xml"
TEST_EXIT_CODE=$?

#### COPY TEST REPORTS #########################################################################

if [ -n "$1" ]
  then
    mkdir -p "$1"
    cp "$SCRIPT_SOURCE_DIR"/../target/junit-report.xml "$1"/os-indexer-junit-report.xml
    cp  -R "$SCRIPT_SOURCE_DIR"/../target/surefire-reports "$1"
fi

exit $TEST_EXIT_CODE
