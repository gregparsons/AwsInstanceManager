package com.swimr.aws.rmi;

import com.amazonaws.services.ec2.model.Instance;
import com.swimr.aws.system.HwComputer;

import java.io.Serializable;
import java.util.*;


/**
 *
 * Class to facilitate transporting a list of hardware and logical computers
 * back to the user interface from the hardware manager.
 *
 */
public class StatusTransportObject implements Serializable {

	public Map<Utils.Hw_Computer_Size, Map<String,HwComputerInterface>> computer_lists = new HashMap<Utils.Hw_Computer_Size, Map<String,HwComputerInterface>>();


	// Map of 290b computers running on the above computers. Above computers' id
	// is the key. List is the list of computer process IDs.

	public List<String> _logicalComputerProcesses = new ArrayList<>();


	//Processes are not serializable
	public List<String> _logicalSpaceProcessesOnHwManager = new ArrayList<>();

	public StatusTransportObject(Map<Utils.Hw_Computer_Size, Map<String, HwComputerInterface>> computers){
		computer_lists = computers;


	}
}
