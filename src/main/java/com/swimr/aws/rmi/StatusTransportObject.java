package com.swimr.aws.rmi;

import com.amazonaws.services.ec2.model.Instance;
import com.swimr.aws.system.HwComputer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * Class to facilitate transporting a list of hardware and logical computers
 * back to the user interface from the hardware manager.
 *
 */
public class StatusTransportObject implements Serializable {

	public List<HwComputerInterface> _awsInstances = new ArrayList<>();

	// Map of 290b computers running on the above computers. Above computers' id
	// is the key. List is the list of computer process IDs.

	public List<String> _logicalComputerProcesses = new ArrayList<>();


	//Processes are not serializable
	public List<String> _logicalSpaceProcessesOnHwManager = new ArrayList<>();

}
