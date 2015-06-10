#!/bin/bash

#java -Djava.security.policy=jars/policy -cp 'jars/h4.jar' system.SpaceImpl djava.dyndns.org multi
java -Djava.rmi.server.hostname=djava.dyndns.org -Djava.security.policy=scripts/jars/policy -cp 'scripts/jars/h4.jar' system.SpaceImpl


