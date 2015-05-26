package system;

import com.amazonaws.services.ec2.model.Instance;
import rmi.HwManagerInterface;
import rmi.HwUserInterface;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

public class HwUser implements HwUserInterface {

	HwManagerInterface _hwManager;
	boolean _connectedToHwManager = false;
	String _domainName = "";

	List<Instance> instances;



	HwUser(String domain)
	{
		_domainName = domain;

	}




	void connectHwManager(){
		String url = "rmi://" + _domainName + ":" + HwManager._port + "/" + HwManager._serviceName;
		System.out.println("[HwUser.main] Connecting to: "+url);
		try {

			_hwManager = (HwManagerInterface) Naming.lookup(url);
			if (_hwManager != null) {
				String reply = _hwManager.userJustCheckingIn();
				System.out.println("[HwUser.HwUser] reply from manager: " + reply);
				_connectedToHwManager = true;
			}
			else
			{
				System.out.println("[HwUser.HwUser] Naming.lookup returned null.");
			}

		} catch (NotBoundException|MalformedURLException|RemoteException e) {
			System.out.println("Could not connect to Diva Hardware Manager.");
			// e.printStackTrace();

		}
		System.out.println("[HwUser.main] Connected to: " + url);
	}




	 void printMenu(){

		 System.out.println("\n\n\n******************************************************************"
				 + "Welcome to DivA. Connected: " + _connectedToHwManager
				 + "Select an option:\n\n"
				 + "1. Run Traveling Salesman Problem...\n"
				 + "2. System Status\n"
				 + "3. Clear All Worker Processes\n"
				 + "4. Terminate AWS Instance...\n"
				 + "5. Connect to Diva Manager (hardware manager/space instance)\n"
				 + "6. Start Diva Hardware Manager (on AWS)\n\n\n"
		 );
		 System.out.print("> ");

	 }


	void processInput(int selection){

		System.out.println("Selection: " + selection);

		switch (selection){
			case 1: {
				runTsp();
			}
			case 2:{

			}
			case 3:{

			}
			case 4:{

			}
			case 5:{

			}
			default:{
				System.out.println("Select something else: " + selection);

			}
		}

	}

	void runConsole(){
		connectHwManager();

		Scanner in = new Scanner(System.in);
		int selection = 0;

		while(true){
			printMenu();

			try {
				if(in.hasNextInt()) {
					selection = in.nextInt();
					processInput(selection);
					in.nextLine();
				}
				else if(in.hasNextLine())
					in.nextLine();
			}catch(Exception e){
				// do nothing
				selection = 0;
			}
		}
	}





	// *** 1 ***
	void runTsp(){

		String computerStartCommand = "/Users/aaa/290a/aws/aws-test1/aws-test1/computer.sh";
		String[] commands = {computerStartCommand};

		try {

			Process p = Runtime.getRuntime().exec(commands);
			System.out.println("[HwComputer.startLogicalComputers] pid: " + p.toString() + ", isAlive: " + p.isAlive());

		} catch (IOException/*|InterruptedException*/ e) {
			e.printStackTrace();
		}



	}








	public static void main(String[] args){
		System.out.println("[system.HwUser.main] args: " + args[0]);

		if(args.length>0)
		{

			HwUser hwUser = new HwUser(args[0]);

			hwUser.runConsole();

			/*


			try {
				hwUser._hwManager.getRunningInstances();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			*/
		}
	}
}