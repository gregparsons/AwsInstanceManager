#!/bin/sh 

### BEGIN INIT INFO
# Provides:          startHwComputer
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Example initscript
# Description:       This file should be used to construct scripts to be
#                    placed in /etc/init.d.
### END INIT INFO


cd /home/ubuntu/290b/AwsInstanceManager; 
mvn test -Pcomputer
