package com.swimr.aws.rmi;

import java.io.Serializable;

public class ComputerRegistration implements Serializable {

	public HwComputerInterface hwComputerInterface;

	public String id = new String("none");
	public Utils.Hw_Computer_Size size = Utils.Hw_Computer_Size.micro;


}
