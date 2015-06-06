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
/*import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
*/
import com.swimr.aws.rmi.HwComputerInterface;
import com.swimr.aws.rmi.HwManagerInterface;
import com.swimr.aws.rmi.StatusTransportObject;

import javax.print.DocFlavor;


public class HwManager extends UnicastRemoteObject implements HwManagerInterface {

	static final int MAX_EC2_INSTANCES_AT_A_TIME = 5;

    static AmazonEC2 ec2;
    //static AmazonS3  s3;

	//HW_COMPUTER AMI
	static final String _ami = "ami-113d0221";	//ami-85467ab5
	static final InstanceType _type = InstanceType.T2Micro;
	static final String _keyName = "290b-java";
	static final String _securityGroup = "RMI";
	static List<Instance> _instances = new ArrayList<Instance>();
	static List<HwComputerInterface> _awsComputers = new ArrayList<>();

	static List<Process> _spaceProcesses = new ArrayList<>();

	public HwManager() throws RemoteException {
		super(_port);
	}



	// ********** Interface Methods ***********************



	// * A new computer calls this when it starts.
	public
	void registerComputer(HwComputerInterface hwComputer) throws RemoteException {

		System.out.println("[HwManager.registerAsComputer]");

		if(!_awsComputers.contains(hwComputer))
			_awsComputers.add(hwComputer);

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
		for(int i=_awsComputers.size()-1; i>=0; i--)
		{
			HwComputerInterface computerInterface = _awsComputers.get(i);
			if(computerInterface == null){
				_awsComputers.remove(computerInterface);
			}
		}
	}


	// * User calls this. Returns the object containing all the hardware and logical computers.
	public
	StatusTransportObject getSystemStatus() throws RemoteException
	{
		System.out.println("[HwManager.getSystemStatus] connected computers: " + _awsComputers.size()
			+ ", getting logical compute processes...");

		StatusTransportObject transportObj = new StatusTransportObject();


		// Space processes
		cleanDeadComputerInstances();
		cleanDeadSpaceProcesses();

		System.out.println("[HwManager.getSystemStatus] " + _spaceProcesses.size() + " space processes.");
		for(Process process:_spaceProcesses){
			transportObj._logicalSpaceProcessesOnHwManager.add(process.toString());
		}


		// AWS computers and logical computer processes on them
		for(HwComputerInterface computer: _awsComputers) {
			transportObj._awsInstances.add(computer);

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
		}




		return transportObj;
	}






	// * Greeting to user/client, confirming connection.
	@Override
	public String userJustCheckingIn() {
		return "hello from manager!";
	}






	// ************************* APPLICATION helper functions **************************************************




	// Start an application SPACE on this instance. Save the PID so it can be killed, etc.
	@Override
	public void startApplicationSpaceOnHwManager() // throws RemoteException
	{

		String scriptToRun = "scripts/sw_space_startup.sh";
		System.out.println("[HwManager.startLogicalComputeSpace] Exec: " + scriptToRun);

		String[] commands = {scriptToRun};
		try {
			Process p = Runtime.getRuntime().exec(commands);
			// Save a reference to the process just started, so you can kill it later.
			cleanDeadSpaceProcesses();
			_spaceProcesses.add(p);
			System.out.println("[HwComputer.startLogicalComputers] pid: "
				+ p.toString() + ", isAlive: "
				+ p.isAlive() + ", \ntotal processes: " + _spaceProcesses.size());
		} catch (IOException/*|InterruptedException*/ e) {
			e.printStackTrace();
		}
	}





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
				if(i.getImageId().equals(_ami))
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

	@Override
	public void spaceRequestsLogicalComputers(int requestedCores) {

		// TO DO: CONVERT CORES TO INSTANCES. IMPLIES KNOWLEDGE OF NUMBER OF CORES ON
		// THE EC2 INSTANCE THAT WILL START.


		if(_awsComputers.size() >= MAX_EC2_INSTANCES_AT_A_TIME)
		{
			System.out.println("[HwManager.spaceRequestsLogicalComputers] Max EC2 instances reached. ");
			return;
		}


		launchAWSInstance();


		//right now this will not work.

		// Instances are not registering on startup yet.


	}

	// Start an Amazon instance. Need to correlate this image id later with the computer object
	// when the hwComputer registers.
	private static void launchAWSInstance()
	{

		// https://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/run-instance.html
		System.out.println("Launching instance of ami: " + _ami + " and size: " + _type);

		RunInstancesRequest runRqst = new RunInstancesRequest();








		// Put startup script in here somehow
		// https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html
		//
		String startupUserData = "";










		runRqst.withImageId(_ami)
			.withInstanceType(_type)
			.withMinCount(1)
			.withMaxCount(2)
			.withKeyName(_keyName)
			.withSecurityGroups(_securityGroup)
			.withUserData(startupUserData);


		if(ec2!=null)
		{
			RunInstancesResult result = ec2.runInstances(runRqst);
			//	DryRunResult<RunInstancesResult> dryRunResult = ec2.dryRun(DryRunSupportedRequest<RunInstancesRequest>)
			System.out.println("Run Result: " + result.toString());

			String id = result.getReservation().getInstances().get(0).getImageId();
		}
		else{
			try {
				initAWS();
			} catch (Exception e) {

				e.printStackTrace();
			}
		}
	}

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
