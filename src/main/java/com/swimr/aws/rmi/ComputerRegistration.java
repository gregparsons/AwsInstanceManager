package com.swimr.aws.rmi;

import java.io.Serializable;

/**
 * Wrapper class for the HwComputer to pass itself and its AWS ID to the HwManager.
 */
public class ComputerRegistration implements Serializable {

	public HwComputerInterface hwComputerInterface;
	public String id = new String("none");
	public Utils.Hw_Computer_Size size = Utils.Hw_Computer_Size.micro;

}
