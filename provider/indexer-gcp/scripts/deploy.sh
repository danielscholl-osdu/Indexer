#!/bin/bash

# deploy script

# Exit as soon as a command fails
set -e

SCRIPTS_DIR=$(dirname $0)
DROP_DIR=$(dirname $SCRIPTS_DIR)

# Go to drop directory
cd $DROP_DIR
# convert to full path
DROP_DIR=`pwd`
echo "Current working directory: $DROP_DIR"

DEPLOY_DIR=$DROP_DIR/deploy

if [ -s $DEPLOY_DIR ]; then
  rm -rf $DEPLOY_DIR/*
else
  mkdir $DEPLOY_DIR
fi

DEPLOY_SCRIPTS_DIR=$DEPLOY_DIR/scripts

mkdir -p $DEPLOY_SCRIPTS_DIR

echo "Copy artifacts to folder: $DEPLOY_DIR"
cp $DROP_DIR/app.yaml $DEPLOY_DIR
cp $DROP_DIR/indexer-gcp-*.jar  $DEPLOY_DIR
cp $DROP_DIR/testing.zip  $DEPLOY_DIR
cp $DROP_DIR/scripts/* $DEPLOY_SCRIPTS_DIR
chmod a+x $DEPLOY_SCRIPTS_DIR/*.sh

# Go to deploy directory
cd $DEPLOY_DIR
echo "Current working directory: $DEPLOY_DIR"

source $DEPLOY_SCRIPTS_DIR/config.sh

echo "Deploying to gcp"
$DEPLOY_SCRIPTS_DIR/deploy2gcp.sh
echo "Deployed to gcp"