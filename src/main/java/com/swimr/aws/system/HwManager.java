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
import com.swimr.aws.rmi.*;


public class HwManager extends UnicastRemoteObject implements HwManagerInterface {

	//static final int MAX_EC2_INSTANCES_AT_A_TIME = 5;
	static HwManager _thisHwManager;

    static AmazonEC2 ec2;
    //static AmazonS3  s3;


	static final InstanceType _type = InstanceType.T2Micro;
	static final String _keyName = "290b-java";
	static final String _securityGroup = "RMI";



	// Make all these thread safe?
	static List<Instance> _instances = new ArrayList<Instance>();
	//static List<HwComputerInterface> _awsComputers = new ArrayList<>();
	static List<Process> _spaceProcesses = new ArrayList<>();

	//list of all computers
	static Map<String, HwComputerProxy> _all_computers = Collections.synchronizedMap(new HashMap<>());
	//separate list of computers to query by size
	static Map<Utils.Hw_Computer_Size, Map<String, HwComputerInterface>> _computer_lists_by_size = Collections.synchronizedMap(new HashMap<Utils.Hw_Computer_Size, Map<String,HwComputerInterface>>());



	public HwManager() throws RemoteException {
		super(_port);

		_computer_lists_by_size.put(Utils.Hw_Computer_Size.micro, new HashMap<String, HwComputerInterface>());
		_computer_lists_by_size.put(Utils.Hw_Computer_Size.large, new HashMap<String, HwComputerInterface>());
		_computer_lists_by_size.put(Utils.Hw_Computer_Size.two_xl, new HashMap<String, HwComputerInterface>());
		_computer_lists_by_size.put(Utils.Hw_Computer_Size.unknown, new HashMap<String, HwComputerInterface>());

		_thisHwManager = this;
	}



	// ********** Interface Methods ***********************

	@Override
	public void registerComputer(ComputerRegistration computerReg) throws RemoteException {

		System.out.println("[HwManager.registerComputer]");
		System.out.println("[HwManager.registerComputer] Registering: " + computerReg.id + ", size: " + computerReg.size);



		//Add to master computer list
		if(_all_computers.containsKey(computerReg.id)){
			_all_computers.remove(computerReg.id);
		}
		HwComputerProxy proxy = new HwComputerProxy(computerReg.id, computerReg.hwComputerInterface);
		_all_computers.put(computerReg.id, proxy);


		//Add to the size list
		Map<String, HwComputerInterface> computersOfSize = _computer_lists_by_size.get(Utils.Hw_Computer_Size.micro);
		if(computersOfSize==null){

			System.out.println("[HwManager.registerComputer] Can't register computer. List is null.");
			return;
		}
		// if run from localhost the ID will be "unknown" and only one computer will ever register, FYI
		if(computersOfSize.containsKey(computerReg.id))
			computersOfSize.remove(computerReg.id);
		computersOfSize.put(computerReg.id, computerReg.hwComputerInterface);
		System.out.println("[HwManager.registerComputer] Registered: " + computerReg.id + ", size: " + computerReg.size);


		// Start proxy heartbeat check
		Thread thread = new Thread(proxy);
		thread.start();

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
/*
		for(int i= _computer_lists_by_size.get(Utils.Hw_Computer_Size.micro).size()-1; i>=0; i--){
			HwComputerInterface c = _computer_lists_by_size.get(Utils.Hw_Computer_Size.micro).get(i);
			if(c == null)
				_computer_lists_by_size.get(Utils.Hw_Computer_Size.micro).remove(c);
		}
		for(int i= _computer_lists_by_size.get(Utils.Hw_Computer_Size.large).size()-1; i>=0; i--){
			HwComputerInterface c = _computer_lists_by_size.get(Utils.Hw_Computer_Size.large).get(i);
			if(c == null)
				_computer_lists_by_size.get(Utils.Hw_Computer_Size.large).remove(c);
		}
		for(int i= _computer_lists_by_size.get(Utils.Hw_Computer_Size.two_xl).size()-1; i>=0; i--){
			HwComputerInterface c = _computer_lists_by_size.get(Utils.Hw_Computer_Size.two_xl).get(i);
			if(c == null)
				_computer_lists_by_size.get(Utils.Hw_Computer_Size.two_xl).remove(c);
		}
		for(int i= _computer_lists_by_size.get(Utils.Hw_Computer_Size.unknown).size()-1; i>=0; i--){
			HwComputerInterface c = _computer_lists_by_size.get(Utils.Hw_Computer_Size.unknown).get(i);
			if(c == null)
				_computer_lists_by_size.get(Utils.Hw_Computer_Size.unknown).remove(c);
		}
		*/
	}


	// * User calls this. Returns the object containing all the hardware and logical computers.
	public
	StatusTransportObject getSystemStatus() throws RemoteException
	{
		System.out.println("[HwManager.getSystemStatus] Computers: \n"
			+ _computer_lists_by_size.get(Utils.Hw_Computer_Size.micro).size() + " t2.micro\n"
			+ _computer_lists_by_size.get(Utils.Hw_Computer_Size.large).size() + " m3.large\n"
			+ _computer_lists_by_size.get(Utils.Hw_Computer_Size.two_xl).size() + " c4.2xlarge\n"

			+ "Getting logical compute processes...");

		//Load this object with all the status info to pass back to the HwUser.
		StatusTransportObject transportObj = new StatusTransportObject(_computer_lists_by_size);

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

		//for(HwComputerInterface computer: (_computer_lists_by_size.get(Utils.Hw_Computer_Size.micro))) {

		//transportObj._awsInstances.add(computer);


		List<String> processList;
		try {
			for (Map.Entry<String, HwComputerInterface> entry : _computer_lists_by_size.get(Utils.Hw_Computer_Size.micro).entrySet()) {
				transportObj._logicalComputerProcesses.addAll(entry.getValue().getRunningProcessStrings());
			}
			for (Map.Entry<String, HwComputerInterface> entry : _computer_lists_by_size.get(Utils.Hw_Computer_Size.large).entrySet()) {
				transportObj._logicalComputerProcesses.addAll(entry.getValue().getRunningProcessStrings());
			}
			for (Map.Entry<String, HwComputerInterface> entry : _computer_lists_by_size.get(Utils.Hw_Computer_Size.two_xl).entrySet()) {
				transportObj._logicalComputerProcesses.addAll(entry.getValue().getRunningProcessStrings());
			}
			for (Map.Entry<String, HwComputerInterface> entry : _computer_lists_by_size.get(Utils.Hw_Computer_Size.unknown).entrySet()) {
				transportObj._logicalComputerProcesses.addAll(entry.getValue().getRunningProcessStrings());
			}
		}
		//Catch HwComputer remote exception at the manager, instead of passing it back to the computer as would happen if this wasn't here.
		catch(RemoteException e){
			System.out.println("[HwManager.getSystemStatus] Connection attempt to computer failed while getting processes.");
		}


		System.out.println("[HwManager.getSystemStatus] Status snapshot complete. Returning to user.");

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
				if(i.getImageId().equals(Utils.HW_COMPUTER_AMI))
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
		System.out.println("Launching instance of ami: " + Utils.HW_COMPUTER_AMI + " and size: " + _type);

		RunInstancesRequest runRqst = new RunInstancesRequest();

		// Put startup script in here
		// https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html
		//
		//https://help.ubuntu.com/community/CloudInit

		String startupUserData =  "IyEvYmluL2Jhc2ggCmNkIC9ob21lL3VidW50dS8yOTBiL0F3c0luc3RhbmNlTWFuYWdlcjsgCm12biB0ZXN0IC1QY29tcHV0ZXI=";

		//String startupUserData = "#!/bin/bash cd /home/ubuntu/290b/AwsInstanceManager; mvn test -Pcomputer";
		//startupUserData = com.amazonaws.util.Base64.encodeAsString(startupUserData.getBytes());

		runRqst.withImageId(Utils.HW_COMPUTER_AMI)
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
			Thread.sleep(500);
		} catch (InterruptedException e) {
			//
		}
	}


	@Override
	public void terminateApplicationSpaceAndComputers() throws RemoteException {
		// Kill space process
		killAllSpaceProcesses();

		// kill sw_computer processes

		for(Map.Entry<String, HwComputerInterface> entry: _computer_lists_by_size.get(Utils.Hw_Computer_Size.micro).entrySet()){
			entry.getValue().terminateSwComputers();
		}
		for(Map.Entry<String, HwComputerInterface> entry: _computer_lists_by_size.get(Utils.Hw_Computer_Size.large).entrySet()){
			entry.getValue().terminateSwComputers();
		}
		for(Map.Entry<String, HwComputerInterface> entry: _computer_lists_by_size.get(Utils.Hw_Computer_Size.two_xl).entrySet()){
			entry.getValue().terminateSwComputers();
		}
		for(Map.Entry<String, HwComputerInterface> entry: _computer_lists_by_size.get(Utils.Hw_Computer_Size.unknown).entrySet()){
			entry.getValue().terminateSwComputers();
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
			Map<String,HwComputerInterface> computers = _computer_lists_by_size.get(hwRequest.size);
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

			System.out.println("[startApplicationSpaceAndComputers] Calling hw_computers to start sw_computers...");


			int i=0;
			for(Map.Entry<String, HwComputerInterface> entry:computers.entrySet()){

				HwComputerInterface c = entry.getValue();
				if(i<hwRequest.numHwComputers && c!=null){
					System.out.println("Starting application computer." );
					c.startLogicalComputers(1);

				}
				else {
					break;
				}
				i++;

			}
			System.out.println("[startApplicationSpaceAndComputers] Done calling hw_computers to start sw_computers...");
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


	//synchronized
	void removeComputerFromLists(String id){
		if(id==null)
			return;
		_all_computers.remove(id);
		_computer_lists_by_size.get(Utils.Hw_Computer_Size.micro).remove(id);
		_computer_lists_by_size.get(Utils.Hw_Computer_Size.large).remove(id);
		_computer_lists_by_size.get(Utils.Hw_Computer_Size.two_xl).remove(id);
		_computer_lists_by_size.get(Utils.Hw_Computer_Size.unknown).remove(id);


	}









	// MAIN
    public static void main(String[] args) throws Exception
	{

		System.out.println("[main] Starting Hardware Manager");
        //System.out.println("===========================================");
        //System.out.println("Welcome to DIstributed jaVA (DIVA)");
        //System.out.println("===========================================");
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

	// The only purpose of this is to check if the computer is running. If it's not, remove it.
	class HwComputerProxy implements Runnable{

		public String _awsId = "unknown";
		public HwComputerInterface _hwComputerInterface = null;

		public HwComputerProxy(String awsId, HwComputerInterface hwComputerInterface){
			_awsId = awsId;
			_hwComputerInterface = hwComputerInterface;
		}


		@Override
		public void run() {

			int fail_count = 0;
			while(true){

				if(_hwComputerInterface!=null) {
					try {
						System.out.println("Heartbeat from computer " + _awsId + ":  " + _hwComputerInterface.isAlive());
					} catch (RemoteException e) {

						e.printStackTrace();


						fail_count++;
						System.out.println("Computer " + _awsId + " failed. " + fail_count);
						if(fail_count > 5) {
							// remove hwcomputer
							System.out.println("Computer " + _awsId + " failed. Removing.");
							HwManager._thisHwManager.removeComputerFromLists(_awsId);
							return;
						}


					}
				}
				// should remove if it's null also

				try {
					Thread.sleep(5000);
				}
				catch (InterruptedException e) { /* for thread sleep, nothing */}
			}
		}
	}
}
