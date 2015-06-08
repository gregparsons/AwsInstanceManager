#!/bin/bash

#/System/Library/Frameworks/JavaVM.framework/Versions/A/Commands/java
#java -Djava.security.policy=scripts/jars/policy -Djava.rmi.server.hostname=djava.dyndns.org -cp 'scripts/jars/h4.jar' system.ComputerImpl djava.dyndns.org multi
java -Djava.security.policy=scripts/jars/policy -cp 'scripts/jars/h4.jar' system.ComputerImpl djava.dyndns.org multi




