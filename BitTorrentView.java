//Imports
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JSeparator;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.Component;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.ImageIcon;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.*;
import java.nio.file.Paths;
import java.nio.file.Path;

import javax.swing.*;

class BitTorrentView extends JFrame
{
	// Instance attributes used in this example
	public static JFileChooser fc;
	protected File file;
	protected JTextArea log;
	static private final String newline = "\n";
	JFrame frame;
	private static JTable table;
	MyProgressBar pro;
	MyTableModel tableModel;
	// Create some data
	public static Object dataValues[] = { "", "","", "", "","", "", "", "" };
	public static DecimalFormat twoDForm = new DecimalFormat("#.##");
	private static HashMap<Integer,Download> clients = new HashMap<Integer, Download>();
	private static ArrayList<waitObj> waiting = new ArrayList<waitObj>(); 
	public static Download running = null;

	// Constructor of main frame
	public BitTorrentView() {
		setResizable(false);
		getContentPane().setSize(new Dimension(450, 300));
		setSize(new Dimension(950, 355));
		setTitle("ZMJTorrent");
		System.out.println("init!");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setPreferredSize(getPreferredSize());
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
	    pro = new MyProgressBar();
	    pro.setMaximum(100);
	    // pro.setv
	    pro.setValue(0);
	    pro.setBorderPainted(true);
		
	    dataValues[4]=pro;
	    
		JButton btnBrowse = new JButton("");
		btnBrowse.setSelected(true);
		btnBrowse.setIcon(new ImageIcon("C:\\Users\\MANISH\\workspace\\GUIDemo\\Open16.gif"));
		btnBrowse.setHorizontalTextPosition(SwingConstants.CENTER);
		btnBrowse.setAlignmentX(Component.CENTER_ALIGNMENT);
		//Create a file chooser
        fc = new JFileChooser();
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//do stuff when browse btn clicked
				int returnVal = fc.showOpenDialog(frame);

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                file = fc.getSelectedFile();
	                dataValues[0]=file.getName();
	                tableModel.addRow(dataValues);
	                tableModel.getValueAt(table.getSelectedRow(), 0);
	               	download(file);
	                
	            } else {
	                log.append("Open command cancelled by user." + newline);
	            }
			}
		});
		getContentPane().setLayout(null);
		btnBrowse.setBounds(10, 11, 40, 25);
		getContentPane().add(btnBrowse);
		
		final JToggleButton tglbtnStart = new JToggleButton(new ImageIcon("C:\\Users\\MANISH\\workspace\\GUIDemo\\Start.png"));
		tglbtnStart.setPreferredSize(new Dimension(40, 33));
		tglbtnStart.setMinimumSize(new Dimension(40, 33));
		tglbtnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
		tglbtnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(tglbtnStart.isSelected()){
					
					String name = "";
            		for(int i = 0; i<running.tiObject.file_name.length();i++)
            		{
               		 if(Character.isLetterOrDigit(running.tiObject.file_name.charAt(i)))
                	 {
                  		  name+=running.tiObject.file_name.charAt(i);
               		 }else{
                	   	 break;
                	 }
            		}
            		name+="hist.txt";
            		waiting.add(new waitObj(1,name,null));
					tglbtnStart.setText("Pause");
					running.running = false;
					running = null;
					//tglbtnStart.setIcon(new ImageIcon("C:\\Users\\MANISH\\workspace\\GUIDemo\\Pause.png"));
					//do stuff when Start is clicked
				}else{

					if(waiting.size()>0){

						waitObj temp = waiting.remove(0);
						System.out.println(temp.name+" "+temp.type);
						if(temp.type == 1)
						{
							down(temp.name ,1);
						}
						tglbtnStart.setText("Start");
					}
					//tglbtnStart.setIcon(new ImageIcon("C:\\Users\\MANISH\\workspace\\GUIDemo\\Start.png"));
					//do stuff when pause clicked
				}
			}
		});
		tglbtnStart.setBounds(112, 11, 40, 25);
		getContentPane().add(tglbtnStart);
		
		JButton btnRemove = new JButton("");
		btnRemove.setHorizontalTextPosition(SwingConstants.CENTER);
		btnRemove.setIcon(new ImageIcon("C:\\Users\\MANISH\\workspace\\GUIDemo\\Delete.png"));
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//do stuff when remove btn is clicked
				removeSelectedRows(table);
			}
		});
		btnRemove.setBounds(148, 11, 40, 25);
		getContentPane().add(btnRemove);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 216, 924, 93);
		getContentPane().add(scrollPane);
		
		log = new JTextArea();
		log.setEditable(false);
		scrollPane.setViewportView(log);
		log.setBackground(Color.LIGHT_GRAY);
		
		JLabel lblLogs = new JLabel("Logs");
		lblLogs.setHorizontalAlignment(SwingConstants.CENTER);
		lblLogs.setBounds(318, 193, 70, 25);
		getContentPane().add(lblLogs);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(10, 55, 91, 114);
		getContentPane().add(scrollPane_1);
		
		
		DefaultListModel model = new DefaultListModel();
		JList list = new JList(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setBorder(new EmptyBorder(0, 0, 0, 0));
		model.addElement("Completed (" + 1 +")");
		model.addElement("Active (" + 2 +")");
		model.addElement("Inactive (" + 0 +")");
		scrollPane_1.setViewportView(list);
		
		
		JScrollPane scrollPane_2 = new JScrollPane();
		scrollPane_2.setBounds(111, 55, 823, 114);
		getContentPane().add(scrollPane_2);
		
		// Create columns names
		String columnNames[] = { "Name", "Size(kb)","Torrent", "Time", "Status","Uploaded", "U Speed", "Peers", "Seed" };

			
		tableModel = new MyTableModel(columnNames, 0);
		//tableModel.addRow(dataValues);
		
		// Create a new table instance
		table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
	        public void valueChanged(ListSelectionEvent event) {
	            // do some actions here, for example
	            // print first column value from selected row
	            System.out.println(table.getValueAt(table.getSelectedRow(), 0).toString());
	        }
	    });
		
		
		TableColumn column = null;
		for (int i = 0; i < columnNames.length; i++) {
		    column = table.getColumnModel().getColumn(i);
		    if (i==0) {
		        column.setPreferredWidth(100); //1st column is bigger
		    } else if(i == 4){
		    	column.setPreferredWidth(150); //5th column is bigger
		    }else {
		        column.setPreferredWidth(50);
		    }
		}
		table.getColumnModel().getColumn(4).setCellRenderer(new MyProgressBar());
		scrollPane_2.setViewportView(table);
		
		JSeparator separator = new JSeparator();
		separator.setBounds(10, 193, 924, 8);
		getContentPane().add(separator);
		
		JSeparator separator_1 = new JSeparator();
		separator_1.setBounds(10, 45, 924, 14);
		getContentPane().add(separator_1);

	}
	
	public void removeSelectedRows(JTable t){
		   int[] rows = t.getSelectedRows();
		   for(int i=0;i<rows.length;i++){
		     tableModel.removeRow(rows[i]-i);
		   }
		}
	
	/*private void selectedRowValue(JTable t){

               
              int selectedRow = t.getSelectedRow();
               
                                     
             System.out.println("Selected row "+ " "+ selectedRow);
             
             Object selectedCellValue=t.getValueAt(selectedRow, 0);
             System.out.println("selectedCellValue "+" "+selectedCellValue);
		}*/

	
	
	public static void update(int b, int up, double ds, double us, int p) {
	
	    MyProgressBar bar = (MyProgressBar) table.getValueAt(0, 4);
	    bar.setStringPainted(true);
				
	    if(bar.getValue()==100){
	    	bar.setString("Seeding");
	    }else{
	    	bar.setString(bar.getValue());
	    }
	    bar.setValue(b);
	    ((MyTableModel) table.getModel()).fireTableCellUpdated(0, 4);
	    
	    dataValues[2]=ds;
	   	table.setValueAt(dataValues[2], 0,2);

	    dataValues[5] = up;
	    table.setValueAt(dataValues[5], 0,5);
		dataValues[6] = us;
	    table.setValueAt(dataValues[6], 0,6);
	    dataValues[7] = p;
	    table.setValueAt(dataValues[7], 0,7);
	}
	class MyProgressBar extends JProgressBar implements TableCellRenderer {

	    @Override
	    public Component getTableCellRendererComponent(JTable table,
	            Object value, boolean isSelected, boolean hasFocus, int row,
	            int column) {

	        if (value instanceof JComponent) {
	            return (JComponent) value;
	        } else {
	            return null;
	        }
	    }

	    public void setString(int value) {
			// TODO Auto-generated method stub
			
		}

		@Override
	    public void setValue(int n) {

	        super.setValue(n);
	    }

	}
	private class MyTableModel extends DefaultTableModel {

	    public MyTableModel(Object[] obj, int row) {

	        super(obj, row);
	    }

		@Override
	    public boolean isCellEditable(int row, int column) {
	       //all cells false
	       return false;
	    }
	}
	
	// Main entry point for this example
	public static void main(String[] args)
	{

		try {
			BitTorrentView window = new BitTorrentView();
			window.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//before everything... load the history download first 
		File[] files = new File(Paths.get(".").toAbsolutePath().normalize().toString()).listFiles();
		int index = 1;
		if(files!=null)
		{
			for(File file : files)
			{
				if(file.getName().contains("hist.txt")){
					System.out.println(file.getName());
					down(file.getName(),index);
				}
				index++;
			}
        }else{
        	System.out.println("\nNo history file.");
        }
	
	}
	
	private static void down(String name, int i)
	{
		if(running == null){
			running = Download.loadDownloadHistory(name);
			clients.put(i, running);
			try{
				(new Thread(running)).start();
				dataValues[0] = running.tiObject.file_name;
				table.setValueAt(dataValues[0],0,0);
				dataValues[1]=twoDForm.format(running.tiObject.file_length/1024);
				table.setValueAt(dataValues[1], 0, 1);
				updateView up = new updateView(running, 0);
				(new Thread(up)).start();
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}else{

		}
	}

	private void download(File f)
	{	
		if(running != null){
			waiting.add(new waitObj(0,null, f));
			return;
		}
		running = new Download(f);
	    try{
	        	(new Thread(running)).start();
	    }catch(Exception a)
	    {
	    }
	    table.setValueAt(dataValues[0], 0, 0);
	    dataValues[1]=twoDForm.format(f.length()/1024);
	    table.setValueAt(dataValues[1], 0, 1);
	    System.out.println(dataValues[0]);
	  	log.append("Opening: " + f.getName() + "." + newline);
	}
}

class waitObj
{
    int type;		// 0 for torrent file, 1 for history file
    String name;
    File f;
    public waitObj(int t, String n, File f)
    {
        this.type = t;
        this.name = n;
        this.f = f;
    }
}

class updateView implements Runnable{

	public Download dl;
	public int id;	
	int lastdownloaded;
	int lastuploaded;
	public updateView(Download dl, int id)
	{
		this.dl = dl;
		this.id = id;
		this.lastuploaded = dl.uploaded;
		this.lastdownloaded = dl.downloaded;
	}

	public void run(){
		try{
		while(BitTorrentView.running != null){
			
			Thread.sleep(1000);
			if(BitTorrentView.running == null) return;
			int per = 100*dl.downloaded / (dl.downloaded+dl.left);

			double ds = ((double)dl.downloaded - (double)lastdownloaded)/1024;
			double us = ((double)dl.uploaded - (double)lastuploaded)/1024;
			this.lastuploaded = dl.uploaded;
			this.lastdownloaded = dl.downloaded;
			System.out.println(ds+" "+us);			
			BitTorrentView.update(per,dl.uploaded/1024,ds,us,0);
		}
	}catch(Exception e)
	{
		e.printStackTrace();
	}
	}

}
