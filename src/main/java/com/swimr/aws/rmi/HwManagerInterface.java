package com.swimr.aws.rmi;

import com.amazonaws.services.ec2.model.Instance;
import com.swimr.aws.system.HwComputer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


public interface HwManagerInterface extends Remote {

	public static int _port = 9876;
	public static String _serviceName = "aws_hardware_manager";

	/**
	 * Hw_User calls this to get a snapshot of all available computers and running application processes.
	 * @return StatusTransportObject
	 * @throws RemoteException
	 */
	StatusTransportObject getSystemStatus() throws RemoteException;

	/**
	 * Hw_User calls to verify connection to Hw_Manager. Used to update status display in GUI. Could be used as a heartbeat.
	 * @return String message
	 * @throws RemoteException
	 */
	String getWelcomeMessage() throws RemoteException;		//for Hw_User

	/**
	 * Hw_Computer calls this to register itself with the Hw_Manager. Manager can then make RMI calls to it.
	 * @param computerReg
	 * @throws RemoteException
	 */
	void registerComputer(ComputerRegistration computerReg) throws RemoteException;

	/**
	 * Hw_Computer calls this to confirm existence of the Hw_Manager. Only useful in that Hw_Computer
	 * knows to reinitiate a connection if this fails. Hw_Computers thus always retry connections.
	 * @return
	 * @throws RemoteException
	 */
	String getManagerHeartbeat() throws RemoteException;	//for Hw_Computer

	/**
	 * Starts an (application) space and the number/size of (application) computers requested in the Hw_Request.
	 * @param hwRequest
	 * @throws RemoteException
	 */
	void startApplicationSpaceAndComputers(Utils.Hw_Request hwRequest) throws RemoteException;

	/**
	 * Terminate the application processes on the Hw_Manager and on all Hw_Computers.
	 * @throws RemoteException
	 */
	void terminateApplicationSpaceAndComputers() throws RemoteException;

	/**
	 * Start one or more AWS EC2 instances.
	 * @param hwRequest
	 * @throws RemoteException
	 */
	void startHw_ComputerInstances(Utils.Hw_Request hwRequest) throws RemoteException;

	/**
	 * Terminate all hardware computer AWS instances.
	 * @throws RemoteException
	 */
	void terminateAllHw_ComputerInstances() throws RemoteException;


}
