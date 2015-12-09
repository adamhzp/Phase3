import java.util.Scanner;
import java.io.*;

/**
 * This is the main client class for CS352, BitTorrent project 1 The program is
 * designed to use a command-line argument the name of the .torrent file to be loaded
 * and the name of the file to save the data to
 * Opens the .torrent file and parse the data inside and decodes the data
 * Send an HTTP GET request to the tracker at the IP address and port specified by the TorrentFile object.
 * Download the piece of the file and verify its SHA-1 hash against the hash stored in the metadata file
 * All communication is done over TCP.
 * 
 * @author ZHANPENG HE, MANISH PATEL, JOHN NELSON
 *
 */
public class RUBTClient{


	public static void main(String[] args){
		 if (args.length != 2) {
            System.out.println("Incorrect number of arguments");
            System.out.println("Correct Usage: java -cp . RUBTClient <.torrent file> <File to Save to>");
            return;
        }
        //to load the torrent file
        File tf = new File(args[0]);
		if (!tf.canRead()) {
			System.out.println("Can't read torrent file");
			return;
		}
		Download dl = new Download(tf);
		(new Thread(dl)).start();

		Scanner scannerObj = new Scanner(System.in);
		System.out.println("Type <quit> or <q> to quit the program");
		String input = scannerObj.nextLine();
		//waiting for input
		while((input==null)||((!input.equalsIgnoreCase("quit")) && (!input.equalsIgnoreCase("q")))){
			System.out.println("Type <quit> or <q> to quit the program");
			input = scannerObj.nextLine();
		}

		if(input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")){
				dl.running = false;
				System.out.println("All shut!");
		}
	}



}
