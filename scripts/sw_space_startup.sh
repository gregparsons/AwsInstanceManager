#!/bin/bash

#java -Djava.security.policy=jars/policy -cp 'jars/h4.jar' system.SpaceImpl djava.dyndns.org multi
java -Djava.rmi.server.hostname=djava.dyndns.org -Djava.security.policy=scripts/jars/policy -cp 'scripts/jars/h4.jar' system.SpaceImpl


#counter=0
#while [ $counter -lt 1000 ]; do
#	echo $counter
#	let counter=counter+1

	#sleep seconds
#	sleep 1
#done