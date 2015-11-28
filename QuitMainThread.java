import java.util.Scanner;

/**
 * This is a class waiting for input from users so that the program is able to quit normally
 * after quiting, all the piece downloaded will be saved.
 */

public class QuitMainThread implements Runnable{
	  private Thread t;
	  private String threadName;
	  
	  public QuitMainThread(String name){
	       threadName = name;
	   }

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Scanner scannerObj = new Scanner(System.in);
		
		System.out.println("Type <quit> or <q> to quit the program");
		String input = scannerObj.nextLine();
		
		//waiting for input
		while((input==null)||((!input.equalsIgnoreCase("quit")) && (!input.equalsIgnoreCase("q")))){
			System.out.println("Type <quit> or <q> to quit the program");
			input = scannerObj.nextLine();
		}

		if(input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")){
				RUBTClient.running = false;
				System.out.println("All shut!");
		}

	}

}