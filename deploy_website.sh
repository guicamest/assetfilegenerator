#!/bin/bash
#
# Deploys the current Asset File Generator website to the gh-pages branch of the GitHub
# repository. To test the site locally before deploying run `jekyll --server`
# in the website/ directory.

set -ex

REPO="git@github.com:guicamest/assetfilegenerator.git"
USER_ID="guicamest"
GROUP_ID="io.github.guicamest"
ARTIFACT_ID="assetfilegenerator"

LATEST_VERSION=`curl "https://api.bintray.com/packages/guicamest/maven/assetfilegenerator" | jq '.latest_version'`
LATEST_VERSION="${LATEST_VERSION//\"/}"

GROUP_PATH="${GROUP_ID//\./\/}"

sed -e "s/%VERSION%/'$LATEST_VERSION'/g" website/tpl/jquery-maven-artifact.min.js > website/static/jquery-maven-artifact.min.js

curl -f -L -I "https://dl.bintray.com/$USER_ID/maven/$GROUP_PATH/$ARTIFACT_ID/$LATEST_VERSION/$ARTIFACT_ID-$LATEST_VERSION-javadoc.jar"

exit

DIR=temp-assetfilegenerator-clone

# Delete any existing temporary website clone
rm -rf $DIR

# Clone the current repo into temp folder
git clone $REPO $DIR

# Move working directory into temp folder
cd $DIR

# Checkout and track the gh-pages branch
git checkout -t origin/gh-pages

# Delete everything
rm -rf *

# Copy website files from real repo
cp -R ../website/* .

# Download the latest javadoc
curl -L "https://dl.bintray.com/$USER_ID/maven/$GROUP_PATH/$ARTIFACT_ID/$LATEST_VERSION/$ARTIFACT_ID-$LATEST_VERSION-javadoc.jar" > javadoc.zip
mkdir javadoc
unzip javadoc.zip -d javadoc
rm javadoc.zip

# Stage all files in git and create a commit
git add .
git add -u
git commit -m "Website at $(date)"

# Push the new files up to GitHub
git push origin gh-pages

# Delete our temp folder
cd ..
rm -rf $DIR
