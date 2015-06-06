#!/bin/bash

#java -Djava.security.policy=jars/policy -cp 'jars/h4.jar' system.SpaceImpl djava.dyndns.org multi
java -Djava.security.policy=jars/policy -cp 'jars/h4.jar' system.SpaceImpl localhost multi


counter=0
while [ $counter -lt 1000 ]; do
	echo $counter
	let counter=counter+1

	#sleep seconds
	sleep 1
done