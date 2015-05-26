package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface HwComputerInterface extends Remote {

	/**
	 * Start the number of logical computers specified. They should be informed of the
	 * RMI registry/space url to call back to.
	 */
	boolean startLogicalComputers(int numComputers, String spaceURL) throws RemoteException;

}
