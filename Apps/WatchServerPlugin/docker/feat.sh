#!/bin/bash
if [ -z "${OVERLAY}" ] ; then
	/opt/rapture/WatchServerPlugin/bin/WatchServerPlugin -host http://$HOST:8665/rapture -password $PASSWORD "$@"
else
	/opt/rapture/WatchServerPlugin/bin/WatchServerPlugin -host http://$HOST:8665/rapture -password $PASSWORD -overlay $OVERLAY "$@"
fi
