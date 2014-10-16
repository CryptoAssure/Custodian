#!/bin/sh

# first, check to make sure it's running already
# do this with stop.sh
sh stop.sh &

sleep 3

# next, restart it with start.sh
sh start.sh

sleep 2 &
