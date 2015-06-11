# Java Distributed Computing Deployment to Amazon Web Services Elastic Cloud

## Purpose
This system streamlines the process of deploying a Java-centric compute cluster to Amazon's cloud computing system, Elastic Cloud Compute (EC2). The intended application to run on the system was developed in [this UCSB class](https://www.cs.ucsb.edu/~cappello/290B/) under instruction of Dr. Pete Cappello.

## Achitecture Overview
The system developed in Dr. Cappello's class consists of a compute "space" and a number of "computer" instances to which the space can delegate subtasks. A client application connects to the space and submits a compute task. All of this is developed with Java and communicates via Java Remote Method Invocation (RMI). For the application, the space maintains the RMI registry.

This system is distinct from that system in both purpose and design, but its architecture is similar. By starting a single client application, loading a JAR from the aforementioned "application", and creating an AWS account and local credentials file (~/.aws/credentials), this system then provides the option to run the space and computers on Amazon EC2 instances of varying sizes. Testing of the client application on various EC2 instance sizes is thus very easy.

## Setup
[in progress]

## Use
