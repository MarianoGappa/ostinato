#!/usr/bin/env bash

# Echoes the build.sbt project scala major version to STDOUT.
# e.g. Major version of 2.11.7 is 2.11
#
# NOTES:
# - place this script on the root of your project
# - this script should then find a build.sbt inside your project
# - this script assumes your project version setting is a literal e.g.: `version := "0.1-SNAPSHOT"`

# Exit on unset variables or if any of the following commands returns non-zero
set -eu

# cd to path of current script
# (based on http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in)
SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd $SCRIPT_DIR

# Find the first build.sbt in the path tree of this script,
# find the version line and extract the content within double quotes
find '.' -name "build.sbt" |
    head -n1 |
    xargs grep '[ \t]*scalaVersion :=' |
    head -n1 |
    sed 's/.*"\(.*\)".*/\1/' |
    sed 's/^\([0-9]*\.[0-9]*\)\.[0-9]*$/\1/'
