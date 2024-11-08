#!/bin/bash

# 啟動 Xvfb
Xvfb :99 -screen 0 1536x816x24 &

# 設置 DISPLAY 變量
export DISPLAY=:99

#${JAR_NAME} ${JAVA_COMMAND_STR} 為deployment傳進來的環境變數
java -server ${JAVA_COMMAND_STR} -jar  /opt/bigstock/${JAR_NAME}
while sleep 60; do
  ps aux |grep ${JAR_NAME} |grep -q -v grep
  PROCESS_1_STATUS=$?

  # If the greps above find anything, they exit with 0 status
  # If they are not both 0, then something is wrong
  if [ $PROCESS_1_STATUS -ne 0 ]; then
    echo "One of the processes has already exited."
    exit 1
  fi
done