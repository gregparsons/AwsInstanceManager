package rmi;

import com.amazonaws.services.ec2.model.Instance;
import system.HwComputer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


public interface HwManagerInterface extends Remote {

	public static int _port = 9876;
	public static String _serviceName = "aws_hardware_manager";

	void registerComputer(HwComputerInterface hwComputerInterface) throws RemoteException;

	List<Instance> getRunningInstances() throws RemoteException;

	String userJustCheckingIn() throws RemoteException;

}
