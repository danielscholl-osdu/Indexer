#!/bin/bash

# Exit as soon as a command fails
set -e

if [ ! $# = 1 ]; then
    echo "Usage: $0 dir"
    exit 1
fi

STAGE_DIR=$1

cd $BUILD_REPOSITORY_LOCALPATH/provider/indexer-gcp/scripts

source ./config.sh

#apply gomplate to deploy2gcp.sh.tmpl
gomplate -f ./deploy2gcp.sh.tmpl -o ./deploy2gcp.sh
chmod a+x ./*.sh
echo "Contents of deploy2gcp.sh:"
cat ./deploy2gcp.sh

cd ..

# Upload all build and deploy scripts as artifacts
cp -R ./scripts $STAGE_DIR
# Upload the build generated .jar file as an artifact
cp ./target/indexer-gcp-*-spring-boot.jar $STAGE_DIR

echo "Staging deployment artifacts to folder: $STAGE_DIR"
cp ./src/main/appengine/app.yaml $STAGE_DIR

# set current directory back to build repo root
cd $BUILD_REPOSITORY_LOCALPATH

# Zip integration tests and upload it as artifact
pushd testing
zip -r testing.zip *
cp testing.zip $STAGE_DIR
popd

