#!/bin/bash

#/System/Library/Frameworks/JavaVM.framework/Versions/A/Commands/java
#java -Djava.security.policy=jars/policy -cp 'jars/h4.jar' applications.euclideantsp.JobEuclideanTsp djava.dyndns.org multi
#java -Djava.security.policy=jars/policy -cp 'jars/h4.jar' applications.euclideantsp.JobEuclideanTsp localhost


java -Djava.security.policy=scripts/jars/policy -cp 'scripts/jars/h4.jar' applications.euclideantsp.JobEuclideanTsp localhost

# temp junk to keep the process running for a while. TESTING only.
#counter=0
#while [ $counter -lt 1000 ]; do
#	echo $counter
#	let counter=counter+1

	#sleep seconds
#	sleep 1
#done