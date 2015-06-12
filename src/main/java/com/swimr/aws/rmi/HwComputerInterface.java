package com.swimr.aws.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface HwComputerInterface extends Remote {

	/**
	 * Start the number of logical computers specified. They should be informed of the
	 * RMI registry/space url to call back to.
	 */
	boolean startLogicalComputers(int numComputers) throws RemoteException;

	/**
	 * HwManager calls this to get a snapshot of what SwComputer processes are running
	 * on this computer
	 * @return List<String>
	 * @throws RemoteException
	 */
	List<String> getRunningProcessStrings() throws RemoteException;

	/**
	 * Not used. Can be used to ask a computer what its Amazon instance ID is.
	 * @return String
	 * @throws RemoteException
	 */
	String getAwsInstanceId() throws RemoteException;

	/**
	 * Find out what size this computer is.
	 * @return
	 * @throws RemoteException
	 */
	Utils.Hw_Computer_Size getEc2Size() throws RemoteException;

	/**
	 * Terminate the application processes running on this computer.
	 * @throws RemoteException
	 */
	void terminateSwComputers() throws RemoteException;

	/**
	 * Heartbeat for manager to call to see if the computer is still there (or if it should remove it
	 * from the computer list.
	 * @return boolean
	 * @throws RemoteException
	 */
	boolean isAlive() throws RemoteException;

}
