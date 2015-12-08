import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;


/*
 * This is a class that set up the server socket and listen on a certain port.
 * Any incomming connection will be handled in this thread and a connectionToPeer object will be created.
 * All these object will be added to the main thread
 */
public class Listener implements Runnable{

	private boolean running = true;
	private int port;
	private ServerSocket server = null;
	private ByteBuffer selfInfoHash = null;
	private ByteBuffer selfId = null;
	private int pieces;
	private Download dl = null;

	/**
	 * Constructor for the runnable class Listener
	 * @param port	The port number the server socket will listen on
	 * @param infoHash The info hash extracted from the torrent
	 * @param id The peer id of the self program
	 * @param pieces The total number of pieces
	 */
	
	public Listener(int port, ByteBuffer infoHash, ByteBuffer id, int pieces, Download dl)
	{
		this.port = port;
		this.dl = dl;
		this.selfInfoHash = infoHash.duplicate();
		this.selfId = id.duplicate();
		this.pieces = pieces;
		try {
			server = new ServerSocket(port);
		}catch(IOException e)
		{
			e.printStackTrace();
			running = false;
		}
		running = true;
	}


	@SuppressWarnings("resource")
	@Override
	public void run()
	{
		while(running)
		{
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Socket client = null;
			try{
				 client = server.accept();
			}catch(Exception e)
			{
				break;
			}

			//create a connectionToPeer object and start communication
			ConnectionToPeer pc = new ConnectionToPeer(client, this.selfInfoHash, this.selfId, this.pieces, dl);			
			(new Thread(pc)).start();
		}
		
					
	}

	/**
	 * Shutting down the runnable listener
	 */
	
	public void shutDown()
	{
		this.running = false;
		try{
			server.close();
		}catch(IOException e)
		{
			e.printStackTrace();
		}

	}

}
