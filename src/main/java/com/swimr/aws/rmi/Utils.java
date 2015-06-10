package com.swimr.aws.rmi;


import com.amazonaws.services.ec2.model.InstanceType;

import java.io.Serializable;

public class Utils {


	public static final int MAX_EC2_INSTANCES_AT_A_TIME = 8;


	public static final String HW_COMPUTER_AMI = "ami-3bbd850b"; // 12 cities
	//public static final String HW_COMPUTER_AMI = "ami-dbb880eb"; // 10 cities
	public static final String HW_MANAGER_AMI = "ami-8fc6febf";		// 12 cities
	public static final InstanceType AWS_HW_MANAGER_DEFAULT_SIZE = InstanceType.M3Large;	//m3.large
	public static final String KEY_NAME = "290b-java";
	public static final String SECURITY_GROUP = "RMI";


	public static  enum Hw_Computer_Size {
		micro,		//t2.micro
		large,		//m3.large
		two_xl,		//c4.2xlarge
		unknown,
		MAX_DO_NOT_USE
	}

	public static class Hw_Request implements Serializable{
		public Hw_Computer_Size size = Hw_Computer_Size.micro;
		public int numHwComputers = 1;
	}



	public static Hw_Computer_Size convertSizeString(String awsSizeString){

		Hw_Computer_Size size = Hw_Computer_Size.unknown;

		if(awsSizeString!=null) {
			if (awsSizeString.equals("t2.micro"))
				size = Hw_Computer_Size.micro;
			else if (awsSizeString.equals("m3.large"))
				size = Hw_Computer_Size.large;
			else if (awsSizeString.equals("c4.2xlarge"))
				size = Hw_Computer_Size.two_xl;
		}
		return size;
	}



}



