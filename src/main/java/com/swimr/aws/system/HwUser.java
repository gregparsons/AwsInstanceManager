package com.swimr.aws.system;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.sun.org.apache.bcel.internal.generic.FieldGenOrMethodGen;
import com.swimr.aws.rmi.*;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;

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

	static AmazonEC2 ec2;
	static List<Instance> _awsInstancesRunningHwManagerAMI = new ArrayList<Instance>();



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
		// connectHwManager();





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
			_hwManager.getManagerHeartbeat();
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
				+ "22. Start Application\n"
				+ "23. Terminate All Test Processes\n"
				+ "\n\n\n"
		);
	}

	void startApplicationSpaceAndComputers(){

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

		if(_hwManager == null){
			System.out.println("[HwUser.startComputeInstance] Hardware Manager isn't started.");
			return;
		}

		try {
			_hwManager.startApplicationSpaceAndComputers(hwRequest);
		} catch (RemoteException e) {
			System.out.println("[HwUser.startComputeInstance] Hardware Manager isn't started.");
		}
	}




	void printAwsMenu(){



		System.out.println("\n\n\n******************************************************************\n"
				+ "TSP Options. Connected to Hardware Manager: " + _connectedToHwManager
				+ "\n\nSelect an option:\n\n"
				//+"Start hardware manager"
				+ "41. Connect to Hardware Manager\n"
				+ "42. Start Compute Instance(s)...\n"
				+ "43. Start/Reboot Hardware Manager\n"
				+ "44. Hardware Manager Status\n"
				+ "45. Terminate All Compute Instances\n"
				+ "\n\n"
		);
	}

	void startComputeInstanceMenu(){

		Utils.Hw_Request hwRequest = new Utils.Hw_Request();

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
			_hwManager.startHw_ComputerInstances(hwRequest);
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
			case 43:{
				//start/reboot hardware manager
				startOrRebootHwManager();
				break;
			}
			case 44:{
				initAWS();
				break;
			}
			case 45:{
				if(_hwManager!=null){
					try {
						_hwManager.terminateAllHw_ComputerInstances();
					} catch (RemoteException e) {
						System.out.println("Network call to Hardware Manager failed. No instances terminated.");
					}
				}
			}



			case 21:{
				startApplicationSpaceAndComputers();	//start space and computers
				break;
			}
			case 22:{
				startApplicationClient();	//start application/job/task/client
				break;
			}
			case 23:{
				terminateAllApplicationProcesses();
				break;
			}
			default:{
				System.out.println("Select something else: " + selection);
				break;
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

	void startApplicationClient(){

		String startSwClientScript = "scripts/sw_client_startup.sh";

		if(SystemUtils.IS_OS_WINDOWS)
			startSwClientScript = "scripts\\sw_client_startup_windows.bat";

		//String startSwClientScript = "java -Djava.security.policy=scripts"+ File.separator + "jars" + File.separator + "policy -cp scripts" + File.separator + "jars" + File.separator + "h4.jar applications.euclideantsp.JobEuclideanTsp djava.dyndns.org";
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

	void printSystemStatusFromManager(){

		System.out.println("\n\n***** System Status (From Hardware Manager) *****\n");

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
		if(statusObject==null){
			System.out.println("[HwUser.printSystemStatusFromManager] Error: Status came back null.");
			return;
		}

		System.out.println("Instances Available:\n");

			// Print micro instances
		Map<String,HwComputerInterface> list= statusObject.computer_lists.get(Utils.Hw_Computer_Size.micro);
		if(list !=null){
			System.out.println("   t2.micro\t(1 vCPU)\t" + list.size());
		}

		// Print large instances
		list= statusObject.computer_lists.get(Utils.Hw_Computer_Size.large);
		if(list !=null){
			System.out.println("   m3.large\t(2 vCPU)\t" + list.size());
		}

		// Print 2XL instances
		list= statusObject.computer_lists.get(Utils.Hw_Computer_Size.two_xl);
		if(list !=null){
			System.out.println("   c4.2xlarge\t(8 vCPU)\t" + list.size());
		}

		// Print unknown instances
		list= statusObject.computer_lists.get(Utils.Hw_Computer_Size.unknown);
		if(list !=null){
			System.out.println("   Non-AWS\t\t\t" + list.size());
		}


		System.out.println("\nApplication Processes:\n");

		if(statusObject._logicalComputerProcesses!=null){
			System.out.println("   Computer\t\t\t"
				+ statusObject._logicalComputerProcesses.size());
			statusObject._logicalComputerProcesses.forEach(System.out::println);
		}

		if(statusObject._logicalSpaceProcessesOnHwManager!=null){
			System.out.println("   Space\t\t\t"
				+ statusObject._logicalSpaceProcessesOnHwManager.size());
			statusObject._logicalSpaceProcessesOnHwManager.forEach(System.out::println);
		}
	}

	void connectHwManager(){

		// Make RMI connection to Hw_Manager at djava.dyndns.org

		//Make this a thread that keeps trying in the background.

		String url = "//" + _domainName + ":" + HwManager._port + "/" + HwManager._serviceName;
		System.out.println("[HwUser.main] Connecting to: "+url);
		try {
			_hwManager = (HwManagerInterface) Naming.lookup(url);
			if (_hwManager != null) {
				String reply = _hwManager.getWelcomeMessage();
				System.out.println("[HwUser.HwUser] reply from manager: " + reply);
				_connectedToHwManager = true;
			}
			else {
				System.out.println("[HwUser.HwUser] Naming.lookup returned null.");
			}
		}
		catch (NotBoundException|MalformedURLException e) {
			System.out.println("[HwUser.main] Non-network error connecting to Hardware Manager.");
		}
		catch(RemoteException e){
			System.out.println("[HwUser.main] Network error: Could not connect to Hardware Manager.");
			//e.printStackTrace();
		}
		if(_connectedToHwManager)
			System.out.println("[HwUser.main] Connected to: " + url);
		else
			System.out.println("[HwUser.main] Connect failed to: " + url);
	}


	void exitProgramFromMenu(){
		System.exit(0);
	}






	// Initialize Amazon API
	private static void initAWS()// throws Exception
	{
		// ProfileCredentialsProvider loads AWS security credentials from
		// .aws/config file in your home directory.

		_awsInstancesRunningHwManagerAMI.clear();


		File configFile = new File(System.getProperty("user.home"), ".aws/credentials");
		AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(
			new ProfilesConfigFile(configFile), "default");

		if (credentialsProvider.getCredentials() == null)
		{
			throw new RuntimeException("No AWS security credentials found:\n"
				+ "Make sure you've configured your credentials in: " + configFile.getAbsolutePath() + "\n"
				+ "For more information on configuring your credentials, see "
				+ "http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html");
		}

		ec2 = new AmazonEC2Client(credentialsProvider);
		try
		{
			// Set region.
			// https://docs.aws.amazon.com/general/latest/gr/rande.html
			// Oregon
			com.amazonaws.regions.Region usWest2 = com.amazonaws.regions.Region.getRegion(com.amazonaws.regions.Regions.US_WEST_2);
			ec2.setRegion(usWest2);

			/*
			// Zones within this region.
			DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
			List<AvailabilityZone> availabilityZones = availabilityZonesResult.getAvailabilityZones();
			System.out.println("You have access to " + availabilityZones.size() + " availability zones:");
			for (AvailabilityZone zone : availabilityZones)
			{
				System.out.println(" - " + zone.getZoneName() + " (" + zone.getRegionName() + ")");
			}
			*/
			DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
			Set<Instance> instances = new HashSet<Instance>();
			for (Reservation reservation : describeInstancesResult.getReservations())
			{

				List<Instance> reservationInstances = reservation.getInstances();
				instances.addAll(reservationInstances);
			}

			//System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");

			int runningInstances = 0;
			for(Instance i:instances)
			{

				//if pending or running
				if(i.getImageId().equals(Utils.HW_MANAGER_AMI))
				{
					if(i.getState().getCode() == 16 || i.getState().getCode()==0)
					{
						//https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/InstanceState.html
						runningInstances++;
						_awsInstancesRunningHwManagerAMI.add(i);

						System.out.println("Id: " + i.getInstanceId() + ": "
								+ i.getPublicIpAddress()
								+ ", Img: " + i.getImageId()
								+ ", state: " + i.getState().getName()
						);
					}
				}
			}
			System.out.println("Hardware Manager instances: " + runningInstances);

		}
		catch (AmazonServiceException ase)
		{
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		}
		catch (AmazonClientException ace)
		{
			System.out.println("Error Message: " + ace.getMessage());
		}
	}


	void startOrRebootHwManager(){


		// Connect to AWS API, see if a hardware manager is running (by AMI id). Start or restart.

		initAWS();

		if(_awsInstancesRunningHwManagerAMI.size() > 0){
			// then there's a hw_manager running, reboot it

			Instance hwMgrInstance = _awsInstancesRunningHwManagerAMI.get(0); //first

			if(hwMgrInstance!=null) {
				RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
				rebootInstancesRequest.withInstanceIds(hwMgrInstance.getInstanceId());

				if(ec2!=null){
					ec2.rebootInstances(rebootInstancesRequest);
					System.out.println("Instance " + hwMgrInstance.getInstanceId() + " is rebooting.");
				}
			}
		}
		else{
			// nothing with that AMI running, start it

			// https://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/run-instance.html
			System.out.println("Launching instance of ami: " + Utils.HW_MANAGER_AMI + " and size: " + Utils.AWS_HW_MANAGER_DEFAULT_SIZE);
			RunInstancesRequest runRqst = new RunInstancesRequest();
			String startupUserData =  ""; 			// Never got this working

			runRqst.withImageId(Utils.HW_MANAGER_AMI)
				.withInstanceType(Utils.AWS_HW_MANAGER_DEFAULT_SIZE)
				.withMinCount(1)
					// .withMaxCount(Utils.MAX_EC2_INSTANCES_AT_A_TIME) // NOT THIS
				.withMaxCount(1)
				.withKeyName(Utils.KEY_NAME)
				.withSecurityGroups(Utils.SECURITY_GROUP)
				.withUserData(startupUserData);

			if(ec2!=null)
			{
				RunInstancesResult result = ec2.runInstances(runRqst);
				System.out.println("Run Result: " + result.toString());
				//String id = result.getReservation().getInstances().get(0).getImageId();
			}
		}
	}



	public static void main(String[] args){
		System.out.println("[system.HwUser.main] args: " + args[0]);
		if(args.length>0) {
			// Start the user console
			HwUser hwUser = new HwUser(args[0]);
			hwUser.runConsole();
		}
	}
}