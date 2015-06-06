#!/bin/bash
counter=0
while [ $counter -lt 1000 ]; do
	echo $counter
	let counter=counter+1

	#sleep seconds
	sleep 1
done