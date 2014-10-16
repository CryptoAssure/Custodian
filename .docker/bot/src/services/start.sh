#!/bin/sh

echo "Starting Nu bot"
nohup java -jar /data/src/NuBot.jar /data/src/options.json > /data/services/operations.log 2>&1 & echo $! > /data/services/bot-$!.pid

sleep 1.5

echo "Nu bot is running [PID: $!]"