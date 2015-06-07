package com.swimr.aws.rmi;


public class Utils {

	public static final int MAX_EC2_INSTANCES_AT_A_TIME = 8;

	public static  enum Hw_Computer_Size {
		micro,
		large,
		two_xl,
		MAX_DO_NOT_USE
	}

	public static class Hw_Request{
		public Hw_Computer_Size size = Hw_Computer_Size.micro;
		public int numHwComputers = 1;
	}

	public static class TspTestRequest{
		public Hw_Computer_Size size = Hw_Computer_Size.micro;
		public int numHwComputers = 1;
		public int numCities = 10;
	}



/*
	static Hw_Computer_Size convertSizeString(String awsSizeString){

//		if(awsSizeString.equals())

	}

*/

}
