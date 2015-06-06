#!/bin/bash

#/System/Library/Frameworks/JavaVM.framework/Versions/A/Commands/java
java -Djava.security.policy=scripts/jars/policy -Djava.rmi.server.hostname=djava.dyndns.org -cp 'scripts/jars/h4.jar' system.ComputerImpl djava.dyndns.org multi



# do nothing slowly, to see if the process stays running
counter=0
while [ $counter -lt 1000 ]; do
	echo $counter
	let counter=counter+1

	#sleep seconds
	sleep 1
done


