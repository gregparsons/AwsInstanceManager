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
				printSystemStatusFromManager();
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

		// 1. Run Client Job/Task/Application
		//runTspApplication_0();

		// 2. Run Space
		//String urlForSpaceRegistry = "//:pathToThisSpace:PORT/space_registry_OR_USE_HW_MANAGER_REGISTRY";
		runTspSpace_1();

		// 3. Run Computers
		int numComputersDesired = 1;
		runTspComputers_2(numComputersDesired);


	}


	// DOES NOTHING CURRENTLY: CHANGE THIS SCRIPT TO RUN THE TSP APPLICATION LOCALLY
	void runTspApplication_0(){

		String computerStartCommand = "/Users/aaa/290a/aws/aws-test1/aws-test1/scripts/ZZZZZZZZZZZZZZZ.sh";
		String[] commands = {computerStartCommand};
		try {
			Process p = Runtime.getRuntime().exec(commands);
			System.out.println("[HwUser.runTspApplication] pid: " + p.toString() + ", isAlive: " + p.isAlive());

		} catch (IOException/*|InterruptedException*/ e) {
			e.printStackTrace();
		}
	}

	void runTspSpace_1(){
		try {
			if(_hwManager!=null) {
				_hwManager.startApplicationSpaceOnHwManager();
				return;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println("[HwUser.runTspSpace] Net call to startApplicationSpaceOnHwManager failed.");
	}

	void runTspComputers_2(int numComputersDesired){
		// Tell the HwManager how many computers to start for this space.
	}




	// *** 2 ***
	void printSystemStatusFromManager(){

		if(_hwManager == null){
			System.out.println("[HwUser.printSystemStatusFromManager] Hardware Manager is null. Start HwManager instance first.");
			return;
		}

		try {


			// Ask AWS Hardware Manager to get the status of all running HwComputers and logical compute processes.
			StatusTransportObject statusObject = _hwManager.getSystemStatus();

			//print all this here!!!
			if(statusObject==null){
				System.out.println("[HwUser.printSystemStatusFromManager] Error: Status came back null.");
				return;
			}


			System.out.println("[HwUser.printSystemStatusFromManager] System Status\n");

			if(statusObject._awsInstances!=null) {
				System.out.println("[HwUser.printSystemStatusFromManager] Hardware Computers Running (AWS Instances): "
					+ statusObject._awsInstances.size());
				//statusObject._awsInstances.forEach(System.out::println);

				for(HwComputerInterface awsComputer:statusObject._awsInstances){

					System.out.println("  Aws Id: " + awsComputer.getAwsInstanceId());
				}

			}
			if(statusObject._logicalComputerProcesses!=null){
				System.out.println("[HwUser.printSystemStatusFromManager] Logical compute processes running: "
					+ statusObject._logicalComputerProcesses.size());

				statusObject._logicalComputerProcesses.forEach(System.out::println);
			}





			if(statusObject._logicalSpaceProcessesOnHwManager!=null){
				System.out.println("[HwUser.printSystemStatusFromManager] Space processes running (on hw mgr): "
					+ statusObject._logicalSpaceProcessesOnHwManager.size());

				statusObject._logicalSpaceProcessesOnHwManager.forEach(System.out::println);

			}







		} catch (RemoteException e) {
			System.out.println("[HwUser.printSystemStatusFromManager] Network call to manager failed. Setting hwMgr to null.");
			_hwManager = null;
			e.printStackTrace();
		}

		if(instances!=null)
			System.out.println("[HwUser.printSystemStatusFromManager] Running instances: ");
			instances.forEach(System.out::println);	// print all running instances



	}


	// ***** 5 Connect to the Hardware Manager *****
	void connectHwManager(){

		//Make this a thread that keeps trying in the background.

		String url = "//" + _domainName + ":" + HwManager._port + "/" + HwManager._serviceName;
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

		}
		catch (NotBoundException|MalformedURLException e)
		{
			System.out.println("[HwUser.main] Non-network error connecting to Hardware Manager.");
			// e.printStackTrace();

		}
		catch(RemoteException e){
			System.out.println("[HwUser.main] Network error: Could not connect to Hardware Manager.");

		}
		if(_connectedToHwManager)
			System.out.println("[HwUser.main] Connected to: " + url);
		else
			System.out.println("[HwUser.main] Connect failed to: " + url);
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