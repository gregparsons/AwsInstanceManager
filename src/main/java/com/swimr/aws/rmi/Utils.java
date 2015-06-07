package com.swimr.aws.rmi;



public class Utils {

	public static final int MAX_EC2_INSTANCES_AT_A_TIME = 8;

	public static  enum Hw_Computer_Size {
		micro,		//t2.micro
		large,		//m3.large
		two_xl,		//c4.2xlarge
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




	public static Hw_Computer_Size convertSizeString(String awsSizeString){
		Hw_Computer_Size size = Hw_Computer_Size.MAX_DO_NOT_USE;
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