#!/bin/bash

java -server ${JAVA_COMMAND_STR} -jar /opt/bigstock/${JAR_NAME} &
PID=$!

while sleep 60; do
  if ! kill -0 $PID 2>/dev/null; then
    echo "Java process exited"
    exit 1
  fi
done
