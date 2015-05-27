package com.swimr.aws.system;

import com.amazonaws.services.ec2.model.Instance;
import com.swimr.aws.rmi.HwComputerInterface;
import com.swimr.aws.rmi.HwManagerInterface;
import com.swimr.aws.rmi.HwUserInterface;
import com.swimr.aws.rmi.StatusTransportObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

public class HwUser implements HwUserInterface {

	HwManagerInterface _hwManager;
	boolean _connectedToHwManager = false;
	String _domainName = "";

	List<Instance> instances;



	HwUser(String domain)
	{
		_domainName = domain;

	}



	void runConsole(){

		// Try connecting right away.
		connectHwManager();

		// Read from stdin
		Scanner in = new Scanner(System.in);
		int selection = 0;

		// Infinite loop user interface
		printMenu();
		while(true){
			//printMenu();
			System.out.print("> ");

			try {
				if(in.hasNextInt()) {
					selection = in.nextInt();
					processInput(selection);
					in.nextLine();
				}
				else if(in.hasNextLine())
					in.nextLine();
			}catch(Exception e){
				// do nothing
				selection = 0;
			}
		}
	}


	void printMenu(){

		 System.out.println("\n\n\n******************************************************************\n"
				 + "Welcome to DivA. Connected: " + _connectedToHwManager
				 + "\n\nSelect an option:\n\n"
				 + "0. Show This Menu\n"
				 + "1. Run Traveling Salesman Problem...\n"
				 + "2. System Status\n"
				 + "3. Clear All Worker Processes\n"
				 + "4. Terminate AWS Instance...\n"
				 + "5. Connect to Diva Manager (hardware manager/space instance)\n"
				 + "6. Start Diva Hardware Manager (on AWS)\n"
				 + "7. Start a hardware computer (AWS instance) TEST\n"
				 + "\n\n\n"
		 );
		 //System.out.print("> ");

	 }


	void processInput(int selection){

		System.out.println("Selection: " + selection);

		switch (selection){
			case 1: {
				runTsp();
				break;
			}
			case 2:{
				getSystemStatusFromManager();
				break;
			}
			case 3:{
				break;
			}
			case 4:{
				break;
			}
			case 5:{
				connectHwManager();
				break;
			}
			case 0:{
				printMenu();
				break;
			}
			case 7:{

			}
			default:{
				System.out.println("Select something else: " + selection);

			}
		}
	}







	// *** 1 ***
	void runTsp(){

		String computerStartCommand = "/Users/aaa/290a/aws/aws-test1/aws-test1/computer.sh";
		String[] commands = {computerStartCommand};

		try {

			Process p = Runtime.getRuntime().exec(commands);
			System.out.println("[HwComputer.startLogicalComputers] pid: " + p.toString() + ", isAlive: " + p.isAlive());

		} catch (IOException/*|InterruptedException*/ e) {
			e.printStackTrace();
		}



	}




	// *** 2 ***
	void getSystemStatusFromManager(){

		try {
			if(_hwManager == null){
				System.out.println("[HwUser.getSystemStatusFromManager] Hardware Manager is null. Start HwManager instance first.");

				return;
			}


			// Ask AWS Hardware Manager to get the status of all running HwComputers and logical compute processes.
			StatusTransportObject statusObject = _hwManager.getSystemStatus();


			//print all this here!!!
			if(statusObject==null){

				System.out.println("[HwUser.getSystemStatusFromManager] Error: Status came back null.");
				return;
			}



			System.out.println("[HwUser.getSystemStatusFromManager] System Status\n");

			if(statusObject._awsInstances!=null) {
				System.out.println("[HwUser.getSystemStatusFromManager] Hardware Computers Running (AWS Instances): "
					+ statusObject._awsInstances.size());
				//statusObject._awsInstances.forEach(System.out::println);

				for(HwComputerInterface awsComputer:statusObject._awsInstances){

					System.out.println("  Aws Id: " + awsComputer.getAwsInstanceId());
				}

			}
			if(statusObject._logicalComputerProcesses!=null){
				System.out.println("[HwUser.getSystemStatusFromManager] Logical compute processes running: "
					+ statusObject._logicalComputerProcesses.size());

				statusObject._logicalComputerProcesses.forEach(System.out::println);
			}










		} catch (RemoteException e) {
			System.out.println("[HwUser.getSystemStatusFromManager] Call to manager failed. Setting hwMgr to null.");
			_hwManager = null;
			e.printStackTrace();
		}

		if(instances!=null)
			System.out.println("[HwUser.getSystemStatusFromManager] Running instances: ");
			instances.forEach(System.out::println);	// print all running instances



	}


	// ***** 5 Connect to the Hardware Manager *****
	void connectHwManager(){


		String url = "rmi://" + _domainName + ":" + HwManager._port + "/" + HwManager._serviceName;
		System.out.println("[HwUser.main] Connecting to: "+url);
		try {

			_hwManager = (HwManagerInterface) Naming.lookup(url);
			if (_hwManager != null) {
				String reply = _hwManager.userJustCheckingIn();
				System.out.println("[HwUser.HwUser] reply from manager: " + reply);
				_connectedToHwManager = true;
			}
			else
			{
				System.out.println("[HwUser.HwUser] Naming.lookup returned null.");
			}

		} catch (NotBoundException|MalformedURLException|RemoteException e) {
			System.out.println("Could not connect to Diva Hardware Manager.");
			// e.printStackTrace();

		}
		System.out.println("[HwUser.main] Connected to: " + url);
	}







	public static void main(String[] args){
		System.out.println("[system.HwUser.main] args: " + args[0]);

		if(args.length>0)
		{

			HwUser hwUser = new HwUser(args[0]);

			hwUser.runConsole();

			/*


			try {
				hwUser._hwManager.getSystemStatus();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			*/
		}
	}
}