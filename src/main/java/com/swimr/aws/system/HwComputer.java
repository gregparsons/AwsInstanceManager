package system;

import rmi.HwComputerInterface;
import rmi.HwManagerInterface;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class HwComputer extends UnicastRemoteObject implements HwComputerInterface, Serializable {



	public HwComputer() throws RemoteException{
	}

	@Override
	public boolean startLogicalComputers(int numComputers, String spaceURL) {

		System.out.println("[HwComputer.startLogicalComputers] Start " + numComputers + " computers w/ space URL: " + spaceURL);
/*


/usr/bin/java -Djava.security.policy=/Users/aaa/temp/policy -cp /Users/aaa/temp/h4.jar system.ComputerImpl localhost multi

 */
		String computerStartCommand = "/Users/aaa/290a/aws/aws-test1/aws-test1/computer.sh"; //"/System/Library/Frameworks/JavaVM.framework/Versions/A/Commands/java -Djava.security.policy=/Users/aaa/temp/policy -cp '/Users/aaa/temp/h4.jar' system.ComputerImpl localhost multi";

		String[] commands = {computerStartCommand};

		try {

			Process p = Runtime.getRuntime().exec(commands);
			System.out.println("[HwComputer.startLogicalComputers] pid: " + p.toString() + ", isAlive: " + p.isAlive());

		} catch (IOException/*|InterruptedException*/ e) {
			e.printStackTrace();
		}




		return false;
	}


	/**
	 * First parameter should be URL to the hardware manager.
	 * "//domain:port/space_name"
	 */
	public static void main(String[] args){

		if(args.length == 0) {
			System.out.println("[HwComputer.main] First argument should be domain to hardware manager registry.");
			return;
		}
		System.out.println("[HwComputer.main] arg: " + args[0]);
		String hwRegistryUrl = "//" + args[0] + ":" + HwManager._port + "/" + HwManager._serviceName;



		// Create the computer to be registered below.
		HwComputer hwComputer = null;
		try {
			hwComputer = new HwComputer();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		while(true) {

			System.out.println("[HwComputer.main] Attempting: " + hwRegistryUrl);
			try {
				HwManagerInterface hwManagerStub = ((HwManagerInterface) Naming.lookup(hwRegistryUrl));

				System.out.println("");


				// Register this computer with the Manager
				if(hwManagerStub!=null) {
					//System.out.println("hwManager: " + hwManager);
					hwManagerStub.registerComputer(hwComputer);
				}
				else
					continue;
				System.out.println("[HwComputer.main] Connected.");
				// Stop trying to connect if success.
				break;
			} catch (RemoteException | NotBoundException | MalformedURLException e) {
				System.out.println("[HwComputer.main] Connect failed. Trying...");
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("[HwComputer.main] Connect failed. Trying again.");
			}
		}
		while(true){
			//stay connected
		}

	}
}
