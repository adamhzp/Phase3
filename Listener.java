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

	/**
	 * Constructor for the runnable class Listener
	 * @param port	The port number the server socket will listen on
	 * @param infoHash The info hash extracted from the torrent
	 * @param id The peer id of the self program
	 * @param pieces The total number of pieces
	 */
	
	public Listener(int port, ByteBuffer infoHash, ByteBuffer id, int pieces)
	{
		this.port = port;
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
			System.out.println("Listener running");
			if(!running)
				break;
			Socket client = null;
			try{
				 client = server.accept();
			}catch(Exception e)
			{
				System.out.println("server is closed!");
			}
			System.out.println("as");
			//create a connectionToPeer object and start communication
			if(client != null){
				ConnectionToPeer pc = new ConnectionToPeer(client, this.selfInfoHash, this.selfId, this.pieces);			
				(new Thread(pc)).start();
			}
			
		}
		System.out.println("listener is shut");
			
		
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
