#!/bin/sh
##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

DIR="$( cd "$( dirname "$0" )" && pwd )"
APP_HOME="$DIR"
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Add default JVM options here if desired
DEFAULT_JVM_OPTS=""

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Find Java
if [ -n "$JAVA_HOME" ] ; then
    JAVA_EXE="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVA_EXE" ] ; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVA_EXE="java"
    which java >/dev/null 2>&1 || { echo "ERROR: Java not found" >&2; exit 1; }
fi

exec "$JAVA_EXE" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

