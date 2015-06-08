package com.swimr.aws.system;

import com.swimr.aws.rmi.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HwUser implements HwUserInterface {

	List<Process> _applicationProcesses = new ArrayList<>();

	class TestSettings{
		int num_cities = 10;
		int num_hw_computers = 1;
		Utils.Hw_Computer_Size size_hw_computers = Utils.Hw_Computer_Size.micro;	// 1=micro; 2=large; 3=2XL
	}

	TestSettings _testSettings = new TestSettings();

	HwManagerInterface _hwManager;
	boolean _connectedToHwManager = false;
	String _domainName = "";

	// List<Instance> instances;



	HwUser(String domain)
	{
		_domainName = domain;

	}


	int readUserInput(){
		// Read from stdin
		Scanner in = new Scanner(System.in);
		int selection = 0;

		try {
			if(in.hasNextInt()) {
				selection = in.nextInt();
			}
			else if(in.hasNextLine())
				in.nextLine();
		}catch(Exception e){
			// do nothing
			selection = 0;
		}
		return selection;
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

			System.out.print("> ");
			/*
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
			*/

			processInput(readUserInput());

		}
	}

	boolean isHwManagerAlive(){
		if(_hwManager == null)
			return false;

		try {
			_hwManager.computerRequestsHeartbeatOfHwManager();
			return true;
		} catch (RemoteException e) {
			return false;
		}

	}

	void printMenu(){


		if(isHwManagerAlive() == true){
			_connectedToHwManager = true;
		}
		else
			_connectedToHwManager = false;


		 System.out.println("\n\n\n******************************************************************\n"
				 + "Welcome. Connected to HwManager: " + _connectedToHwManager
				 + "\n\nSelect an option:\n\n"
				 + "1. Status\n"
				 + "2. Run Application...\n"
				 + "3. This Menu\n"
				 + "4. AWS Management...\n"
				 + "5. Exit\n"
		 );
		 //System.out.print("> ");

	 }

	void printTspMenu(){

		System.out.println("\n\n\n******************************************************************\n"
				+ "TSP Options. Connected to Hardware Manager: " + _connectedToHwManager
				+ "\n\nSelect an option:\n\n"
				+ "21. Start Space and Computers...\n"
				+ "22. Run Tsp Application\n"
				+ "23. Terminate All Test Processes\n"
				+ "\n\n\n"
		);
	}

	void runTspTestPrep(){

		Utils.Hw_Request hwRequest = new Utils.Hw_Request();
		int numCities = 10;

		System.out.print("\nWhat size computer instance (1=micro, 2=Large, 3=2XL? > ");
		//int input = readUserInput();
		switch (readUserInput()){
			case 1:
				hwRequest.size = Utils.Hw_Computer_Size.micro;
				break;
			case 2:
				hwRequest.size = Utils.Hw_Computer_Size.large;
				break;
			case 3:
				hwRequest.size = Utils.Hw_Computer_Size.two_xl;
				break;
		}

		boolean notDone = true;
		while(notDone) {


			System.out.print("\nHow many computers? [" + _testSettings.num_hw_computers + "] > ");
			int input = readUserInput();
			if (input > 0 && input <= Utils.MAX_EC2_INSTANCES_AT_A_TIME) {
				hwRequest.numHwComputers = input;
				notDone = false;
			} else {
				System.out.print("Try something smaller than " + Utils.MAX_EC2_INSTANCES_AT_A_TIME + " > ");

			}
		}
/*
		System.out.print("\nHow many cities? [" + numCities  +"] > ");
		input = readUserInput();
		if(input>0)
			numCities = input;
*/
		// Start the instance(s)

		if(_hwManager == null){
			System.out.println("[HwUser.startComputeInstance] Hardware Manager isn't started.");
			return;
		}


		try {
			_hwManager.startApplicationSpaceAndComputers(hwRequest);
		} catch (RemoteException e) {
			System.out.println("[HwUser.startComputeInstance] Hardware Manager isn't started.");
		}


//		runTspClient_0(); // with numCities entered above

	}



	void printAwsMenu(){
		System.out.println("\n\n\n******************************************************************\n"
				+ "TSP Options. Connected to Hardware Manager: " + _connectedToHwManager
				+ "\n\nSelect an option:\n\n"
				//+"Start hardware manager"
				+ "41. Connect to Hardware Manager\n"
				+ "42. Start Compute Instance(s)...\n"
				+ "\n\n\n"
		);
	}

	void startComputeInstanceMenu(){

		Utils.Hw_Request hwRequest = new Utils.Hw_Request();


		//
		System.out.print("\nWhat size instance (1=micro, 2=Large, 3=2XL? > ");
		//int input = readUserInput();
		switch (readUserInput()){
			case 1:
				hwRequest.size = Utils.Hw_Computer_Size.micro;
				break;
			case 2:
				hwRequest.size = Utils.Hw_Computer_Size.large;
				break;
			case 3:
				hwRequest.size = Utils.Hw_Computer_Size.two_xl;
				break;
		}



		System.out.print("\nHow many? [" + _testSettings.num_hw_computers + "] > ");
		int input = readUserInput();
		if(input > 0 && input <= Utils.MAX_EC2_INSTANCES_AT_A_TIME){
			hwRequest.numHwComputers = input;
		}
		else {
			System.out.print("Try something smaller than " + Utils.MAX_EC2_INSTANCES_AT_A_TIME + " > ");
		}


		// Start the instance(s)

		if(_hwManager == null){
			System.out.println("[HwUser.startComputeInstance] Hardware Manager isn't started.");
			return;
		}


		try {
			_hwManager.startHardwareComputers(hwRequest);
		} catch (RemoteException e) {

			System.out.println("[HwUser.startComputeInstance] Network call to Hardware Manager failed.");
			//e.printStackTrace();
		}

	}

	void processInput(int selection){

		//System.out.println("Selection: " + selection);

		switch (selection){
			case 1:{
				printSystemStatusFromManager();
				break;
			}
			case 2: {
				//runTsp();
				printTspMenu();
				break;
			}
			case 3:{
				// main menu
				printMenu();
				break;
			}
			case 4:{
				// AWS Management
				printAwsMenu();
				break;
			}
			case 5:{
				exitProgramFromMenu();
				break;
			}

			//
			case 41:{
				connectHwManager();
				break;
			}
			case 42:{
				// start hw-computers
				startComputeInstanceMenu();
				break;
			}

			case 21:{
				runTspTestPrep();	//start space and computers
				break;
			}
			case 22:{
				runTspClient_0();
				break;
			}
			case 23:{
				terminateAllApplicationProcesses();
				break;
			}
			default:{
				System.out.println("Select something else: " + selection);

			}
		}
	}

	void terminateAllApplicationProcesses(){

		// client processes
		for(Process process:_applicationProcesses){
			if(process!=null){
				process.destroy();
			}
		}

		// tell manager to clear space and all computer processes
		if(_hwManager!=null){
			try {
				_hwManager.terminateApplicationSpaceAndComputers();
			} catch (RemoteException e) {
				System.out.println("Network error calling manager in terminateAllApplicationProcesses.");
			}
		}

	}

	void runTspClient_0(){


		String startSwClientScript = "scripts/sw_client_startup.sh";
		String[] commands = {startSwClientScript};

		System.out.println("[HwUser.runTspApplication] running script: " + startSwClientScript);

		// Run system command to run application client script
		try {
			Process p = Runtime.getRuntime().exec(commands);
			if(p!=null)
				_applicationProcesses.add(p);
			System.out.println("[HwUser.runTspApplication] pid: " + p.toString() + ", isAlive: " + p.isAlive());
		} catch (IOException/*|InterruptedException*/ e) {
			System.out.println("[HwUser.runTspApplication] IOException running local TSP client/job script.");
		}
	}

	// *** 2 ***
	void printSystemStatusFromManager(){

		if(_hwManager == null){
			System.out.println("[HwUser.printSystemStatusFromManager] Hardware Manager is null. Start HwManager instance first.");
			return;
		}
		StatusTransportObject statusObject = null;
		try {


			// Ask AWS Hardware Manager to get the status of all running HwComputers and logical compute processes.
			statusObject = _hwManager.getSystemStatus();

		} catch (RemoteException e) {
			System.out.println("[HwUser.printSystemStatusFromManager] Network call to manager failed. Setting hwMgr to null.");
			_hwManager = null;
			_connectedToHwManager = false;
			//e.printStackTrace();
		}

			//print all this here!!!
			if(statusObject==null){
				System.out.println("[HwUser.printSystemStatusFromManager] Error: Status came back null.");
				return;
			}


			System.out.println("[HwUser.printSystemStatusFromManager]\n***** System Status *****\n");


			// Print micro instances
			Map<String,HwComputerInterface> list= statusObject.computer_lists.get(Utils.Hw_Computer_Size.micro);
			if(list !=null){
				System.out.println("Micro instances: " + list.size());
				/*
				for(HwComputerInterface c:list){

					if(c!=null)
						System.out.println(c.getAwsInstanceId());
				}
				*/
			}

			// Print large instances
			list= statusObject.computer_lists.get(Utils.Hw_Computer_Size.large);
			if(list !=null){
				System.out.println("Large instances: " + list.size());
				/*for(HwComputerInterface c:list){
					if(c!=null)
						System.out.println(c.getAwsInstanceId());
				}
				*/
			}

			// Print 2XL instances
			list= statusObject.computer_lists.get(Utils.Hw_Computer_Size.two_xl);
			if(list !=null){
				System.out.println("2XL instances: " + list.size());
				/*
				for(HwComputerInterface c:list){
					if(c!=null)
						System.out.println(c.getAwsInstanceId());
				}
*/
			}

			// Print unknown instances
			list= statusObject.computer_lists.get(Utils.Hw_Computer_Size.unknown);
			if(list !=null){
				System.out.println("Unknown instances: " + list.size());
				/*for(HwComputerInterface c:list){
					if(c!=null)
						System.out.println(c.getAwsInstanceId());
				}
				*/
			}

			if(statusObject._logicalComputerProcesses!=null){
				System.out.println("\nComputer processes running: "
					+ statusObject._logicalComputerProcesses.size());

				statusObject._logicalComputerProcesses.forEach(System.out::println);
			}


			if(statusObject._logicalSpaceProcessesOnHwManager!=null){
				System.out.println("\nSpace processes (on HwMgr): "
					+ statusObject._logicalSpaceProcessesOnHwManager.size());

				statusObject._logicalSpaceProcessesOnHwManager.forEach(System.out::println);

			}
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



	// Menu item 8
	void exitProgramFromMenu(){
		System.exit(0);
	}




	public static void main(String[] args){
		System.out.println("[system.HwUser.main] args: " + args[0]);
		if(args.length>0) {
			HwUser hwUser = new HwUser(args[0]);
			hwUser.runConsole();
		}
	}
}