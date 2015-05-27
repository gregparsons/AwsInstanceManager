package com.swimr.aws.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface HwComputerInterface extends Remote {

	/**
	 * Start the number of logical computers specified. They should be informed of the
	 * RMI registry/space url to call back to.
	 */
	boolean startLogicalComputers(int numComputers, String spaceURL) throws RemoteException;

	List<Process> getRunningProcessList() throws RemoteException;

	String getAwsInstanceId() throws RemoteException;
}
