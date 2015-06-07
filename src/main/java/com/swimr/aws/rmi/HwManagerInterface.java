package com.swimr.aws.rmi;

import com.amazonaws.services.ec2.model.Instance;
import com.swimr.aws.system.HwComputer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


public interface HwManagerInterface extends Remote {

	public static int _port = 9876;
	public static String _serviceName = "aws_hardware_manager";

	void registerComputer(HwComputerInterface hwComputerInterface) throws RemoteException;

	StatusTransportObject getSystemStatus() throws RemoteException;

	String userJustCheckingIn() throws RemoteException;



	// Interfaces for Space
	//void spaceRequestsLogicalComputers(int requestedCores) throws RemoteException;



	// For HwComputers
	String computerRequestsHeartbeatOfHwManager() throws RemoteException;

	// AWS Managment for Hw_User
	void startHardwareComputer(Utils.Hw_Request hwRequest) throws RemoteException;

	void startApplicationSpaceAndComputers(Utils.Hw_Request hwRequest) throws RemoteException;



}
