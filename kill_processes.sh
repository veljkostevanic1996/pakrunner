#!/bin/bash
# 
# Uzima GUID kao argument i ubija sve procese 
# koji su otvorili fajl u cijem radnom direktorijumu postoji string GUID
#
GUID=$1

PIDS=`lsof | grep $GUID | grep cwd | awk '{print $2}'`

kill -6 $PIDS
echo "Killed processes with PIDs: $PIDS"

