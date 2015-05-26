package system;

// To Run:
//  mvn package exec:java

import java.io.File;
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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import rmi.HwComputerInterface;
import rmi.HwManagerInterface;


public class HwManager extends UnicastRemoteObject implements HwManagerInterface {

    static AmazonEC2 ec2;
    static AmazonS3  s3;
	static final String _ami = "ami-85467ab5";	//ami-85467ab5
	static final InstanceType _type = InstanceType.T2Micro;
	static final String _keyName = "290b-java";
	static final String _securityGroup = "RMI";
	static List<Instance> _instances = new ArrayList<Instance>();
	static List<HwComputerInterface> _computers = new ArrayList<>();

	public HwManager() throws RemoteException {
		super(_port);
	}



	// ********** Interface Methods ***********************



	// * A new computer calls this when it starts.
	public void registerComputer(HwComputerInterface hwComputer) throws RemoteException {

		System.out.println("[HwManager.registerAsComputer]");

		if(!_computers.contains(hwComputer))
			_computers.add(hwComputer);

	}


	// * User calls this.
	public List<Instance> getRunningInstances() throws RemoteException {
		System.out.println("[HwManager.getRunningInstances] connected computers: " + _computers.size());
		for(HwComputerInterface computer: _computers) {
			computer.startLogicalComputers(1, "hello");
		}
		return null;
	}




	// * Greeting to user/client, confirming connection.
	@Override
	public String userJustCheckingIn() {
		return "hello from manager!";
	}




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
        s3  = new AmazonS3Client(credentialsProvider);


        try
		{
			 //* Set region.


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





	// launchInstance
	private static void launchAWSInstance()
	{

		// https://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/run-instance.html
		System.out.println("Launching instance of ami: " + _ami + " and size: " + _type);

		RunInstancesRequest runRqst = new RunInstancesRequest();

		runRqst.withImageId(_ami)
			.withInstanceType(_type)
			.withMinCount(1)
			.withMaxCount(2)
			.withKeyName(_keyName)
			.withSecurityGroups(_securityGroup);

		if(ec2!=null)
		{
			RunInstancesResult result = ec2.runInstances(runRqst);
			//	DryRunResult<RunInstancesResult> dryRunResult = ec2.dryRun(DryRunSupportedRequest<RunInstancesRequest>)
			System.out.println("Run Result: " + result.toString());
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







	// MAIN
    public static void main(String[] args) throws Exception
	{

        System.out.println("===========================================");
        System.out.println("Welcome to the AWS Java SDK!");
        System.out.println("===========================================");
		System.out.println("[main] args: " + args.length);
		Arrays.asList(args).forEach(System.out::println);

        // initAWS();



		// RMI registry
		System.setSecurityManager(new SecurityManager());
		HwManager hwManager = new HwManager();
		LocateRegistry.createRegistry(_port).rebind(_serviceName, hwManager);


		while(true){

		}


    }
}
