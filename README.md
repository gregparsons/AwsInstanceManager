# Java Distributed Computing Deployment to Amazon Web Services Elastic Cloud

## Purpose
This system streamlines the process of deploying a Java-centric compute cluster to Amazon's cloud computing system, Elastic Cloud Compute (EC2). The intended application to run on the system was developed in [this UCSB class](https://www.cs.ucsb.edu/~cappello/290B/) under instruction of Dr. Pete Cappello.

## Achitecture Overview

The system developed in Dr. Cappello's class consists of a compute "space" and a number of "computer" instances to which the space can delegate subtasks. A client application connects to the space and submits a compute task. All of this is developed with Java and communicates via Java Remote Method Invocation (RMI). For the application, the space maintains the RMI registry.

This system is distinct from that system in both purpose and design, but its architecture is similar. For the purposes of this system, "hardware" generally relates to our newly developed system and to it's three parts: hardware user, hardware manager, and hardware computers. "Application" client, space and computers refers to the distributed compute system developed in class. 

Any distributed application could be run on this system. A single jar and corresponding scripts to start it are in the scripts and scripts/jars directories. Replacing those with the user's choice of applications would allow our system to be used for any compute purpose.

An AWS account is required with credentials in file on both the user machine and on the hardware manager (~/.aws/credentials).

We tested the user interface on both Apple and Windows laptops. It is the only part not run on EC2 instances, but it easily could. Through the Amazon API a "hardware manager" is started to act as the communications bridge to any number of EC2 instances acting as distributed computers. Selected through the client user interface, RMI calls from the client to the hardware manager cause additional hardware instances to start, and application space and application computers to be run.

DDClient runs on the hardware manager to create a static domain name for connecting to the system. We are using djava.dyndns.org, but this should be changed in the Utils class as appropriate. Also, the Amazon machine images (AMIs) we use for the hardware manager have this domain name and our application burned into them. To implement another application these images would need to be modified.

We currently test computation of a Traveling Salesman Problem using 12 cities.

## Setup/Use

1. Create EC2 per the Getting Started section (here)[https://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html].

2. Start the local client using the following command > mvn test -Puser

3. Use the command prompts to start a hardware manager (43) and to connect to it (41).

4. To run your own application, replace the files in scripts and scripts/jars with your own. The sw_*.sh files are run via system command on the hardware manager (space) and on hardware computers (software computers).

5. Using the application menu, start a space and a number of software computers to use for the test. 

6. Start the application. The client application will run on the local computer and must have its own RMI capability for contacting the space (running at djava.dyndns.org in our configuration).