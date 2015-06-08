package com.swimr.aws.system;

import com.amazonaws.util.EC2MetadataUtils;
import com.swimr.aws.rmi.*; // rmi.HwComputerInterface;
// import rmi.HwManagerInterface;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class HwComputer extends UnicastRemoteObject implements Runnable, HwComputerInterface {


	List<Process> _processes = new ArrayList<>();
	String _amazonInstanceId = new String("unknown");
	Utils.Hw_Computer_Size _amazonInstanceType = Utils.Hw_Computer_Size.micro;

	public HwManagerInterface hwManagerStub = null;
	private HwComputerInterface _hwComputerInterface = null;

	String _hostname = "";




	public HwComputer(String hostname) throws RemoteException{

		_hostname = hostname;
		setMyAmazonInstanceId();
		setMyAmazonInstanceSize();
		_hwComputerInterface = this;
	}


	public String getAwsInstanceId() throws RemoteException{
		return _amazonInstanceId;
	}

	public Utils.Hw_Computer_Size getEc2Size(){
		return _amazonInstanceType;
	}


	// @Override
	public boolean startLogicalComputers(int numComputers) {
		System.out.println("[HwComputer.startLogicalComputers] Start " + numComputers);// + " computers w/ space URL: " + spaceURL);

		String computerStartCommand = "scripts/sw_computer_startup.sh";
		String[] commands = {computerStartCommand};
		try {

			Process p = Runtime.getRuntime().exec(commands);
			System.out.println("[HwComputer.startLogicalComputers] pid: " + p.toString() + ", isAlive: " + p.isAlive());

			// Save a reference to the process just started, so you can kill it later.
			_processes.add(p);
			return true;

		} catch (IOException/*|InterruptedException*/ e) {
			e.printStackTrace();
		}

		return false;
	}



	// java.lang.Process is not serializable
	@Override
	public List<String> getRunningProcessStrings(){

		List<String> processStrings = new ArrayList<>();
		if(_processes!=null){
			for(Process process:_processes)
				processStrings.add(process.toString());
		}
		return processStrings;
	}


	private boolean setMyAmazonInstanceId(){

		String result = EC2MetadataUtils.getInstanceId();
		if(result!=null)
			_amazonInstanceId = result;

		System.out.println("[HwComputer.setMyAmazonInstanceId] id: " + _amazonInstanceId);

		if(_amazonInstanceId != null)
			return true;
		else
			return false;
	}

	private void setMyAmazonInstanceSize(){

		_amazonInstanceType = Utils.convertSizeString(EC2MetadataUtils.getInstanceType());

		 System.out.println("[HwComputer.setMyAmazonInstanceSize] " + _amazonInstanceType);



	}


	// Runnable
	@Override
	public void run() {


		Reconnector reconnector = new Reconnector();
		Thread thread = new Thread(reconnector);
		thread.start();




	}


	class Reconnector implements Runnable{

		@Override
		public void run() {
			String hwRegistryUrl = "//" + _hostname + ":" + HwManager._port + "/" + HwManager._serviceName;


			while(true) {

				System.out.println("[HwComputer.main] Attempting: " + hwRegistryUrl);
				try {
					hwManagerStub = ((HwManagerInterface) Naming.lookup(hwRegistryUrl));

					System.out.println("");


					// Register this computer with the Manager
					if(hwManagerStub!=null) {
						//System.out.println("hwManager: " + hwManager);
						ComputerRegistration c = new ComputerRegistration();

						System.out.println("instance id: " + _amazonInstanceId);

						c.id = _amazonInstanceId;
						c.hwComputerInterface = _hwComputerInterface; //
						c.size = _amazonInstanceType;

						hwManagerStub.registerComputer(c);

					}
					else
						continue;


					System.out.println("[HwComputer.main] Connected.");
					// Stop trying to connect if success.
					// break;
				} catch (RemoteException | NotBoundException | MalformedURLException e) {
					System.out.println("[HwComputer.main] Connect failed. Trying...");
				}

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					System.out.println("[HwComputer.main] Connect failed. Trying again.");
				}



				//while(true){
					//stay connected

					//Do heartbeat. If HwManager doesn't respond, break, start trying to connect again.

					// Sleep for a while, then check for a heartbeat.
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						//do nothing
					}

					// break if no hwmanager at all; go back up to reconnect.
					if(hwManagerStub==null){
						System.out.println("[HwComputer.main] hwManagerStub is null. Can't get heartbeat.");
						break;
					}

					// try for a heartbeat if there is a hwmanager. if that fails, break and reconnect
					try {
						String heartbeat = hwManagerStub.computerRequestsHeartbeatOfHwManager();
						if(heartbeat!=null)
							System.out.println("[HwComputer.main] Got heartbeat from HwManager: " + heartbeat);

					} catch (RemoteException e) {

						System.out.println("[HwComputer.main] No heartbeat, retrying connect.");
						break;	//break out of this while, go back into the connect attempt loop
						//					e.printStackTrace();
					}
				//}
			//}
		}
	}





	/**
	 * First parameter should be URL to the hardware manager.
	 * "//domain:port/space_name"
	 */
	public static void main(String[] args){


		HwManagerInterface hwManagerStub = null;


		if(args.length == 0) {
			System.out.println("[HwComputer.main] First argument should be domain to hardware manager registry.");
			return;
		}
		System.out.println("[HwComputer.main] arg: " + args[0]);



		// Create the computer to be registered below.
		HwComputer hwComputer = null;
		try {
			hwComputer = new HwComputer(args[0]);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		hwComputer.run();

	}
}
