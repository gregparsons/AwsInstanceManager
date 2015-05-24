/*
 * Copyright 2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */



// To Run:
//  mvn package exec:java


import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfilesConfigFile;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
/*
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;
*/
/**
 * This class is a starting point for working with the AWS SDK for Java, and
 * shows how to make a few simple requests to Amazon EC2 and Amazon S3.
 *
 * Before you run this code, be sure to fill in your AWS security credentials
 * in the  .aws/credentials file under your home directory.
 *
 * If you don't have an Amazon Web Services account, you can get started for free:
 *   http://aws.amazon.com/free
 *
 * For lots more information on using the AWS SDK for Java, including information on
 * high-level APIs and advanced features, check out the AWS SDK for Java Developer Guide:
 *   http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html
 *
 * Stay up to date with new features in the AWS SDK for Java by following the
 * AWS Java Developer Blog:
 *   https://java.awsblog.com
 */
public class AwsInstanceManager {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            .aws/credentials file under your home directory 
     *             before you run this sample.
     * http://aws.amazon.com/security-credentials
     */
    static AmazonEC2 ec2;
    static AmazonS3  s3;

	static final String _ami = "ami-85467ab5";	//ami-85467ab5
	static final InstanceType _type = InstanceType.T2Micro;
	static final String _keyName = "290b-java";
	static final String _securityGroup = "RMI";



	static List<Instance> _instances = new ArrayList<Instance>();




    /**
	 *
	 * Init()
	 *
	 *
     * The only information needed to create a client are security credentials -
     * your AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints have defaults provided.
     *
     * Additional client parameters, such as proxy configuration, can be specified
     * in an optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.PropertiesCredentials
     * @see com.amazonaws.ClientConfiguration
     */
    private static void init() throws Exception
	{
        /*
         * ProfileCredentialsProvider loads AWS security credentials from a
         * .aws/config file in your home directory.
         *
         * These same credentials are used when working with other AWS SDKs and the AWS CLI.
         *
         * You can find more information on the AWS profiles config file here:
         * http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html
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

    }


	/**
	 *
	 * Start a single EC2 instance.
	 *
	 */
	private static void launchInstance()
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


	private static void terminateAllInstances(   )
	{

		System.out.println("[terminateAllInstances]");

	/*
		TerminateInstancesRequest terminateInstancesRequest;
		TerminateInstancesResult terminateInstancesResult = ec2.terminateInstances(terminateInstancesRequest);
*/

		if(_instances.size() > 0) {


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




    public static void main(String[] args) throws Exception
	{

        System.out.println("===========================================");
        System.out.println("Welcome to the AWS Java SDK!");
        System.out.println("===========================================");

        init();

        try
		{

            /**
			 * Set region.
             */

			Region usWest2 = Region.getRegion(Regions.US_WEST_2);
            ec2.setRegion(usWest2);

			/**
			 *
			 * Zones within this region.
			 *
			 */

            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            List<AvailabilityZone> availabilityZones = availabilityZonesResult.getAvailabilityZones();
            System.out.println("You have access to " + availabilityZones.size() + " availability zones:");
            for (AvailabilityZone zone : availabilityZones)
			{
                System.out.println(" - " + zone.getZoneName() + " (" + zone.getRegionName() + ")");
            }


			/**
			 *
			 * Instances running in this Region.
			 *
			 */
            DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
            Set<Instance> instances = new HashSet<Instance>();
            for (Reservation reservation : describeInstancesResult.getReservations())
			{

				List<Instance> reservationInstances = reservation.getInstances();
//                instances.addAll(reservation.getInstances());
				instances.addAll(reservationInstances);
            }




			/**
			 *
			 * Print the IP addresses of currently running instances.
			 *
			 */
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


			// launch at most 2 instances ... where ami ==
/*			if(runningInstances < 2)
			{
				launchInstance();
			}

*/

			terminateAllInstances();




            /*
             * The Amazon S3 client allows you to manage and configure buckets
             * and to upload and download data.
             *
             * In this sample, we use the S3 client to list all the buckets in
             * your account, and then iterate over the object metadata for all
             * objects in one bucket to calculate the total object count and
             * space usage for that one bucket. Note that this sample only
             * retrieves the object's metadata and doesn't actually download the
             * object's content.
             *
             * In addition to the low-level Amazon S3 client in the SDK, there
             * is also a high-level TransferManager API that provides
             * asynchronous management of uploads and downloads with an easy to
             * use API:
             *   http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/transfer/TransferManager.html
             */
			/*
            List<Bucket> buckets = s3.listBuckets();
            System.out.println("You have " + buckets.size() + " Amazon S3 bucket(s).");

            if (buckets.size() > 0) {
                Bucket bucket = buckets.get(0);

                long totalSize  = 0;
                long totalItems = 0;

				// Iterate over buckets
                for (S3ObjectSummary objectSummary : S3Objects.inBucket(s3, bucket.getName())) {
                    totalSize += objectSummary.getSize();
                    totalItems++;
                }

                System.out.println("The bucket '" + bucket.getName() + "' contains "+ totalItems + " objects "
                        + "with a total size of " + totalSize + " bytes.");

            }
            */
        }
		catch (AmazonServiceException ase)
		{
            /*
             * AmazonServiceExceptions represent an error response from an AWS
             * services, i.e. your request made it to AWS, but the AWS service
             * either found it invalid or encountered an error trying to execute
             * it.
             */
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        }
		catch (AmazonClientException ace)
		{
            /*
             * AmazonClientExceptions represent an error that occurred inside
             * the client on the local host, either while trying to send the
             * request to AWS or interpret the response. For example, if no
             * network connection is available, the client won't be able to
             * connect to AWS to execute a request and will throw an
             * AmazonClientException.
             */
            System.out.println("Error Message: " + ace.getMessage());
        }
    }
}
