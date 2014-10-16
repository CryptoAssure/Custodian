#!/bin/sh

nohup java -jar NuBot.jar options.json > operations.log 2>&1 & echo $1 > bot.pid