package com.swimr.aws.system;

// To Run:
//  mvn package exec:java

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

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


public class HwManager extends UnicastRemoteObject implements HwManagerInterface {

	//static final int MAX_EC2_INSTANCES_AT_A_TIME = 5;

    static AmazonEC2 ec2;
    //static AmazonS3  s3;

	//HW_COMPUTER AMI
	static final String _computerAmi = "ami-1d50682d";

	static final InstanceType _type = InstanceType.T2Micro;
	static final String _keyName = "290b-java";
	static final String _securityGroup = "RMI";



	// Make all these thread safe:
	static List<Instance> _instances = new ArrayList<Instance>();
	//static List<HwComputerInterface> _awsComputers = new ArrayList<>();
	static List<Process> _spaceProcesses = new ArrayList<>();


	static Map<Utils.Hw_Computer_Size, List<HwComputerInterface>> _computer_lists = new HashMap<Utils.Hw_Computer_Size, List<HwComputerInterface>>();



	public HwManager() throws RemoteException {
		super(_port);

		_computer_lists.put(Utils.Hw_Computer_Size.micro, new ArrayList<HwComputerInterface>());
		_computer_lists.put(Utils.Hw_Computer_Size.large, new ArrayList<HwComputerInterface>());
		_computer_lists.put(Utils.Hw_Computer_Size.two_xl, new ArrayList<HwComputerInterface>());
		_computer_lists.put(Utils.Hw_Computer_Size.unknown, new ArrayList<HwComputerInterface>());

	}



	// ********** Interface Methods ***********************

	@Override
	public void registerComputer(ComputerRegistration computerReg) throws RemoteException {

		System.out.println("[HwManager.registerComputer]");
		System.out.println("[HwManager.registerComputer] Computer registering id: " + computerReg.id + ", size: " + computerReg.size);

		if(computerReg.id!=null)
			System.out.println("[HwManager.registerComputer] " + computerReg.id);
		//System.out.println("[HwManager.registerComputer] " + hwComputer._amazonInstanceId);
			//is this making an RMI call somewhere just to get a local var passed in this object?

		// System.out.println("Registering " + hwComputer.getAwsInstanceId() + ", size: " + hwComputer.getEc2Size());


		if(computerReg.size == null)
			//just register as micro for now, if on localhost
			_computer_lists.get(Utils.Hw_Computer_Size.micro).add(computerReg.hwComputerInterface);
		else
			_computer_lists.get(computerReg.size).add(computerReg.hwComputerInterface);

		System.out.println("[HwManager.registerComputer] Computer registered. Id: " + computerReg.id + ", size: " + computerReg.size);



	}



	private void cleanDeadSpaceProcesses()
	{
		for(int i=_spaceProcesses.size()-1; i>=0; i--)
		{
			Process p = _spaceProcesses.get(i);
			if(p.isAlive() != true)
				_spaceProcesses.remove(p);
		}
	}


	void cleanDeadComputerInstances(){
		/*
		for(int i=_awsComputers.size()-1; i>=0; i--)
		{
			HwComputerInterface computerInterface = _awsComputers.get(i);
			if(computerInterface == null){
				_awsComputers.remove(computerInterface);
			}
		}
		*/

		for(int i=_computer_lists.get(Utils.Hw_Computer_Size.micro).size()-1; i>=0; i--){
			HwComputerInterface c = _computer_lists.get(Utils.Hw_Computer_Size.micro).get(i);
			if(c == null)
				_computer_lists.get(Utils.Hw_Computer_Size.micro).remove(c);
		}
		for(int i=_computer_lists.get(Utils.Hw_Computer_Size.large).size()-1; i>=0; i--){
			HwComputerInterface c = _computer_lists.get(Utils.Hw_Computer_Size.large).get(i);
			if(c == null)
				_computer_lists.get(Utils.Hw_Computer_Size.large).remove(c);
		}
		for(int i=_computer_lists.get(Utils.Hw_Computer_Size.two_xl).size()-1; i>=0; i--){
			HwComputerInterface c = _computer_lists.get(Utils.Hw_Computer_Size.two_xl).get(i);
			if(c == null)
				_computer_lists.get(Utils.Hw_Computer_Size.two_xl).remove(c);
		}
		for(int i=_computer_lists.get(Utils.Hw_Computer_Size.unknown).size()-1; i>=0; i--){
			HwComputerInterface c = _computer_lists.get(Utils.Hw_Computer_Size.unknown).get(i);
			if(c == null)
				_computer_lists.get(Utils.Hw_Computer_Size.unknown).remove(c);
		}
	}


	// * User calls this. Returns the object containing all the hardware and logical computers.
	public
	StatusTransportObject getSystemStatus() throws RemoteException
	{
		System.out.println("[HwManager.getSystemStatus] Computers: \n"
			+ _computer_lists.get(Utils.Hw_Computer_Size.micro).size() + " t2.micro\n"
			+ _computer_lists.get(Utils.Hw_Computer_Size.large).size() + " m3.large\n"
			+ _computer_lists.get(Utils.Hw_Computer_Size.two_xl).size() + " c4.2xlarge\n"

			+ "Getting logical compute processes...");

		StatusTransportObject transportObj = new StatusTransportObject(_computer_lists);


		// Space processes
		cleanDeadComputerInstances();
		cleanDeadSpaceProcesses();

		System.out.println("[HwManager.getSystemStatus] " + _spaceProcesses.size() + " space processes.");
		if(_spaceProcesses!= null && _spaceProcesses.size()>0) {
			for (Process process : _spaceProcesses) {
				transportObj._logicalSpaceProcessesOnHwManager.add(process.toString());
			}
		}

		// Load AWS computers and logical computer processes into a transport object.

		//for(HwComputerInterface computer: _awsComputers) {

			//transportObj._awsInstances.add(computer);


			/*
			try {
				List<String> processList = computer.getRunningProcessStrings();
				transportObj._logicalComputerProcesses.addAll(processList);
			}
			catch(RemoteException e){
				System.out.println("[HwManager.getSystemStatus] Connection attempt to computer failed. Removing.");
				transportObj._awsInstances.remove(computer);
				//_awsComputers.remove(computer);	// this will screw up the for loop iterator
				computer = null; //this will let cleanDead find it

			}

			*/

		//}


		System.out.println("[HwManager.getSystemStatus] returning");

		return transportObj;
	}






	// * Greeting to user/client, confirming connection.
	@Override
	public String userJustCheckingIn() {
		return "hello from manager!";
	}






	// ************************* APPLICATION helper functions **************************************************








	// ************************* END user application helper functions *************




	// ********** END Interface Methods **************




	// Init
    private static void initAWS() throws Exception
	{
        /*
         * ProfileCredentialsProvider loads AWS security credentials from a
         * .aws/config file in your home directory.
         *
         */
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
        //s3  = new AmazonS3Client(credentialsProvider);


        try
		{
			 //* Set region.


			//https://docs.aws.amazon.com/general/latest/gr/rande.html
			com.amazonaws.regions.Region usWest2 = com.amazonaws.regions.Region.getRegion(com.amazonaws.regions.Regions.US_WEST_2);
            ec2.setRegion(usWest2);

			// * Zones within this region.
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            List<AvailabilityZone> availabilityZones = availabilityZonesResult.getAvailabilityZones();
            System.out.println("You have access to " + availabilityZones.size() + " availability zones:");
            for (AvailabilityZone zone : availabilityZones)
			{
                System.out.println(" - " + zone.getZoneName() + " (" + zone.getRegionName() + ")");
            }

            DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
            Set<Instance> instances = new HashSet<Instance>();
            for (Reservation reservation : describeInstancesResult.getReservations())
			{

				List<Instance> reservationInstances = reservation.getInstances();
				instances.addAll(reservationInstances);
            }

			System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");

			int runningInstances = 0;
			for(Instance i:instances)
			{
				System.out.println("Id: " + i.getInstanceId() + ": "
					+ i.getPublicIpAddress()
					+ ", Img: " + i.getImageId()
					+ ", state: " + i.getState().getName()
				);

				//if pending or running
				if(i.getImageId().equals(_computerAmi))
				{
					if(i.getState().getCode() == 16 || i.getState().getCode()==0)
					{
						//https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/InstanceState.html
						runningInstances++;
						_instances.add(i);
					}
				}
			}


			System.out.println("Running/pending instances: " + runningInstances);

/*
			// launch at most 2 instances ... where ami ==
			if(runningInstances < 2)
			{
				launchInstance();
			}


			terminateAllInstances();

*/

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

	// AWS Managment for Hw_User
	@Override
	public void startHardwareComputers(Utils.Hw_Request hwRequest){

		InstanceType instanceSize = InstanceType.T2Micro;

		switch(hwRequest.size){
			case micro:{
				instanceSize = InstanceType.T2Micro;
				break;
			}
			case large:{
				instanceSize = InstanceType.M3Large;		//4 vCPU
				break;
			}
			case two_xl:{
				instanceSize = InstanceType.C42xlarge;		//8 vCPU
				break;
			}
			default:{
				System.out.println("HwManager: No size specified");
				instanceSize = InstanceType.T2Micro;
				break;
			}


		}


		// https://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/run-instance.html
		System.out.println("Launching instance of ami: " + _computerAmi + " and size: " + _type);

		RunInstancesRequest runRqst = new RunInstancesRequest();

		// Put startup script in here
		// https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html
		//
		//https://help.ubuntu.com/community/CloudInit

		String startupUserData =  "IyEvYmluL2Jhc2ggCmNkIC9ob21lL3VidW50dS8yOTBiL0F3c0luc3RhbmNlTWFuYWdlcjsgCm12biB0ZXN0IC1QY29tcHV0ZXI=";

		//String startupUserData = "#!/bin/bash cd /home/ubuntu/290b/AwsInstanceManager; mvn test -Pcomputer";
		//startupUserData = com.amazonaws.util.Base64.encodeAsString(startupUserData.getBytes());

		runRqst.withImageId(_computerAmi)
			.withInstanceType(instanceSize)
			.withMinCount(hwRequest.numHwComputers)
			// .withMaxCount(Utils.MAX_EC2_INSTANCES_AT_A_TIME) // NOT THIS
			.withMaxCount(hwRequest.numHwComputers)
			.withKeyName(_keyName)
			.withSecurityGroups(_securityGroup)
			.withUserData(startupUserData);


		if(ec2!=null)
		{
			RunInstancesResult result = ec2.runInstances(runRqst);
			System.out.println("Run Result: " + result.toString());
			//String id = result.getReservation().getInstances().get(0).getImageId();
		}
		/*
		else{
			try {
				initAWS();
			} catch (Exception e) {

				e.printStackTrace();
			}
		}
		*/
	}

	void killAllSpaceProcesses(){

		if(_spaceProcesses!=null && _spaceProcesses.size() > 0){

			for(Process process:_spaceProcesses){
				process.destroy();
			}
		}

		//give them time to clean up
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			//
		}
	}


	// Start an application SPACE on this instance. Save the PID so it can be killed, etc.
	private void startApplicationSpaceOnHwManager() // throws RemoteException
	{

		killAllSpaceProcesses();



		String scriptToRun = "scripts/sw_space_startup.sh";
		System.out.println("[HwManager.startApplicationSpace] Exec: " + scriptToRun);

		String[] commands = {scriptToRun};
		try {
			Process p = Runtime.getRuntime().exec(commands);
			// Save a reference to the process just started, so you can kill it later.
			cleanDeadSpaceProcesses();
			_spaceProcesses.add(p);
			System.out.println("[HwComputer.startApplicationSpace] pid: "
				+ p.toString() + ", isAlive: "
				+ p.isAlive() + ", \ntotal processes: " + _spaceProcesses.size());
		} catch (IOException/*|InterruptedException*/ e) {
			e.printStackTrace();
		}
	}


	@Override
	public void startApplicationSpaceAndComputers(Utils.Hw_Request hwRequest) throws RemoteException {




		if(hwRequest != null && hwRequest.size != Utils.Hw_Computer_Size.MAX_DO_NOT_USE) {
			System.out.println("Starting application with " + hwRequest.numHwComputers + " " + hwRequest.size + "computers." );

			int availComputers = 0;
			List<HwComputerInterface> computers = _computer_lists.get(hwRequest.size);
			if(computers !=null ){
				availComputers = computers.size();
				System.out.println("Computers available: " + availComputers);
			}

			if(availComputers <=0 || availComputers < hwRequest.numHwComputers){
				System.out.println("[HwMgr.startSpace] Not enough hardware computers available. Requested: " + hwRequest.numHwComputers + ", Avail: " + availComputers);
				return;
			}


			startApplicationSpaceOnHwManager();

			try {
				Thread.sleep(3000);//let the space start
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}

			for(int i = 0; i< hwRequest.numHwComputers; i++){

				HwComputerInterface computer = computers.get(i);
				if(computer!=null) {
					System.out.println("Starting application computer." );

					//only start one 290B computer instance per hardware instance
					//let 290B multiprocessor decide how many computers to spawn
					//we're only comparing cost/time of aws machines, not num threads etc
					computer.startLogicalComputers(1);
				}
			}
		}
	}




	// Start an Amazon instance. Need to correlate this image id later with the computer object
	// when the hwComputer registers.
/*	private static void launchAWSInstance()
	{
		//use startHArdwareComputer() instead
	}
*/
	// TERMINATE all instances
	private static void terminateAllAWSInstances(   ) {

		System.out.println("[terminateAllInstances]");
		if (_instances.size() > 0) {

			List<String> instIds = new ArrayList<String>();
			for (Instance i : _instances) {
				System.out.println("Deleting..." + i.getInstanceId() + " (" + i.getImageId() + ")");
				instIds.add(i.getInstanceId());
			}
			TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest(instIds);
			TerminateInstancesResult terminateInstancesResult = ec2.terminateInstances(terminateInstancesRequest);

			System.out.println("[terminateAllInstances] termination results: " + terminateInstancesResult.toString());
		}
	}

	// Keeps the HwManager running indefinitely
	private class Scheduler implements Runnable{

		@Override
		public void run(){
			while (true){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("[HwManager.Scheduler] Thread");
			}
		}
	}

	// A means for HwComputers to check if the HwManager is still running.
	@Override
	public String computerRequestsHeartbeatOfHwManager() throws RemoteException {
		return "HwManager is still alive.";
	}







	// MAIN
    public static void main(String[] args) throws Exception
	{

		System.out.println("[main] Starting Hardware Manager");
        System.out.println("===========================================");
        System.out.println("Welcome to DIstributed jaVA (DIVA)");
        System.out.println("===========================================");
		//System.out.println("[main] args: " + args.length);
		//Arrays.asList(args).forEach(System.out::println);

        initAWS();



		// RMI registry
		System.setSecurityManager(new SecurityManager());
		HwManager hwManager = new HwManager();
		LocateRegistry.createRegistry(_port).rebind(_serviceName, hwManager);

		System.out.println("[main] Hardware Manager started. Service: "
			+ _serviceName + " available on port: " + _port + ".");


		while(true){

		}


    }
}
