import java.io.IOException;

import TSim.TSimInterface;

public class Main {

	/**
	 * The main method expects 3-4 arguments, e.g.:
	 * - command line: java -cp bin Main "Lab1.map" 5 10 20
	 * -   in Eclipse: add them from Run Configurations -> Arguments
	 */
	public static void main(String[] args) {
		try {
			String map = args[0];
			Integer train1_speed = Integer.parseInt(args[1]);
			Integer train2_speed = Integer.parseInt(args[2]);
			Integer tsim_speed = (args.length >= 4) ? Integer.parseInt(args[3]) : 20;
			
			String tsimCommand = String.format("/home/long/TDA384/tsim-core/src/tsim --speed=%d %s", tsim_speed, map);
			Process p = Runtime.getRuntime().exec(tsimCommand);
			TSimInterface.init(p.getInputStream(), p.getOutputStream());
			TSimInterface.getInstance().setDebug(true);
			new Lab1(train1_speed, train2_speed);
			// new Lab2(train1_speed, train2_speed);
			p.waitFor();
			
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Main method expects 3-4 arguments: Lab1.map <Train1Speed> <Train2Speed> [SimulatorSpeed]");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
