#!/bin/sh

PROCESS=`cat /data/services/bot*.pid`

if
  ps -p ${PROCESS} > /dev/null 2>&1
  echo "Attempting to stop Nu bot running on PID: ${PROCESS}."
then 
  kill -9 ${PROCESS}
  rm /data/services/bot-*.pid
fi

sleep 1.25

echo "Nu bot has been stopped."

