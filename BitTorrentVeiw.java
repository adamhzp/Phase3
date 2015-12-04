//Imports
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.nio.file.Paths;
import java.nio.file.Path;

import javax.swing.*;

class BitTorrentView extends 	JFrame
{
	// Instance attributes used in this example
	private	JPanel		centerPanel;
	private	JTable		table;
	private	JScrollPane scrollPane;
	private static File file = null;
	JProgressBar pbar;
	private static HashMap<Integer,RUBTClient> clients = new HashMap<Integer, RUBTClient>();
	

	static final int MY_MINIMUM = 0;

	static final int MY_MAXIMUM = 100;
	
	static private final String newline = "\n";

	JFileChooser fc;
	JTextArea log;
	JScrollPane logScrollPane;
	// Constructor of main frame
	public BitTorrentView()
	{
		// Set the frame characteristics
		setTitle( "ZMJ BitTorrent" );
		setBackground( Color.gray );

		JPanel topPanel = new JPanel();
		getContentPane().add(topPanel, BorderLayout.NORTH);
		
		JButton btnBrowse = new JButton("Browse");
		
		log = new JTextArea(5,80);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        logScrollPane = new JScrollPane(log);
        
		//Create a file chooser
        fc = new JFileChooser();
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
	            int returnVal = fc.showOpenDialog(BitTorrentView.this);
	            

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                file = fc.getSelectedFile();
	                System.out.println(file.getName());
	        		RUBTClient client = new RUBTClient(file);
	        		try{
	        			client.run();
	        		}catch(Exception e)
	        		{
	        			e.printStackTrace();
	        		}
	                //This is where a real application would open the file.
	                log.append("Opening: " + file.getName() + "." + newline);
	            } else {
	                log.append("Open command cancelled by user." + newline);
	            }
	            //log.setCaretPosition(log.getDocument().getLength());

	        //Handle save button action.
	        }
		});
		topPanel.add(btnBrowse);
		
		JButton btnStart = new JButton("Start");
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		topPanel.add(btnStart);
		
		JButton btnPause = new JButton("Pause");
		btnPause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		topPanel.add(btnPause);
		
		JButton btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		topPanel.add(btnStop);
		
		JButton btnRemove = new JButton("Remove");
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		topPanel.add(btnRemove);
		
		JPanel CenterLeftPanel = new JPanel();

		getContentPane().add(CenterLeftPanel, BorderLayout.WEST);
		
		
		DefaultListModel model = new DefaultListModel();
		JList list = new JList(model);
		
		model.addElement("Completed (" + 1 +")");
		model.addElement("Active (" + 2 +")");
		model.addElement("Inactive (" + 0 +")");
		CenterLeftPanel.add(list);
		
		
		// Create a panel to hold all other components
		centerPanel = new JPanel();
		centerPanel.setLayout( new BorderLayout() );
		getContentPane().add( centerPanel );

		// Create columns names
		String columnNames[] = { "Name", "Size", "Completed", "D Speed", "Time", "Status", "Peers", "Seed", "Uploaded", "U Speed"};

		// Create some data
		String dataValues[][] =
		{
			{ "Phase1.torrent", "234", "67", "123", "43", "Downloading", "2", "5", "25", "20" },
			{ "Phase2.torrent", "892", "100", "279", "90", "Seeding", "5", "10", "52", "10" },
			{ "Phase3.torrent", "892", "19", "29", "933", "downloading", "2", "1", "5", "23" }
		}; 

		// Create a new table instance
		table = new JTable( dataValues, columnNames );
		
		// Add the table to a scrolling pane
		scrollPane = new JScrollPane( table );
		centerPanel.add( scrollPane, BorderLayout.CENTER );
		
		pbar = new JProgressBar();
	    pbar.setMinimum(MY_MINIMUM);
	    pbar.setMaximum(MY_MAXIMUM);
	    pbar.setStringPainted(true);
	    centerPanel.add(pbar, BorderLayout.SOUTH);
		
		JPanel bottomPanel = new JPanel();
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
			
		JLabel lblErrorLogs = new JLabel("Logs: ");
		bottomPanel.add(lblErrorLogs);
		
		bottomPanel.add(logScrollPane);
	}
	public void updateBar(int newValue) {
	    pbar.setValue(newValue);
	  }
	
	private static void download(String name, int i)
	{
		RUBTClient temp = RUBTClient.loadDownloadHistory(name);
		clients.put(i, temp);
		try{
			(new Thread(temp)).start();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	// Main entry point for this example
	public static void main(String[] args)
	{
		//before everything... load the history download first 
		File[] files = new File(Paths.get(".").toAbsolutePath().normalize().toString()).listFiles();
		int index = 1;
		if(files!=null)
		{
			for(File file : files)
			{
                System.out.println(file.getName());
				if(file.getName().contains("hist.txt"))
					download(file.getName(),index);
				index++;
			}
        }else{
            System.out.println("null");
        }
		// Create an instance of the test application
		final BitTorrentView mainFrame	= new BitTorrentView();
		mainFrame.setVisible( true );
		mainFrame.setSize(900, 300);
		
		for (int i = MY_MINIMUM; i <= MY_MAXIMUM; i++) {
		      final int percent = i;
		      try {
		        SwingUtilities.invokeLater(new Runnable() {
		          public void run() {
		            mainFrame.updateBar(percent);
		          }
		        });
		        java.lang.Thread.sleep(100);
		      } catch (InterruptedException e) {
		        ;
		      }
		    }
		
	}
}
