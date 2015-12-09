import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.nio.charset.Charset;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.*;

import java.lang.instrument.Instrumentation;

import GivenTools.*;

public class Download implements Runnable{
    private ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<Message>();
    private HashMap<ByteBuffer,Peer> peers = new HashMap<ByteBuffer, Peer>();
    private ArrayList<Pieces> pieces;
    public TorrentInfo tiObject = null;
    private URL trackerURL = null;
    private MappedByteBuffer fileByteBuffer;
    
    /**
     * Fields used for Tracker Request
     * */
    private String peerId;           // A string of length 20 which this downloader uses as its id. 
    private  int  port = 6883;        // The port number this peer is listening on.
    public  int uploaded = 0;        // The total amount uploaded so far
    public  int downloaded = 0;      // The total amount downloaded so far
    public  int left = 0;            // The number of bytes this peer still has to downloaded
    private  String event=null;       // This is an optional key which maps to started , completed , stopped or null
    private  long lastAnnounce = 0;
    private  int minInterval = 0;
    private  int interval = 0;        // The number of seconds the downloader should wait between regular rerequests.
    private  int pcTotal = 0;
    private  boolean sentComplete = false;
    public  boolean running = true;
    
    private final  Object fileLock = new Object();
    private final  Object peerLock = new Object();
    private  RandomAccessFile dataFile;
    private  BitSet piecesHad = null;
    private  String hash = null;      // hash of the bencoded form of the info value from the metainfo file.
    private  Map<ByteBuffer, Object> trackerResponse = null;
    /**
     * Contains information from a TrackerResponse
     */
    public static final ByteBuffer INCOMPLETE = ByteBuffer.wrap(new byte[] {'i','n','c','o','m','p','l','e','t','e'});
    public static final ByteBuffer PEERS = ByteBuffer.wrap(new byte[] {'p','e','e','r','s'});
    public static final ByteBuffer DOWNLOADED = ByteBuffer.wrap(new byte[] {'d','o','w','n','l','o','a','d','e','d'});
    public static final ByteBuffer COMPLETE = ByteBuffer.wrap(new byte[] {'c','o','m','p','l','e','t','e'});
    public static final ByteBuffer MIN_INTERVAL = ByteBuffer.wrap(new byte[] {'m','i','n',' ','i','n','t','e','r','v','a','l'});
    public static final ByteBuffer INTERVAL = ByteBuffer.wrap(new byte[] {'i','n','t','e','r','v','a','l'});

    public static final ByteBuffer PEER_IP = ByteBuffer.wrap(new byte[] {'i','p'});
    public static final ByteBuffer PEER_ID = ByteBuffer.wrap(new byte[] {'p','e','e','r',' ','i','d'});
    public static final ByteBuffer PEER_PORT = ByteBuffer.wrap(new byte[] {'p','o','r','t'});
    

    private  boolean isSeedingOnly = false;
    private  boolean isCompleted = false;

    private  int initiateTime = 0;
    private  int finishTime = 0;
    private  Timer chokeTimer;
    private  byte[] torrentFileData = null;
    
    public Download(File torrentFile)
    {
    	try{
            this.running = true;
    		long sizeOfFile = torrentFile.length();
    		torrentFileData = new byte[(int) sizeOfFile];
    		Path p = torrentFile.toPath();
    		torrentFileData = Files.readAllBytes(p);
    		this.tiObject = new TorrentInfo(torrentFileData);
    		this.left = this.tiObject.file_length;    // The number of bytes this peer still has to download  
            this.peerId = "adambittorrentclient";            // A string of length 20 generated which downloader uses as its id          
            pieces = generatePieces();      // generate piece from torrent
            hash = infoHash(tiObject.info_hash.array());
            trackerURL =tiObject.announce_url;
            String destinationFile = tiObject.file_name;
            try {
                dataFile = new RandomAccessFile(destinationFile, "rw");
                fileByteBuffer = dataFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, (Integer)tiObject.info_map.get(TorrentInfo.KEY_LENGTH));
            } catch (FileNotFoundException e) {
                System.err.println("FileNotFound exception occure while opening the destinationFile to write");
            } catch (IOException e) {
                System.err.println("IO exception occure while opening the destinationFile to write");
            }
            //load history here 
            String name = "";
            for(int i = 0; i<tiObject.file_name.length();i++)
            {
                if(Character.isLetterOrDigit(tiObject.file_name.charAt(i)))
                {
                    name+=tiObject.file_name.charAt(i);
                }else{
                    break;
                }
            }
            name+="hist.txt";
            loadHistory(name);
            trackerResponse = announce("start", peerId, port, uploaded, downloaded, left, hash);
            ToolKit.printMap(trackerResponse, 5);
    	}catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
    public Download(ByteBuffer data)
    {
    	try{
    		this.torrentFileData = data.array();
    		this.tiObject = new TorrentInfo(torrentFileData);
    		this.left = this.tiObject.file_length;    // The number of bytes this peer still has to download  
    		this.peerId = "adambittorrentclietn";            // A string of length 20 generated which downloader uses as its id          
    		this.pieces = generatePieces();      // generate piece from torrent
    		this.hash = infoHash(tiObject.info_hash.array());
    		this.trackerURL =tiObject.announce_url; 
        	this.trackerResponse = announce("start", peerId, port, uploaded, downloaded, left, hash);
        	String destinationFile = "downloadedFile.mov";
            try {
                this.dataFile = new RandomAccessFile(destinationFile, "rw");
                this.fileByteBuffer = dataFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, (Integer)tiObject.info_map.get(TorrentInfo.KEY_LENGTH));
            } catch (FileNotFoundException e) {
                System.err.println("FileNotFound exception occure while opening the destinationFile to write");
            } catch (IOException e) {
                System.err.println("IO exception occure while opening the destinationFile to write");
            }

    	}catch(Exception e){
        	e.printStackTrace();
        }
        ToolKit.printMap(trackerResponse, 5);
    }

    /**
     * This is the main method that is called upon program startup, this method
     * is tasked with validating the program's arguments and then instantiating
     * the other classes need to run the program. When the download is finished
     * this method verifies that it has completed successfully.
     * 
     * @param args he name of the .torrent file to be loaded and the name of the
     *        file to save the data to, using the proper path and file extensions.
     * @throws IOException
     * @throws BencodingException
     */
    public void run() {
    
        /*
         * The main method requires two command line arguments: The name of the
         * torrent file to load, and the name to the resulting file to save-as.
         */
        Listener listener = null; 
    
        //choke and unchoke peers
        try{
            chokeTimer = new Timer("Choke Timer", true);
            chokeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateChokedPeers();
                }
            }, 0, 30000);


            // Obtain list of peers from Tracker Response Object and finds the peers at IP address with peer_id prefix RUBT11
            
            ArrayList<HashMap<ByteBuffer,Object>> tmp_peers = (ArrayList<HashMap<ByteBuffer, Object>>) trackerResponse.get(PEERS);
            int i = 99;

            listener = new Listener(port,tiObject.info_hash,ByteBuffer.wrap(peerId.getBytes()), pcTotal, this);
            (new Thread(listener)).start();

            for (HashMap<ByteBuffer,Object> p : tmp_peers) {
                //only use the peer which peer id is begin with ru or -ru(i found it should be -ru when i print out the map)
                if ((new String(((ByteBuffer)p.get(PEER_IP)).array())).equals("128.6.171.130")  || (new String(((ByteBuffer)p.get(PEER_IP)).array())).equals("128.6.171.131")||(new String(((ByteBuffer)p.get(PEER_IP)).array())).equals("128.6.171.132")) {
                    ConnectionToPeer pc = new ConnectionToPeer(new String(((ByteBuffer)p.get(PEER_IP)).array()), (Integer) p.get(PEER_PORT), (ByteBuffer)p.get(PEER_ID),tiObject.info_hash,ByteBuffer.wrap(peerId.getBytes()), this);
                    pc.sendHandshake(tiObject.info_hash, ByteBuffer.wrap(peerId.getBytes()));
                    Peer pr = new Peer((ByteBuffer) p.get(PEER_ID), pc, pcTotal, this);
                    pr.handshook = false;
                    this.peers.put(pr.peerId, pr);
                    //
                    (new Thread(pc)).start();
                    if (--i == 0) break;
                }           
            }

            //get the min interval from tracker response
            minInterval = (Integer)trackerResponse.get(MIN_INTERVAL) * 1000;
            interval = (Integer)trackerResponse.get(INTERVAL) * 1000;
    
            // If there's no minimum interval
            if (minInterval == 0)  minInterval = interval / 2;
    
            lastAnnounce = System.currentTimeMillis();
            if(running == false)
                System.out.println("running == false ???");    
            while(running) {
                //keep tracker updated!!!
                if ((System.currentTimeMillis() - lastAnnounce) >= (minInterval - 5000)) {
                    announce(null, peerId, port, uploaded, downloaded, left, hash);
                    lastAnnounce = System.currentTimeMillis();
                }
    
                // Process all messages that have come in since the last time we looped.
                processMessages();
    
                // At this point, all peers that are no longer busy (in a multi-part communication)
                // are marked as not busy. So, let's decide what we want each of them to do.
                processFreePeers();
    
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
    } finally {
        System.out.println("\n=============================================== \nExiting the Program now:\n");
        if(!isCompleted){
            storeTempPieces();
        }else if(false)
        {   
            try{
                Process p = Runtime.getRuntime().exec("rm Downloaded.txt");
            }catch(IOException e)
            {
                System.err.println("Error: rm download history fail!!");
            }
        }
        
        System.out.println("The download is "+(100*downloaded / (downloaded+left))+ "% completed!!");
        
        System.out.println("Downloaded: "+downloaded+" bytes   Uploaded: "+uploaded+" bytes");
        System.out.println();
        for(Peer pr: peers.values()){
            System.out.println("IP Address: "+pr.connection.ip+" Downloaded: "+pr.downloaded+" Uploaded: "+pr.uploaded+" Start time: "+pr.startTime);
        }
        System.out.println("=============================================== \n");
        verify(); 
        synchronized (fileLock) {
            try {
                if (dataFile != null) dataFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileByteBuffer = null;
        }
        try {
            // send stopped message
            announce("stop", peerId, port, uploaded, downloaded, left, hash);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BencodingException e) {
            e.printStackTrace();
        }

        listener.shutDown();    //shut down the listener
        for (Peer pr: peers.values()) { // shutdown all the peers
            pr.connection.running=false;
        }
     }   
     chokeTimer.cancel();
    }


    /**
     * Handle all the messages in the array list. These messages are passes by the connection thread.
    */  
    public void processMessages() {
        Message msg;
        while ((msg = messages.poll()) != null) {
            handleMessage(peers.get(msg.getPeerId()), msg);
        }
    }

    /**
       This is a method that handles a single message. For different type messages, we do different things to the buffer(which is the msg in Message object).
       @param pr The peer who is sending this message
       @param msg the message from the peer 
     */
    private void handleMessage(Peer pr, Message msg) {
        if (!pr.handshook) {
            if (msg.getType() == Message.MessageType.Handshake) {
                ByteBuffer message = msg.getBytes();
                if (message.get() != 19 || ((ByteBuffer)message.slice().limit(19)).compareTo(ByteBuffer.wrap(ConnectionToPeer.PROTOCOL_HEADER)) != 0) { // Not BT
                    pr.connection.running = false;
                    return;
                }
                if (((ByteBuffer)message.slice().position(19+8).limit(20)).compareTo(tiObject.info_hash) != 0) { // Wrong infohash
                    pr.connection.running = false;
                    return;
                }
                if (((ByteBuffer)message.slice().position(19+8+20)).compareTo(pr.peerId) != 0) { // Wrong peerId
                    pr.connection.running = false;
                    return;
                }
                //here, we are sure all the info match. So handshake is successfull now
                ByteBuffer bf = getBitField();
                if (bf != null && !pr.connection.ip.equalsIgnoreCase("128.6.171.132")) {
                    pr.connection.sendBitfield(bf);
                }
                
                for(Pieces pc : pieces)
                {
                    if(piecesHad.get(pc.getIndex()))
                    {
                        pr.connection.sendHave(pc.getIndex());
                    }
                }
                pr.startTime = (int) System.currentTimeMillis();
                pr.handshook = true;
                return;
            }

        }
        switch (msg.getType()) {
            case Handshake:
                break;
            case Choke: // Choke
                pr.choked = true;
                pr.outstandingRequests = 0;
                break;
            case Unchoke: // Unchoke
                pr.choked = false;
                break;
            case Interested: // Interested
                pr.interested = true;
                break;
            case UnInterested: // Not Interested
                pr.interested = false;
                break;
            case Have: // Have
                int index = msg.msg.getInt();
                pr.setPieceAvailable(index);
                if (!pr.interestFromPeer && !piecesHad.get(index)) {
                    pr.interestFromPeer = true;
                    pr.connection.sendInterested();
                }
                break;
            case Bitfield: // Bitfield
                pr.setAvailablePieces(msg.getBitfield());
                BitSet tmp = ((BitSet)piecesHad.clone());
                tmp.flip(0, piecesHad.size());
                if (!tmp.intersects(pr.getAvailablePieces())) {
                    pr.interestFromPeer = false;
                    pr.connection.sendUnInterested();
                } else {
                    pr.interestFromPeer = true;
                    pr.connection.sendInterested();
                }
                break;
            case Request:
                int pcIndex = msg.getIndex();
                int pcBegin = msg.getBegin();
                int pcLength = msg.getLength();
                //System.out.println("Peer "+ new String(pr.peerId.array()) + " request: index " + pcIndex+"\n");
                Pieces p = pieces.get(pcIndex);
                if(!isSeedingOnly){
                    if(!pr.choking && piecesHad.get(pcIndex))
                    {
                        ByteBuffer data = p.getByteBuffer();
                        uploaded+=pcLength;
                        pr.uploaded+=pcLength;
                        pr.connection.sendPiece(pcIndex, pcBegin, pcLength, data);
                    }
                }else{
                    if(!pr.choking){
                        ByteBuffer data = p.getByteBuffer();
                        uploaded+=pcLength;                        
                        pr.uploaded+=pcLength;
                        pr.connection.sendPiece(pcIndex, pcBegin, pcLength, data);
                    }
                }
                break;
            case Piece: // Piece
                int idx = msg.msg.getInt();
                int begin = msg.msg.getInt();
                msg.msg.compact().flip();
                Pieces pc = pieces.get(idx);
                pr.outstandingRequests--;

                ((ByteBuffer)pc.getByteBuffer().position(begin)).put(msg.msg);
                pc.putSlice(begin / Pieces.SLICE_SIZE);
                pr.downloaded += msg.msg.limit();
                int slice = pc.getNextSlice();
                if (slice == -1) {
                    if (!pc.isLoadingSlices()) {
                        putPiece(pc);
                    }
                } else {
                    pr.connection.sendRequest(idx, pc.getBeginOfSlice(slice), pc.getLengthOfSlice(slice));
                }

                break;

            case Cancel:
                break;
            default:
                // Shouldn't happen...
        }
    }
    
    /**
     * A method that chokes and unchokes peers periodically
     */
    private void updateChokedPeers() {
        
        Peer choked = null;
        for (Peer p : peers.values()) {
            if (!p.choking) {
                p.connection.sendChoke();
                p.choking = true;
                choked = p;
                break;
            }
        }
        // There are no unchoked peers...
        if (choked == null) {
            int i = 3;
            for (Peer p : peers.values()) {
                p.choking = false;
                p.connection.sendUnchoke();
                if (--i == 0) break;
            }
        } else {
            // We choked an unchoked peer, so now unchoke a random peer.
            for (Peer p : peers.values()) {
                if (p.choking && p != choked) {
                    p.choking = false;
                    p.connection.sendUnchoke();
                    return;
                }
            }
            // Since we couldn't find anyone else to unchoke, just unchoke
            // the choked peer again.
            choked.choking = false;
            choked.connection.sendUnchoke();
        }

        if (left == 0) {
            System.out.println("File is already complete.  Seeding.");
        }
    }


    /*
     * THis is a method that stores the pieces we have downloaded for the next downlaod
     * It won't be called if the download is finished
     */

    public void storeTempPieces()
    {
        int totalSize = 0;
        /*This for loop is for calculate the size needed to store all bytes*/
        for(Pieces pc : pieces)
        {
            if(piecesHad.get(pc.getIndex()))
            {
                totalSize = totalSize + 8 + pc.getSize();
            }
        }
        if(totalSize == 0) return;
        
        // start storing stuff here
        
        totalSize+=4; //for the size of torrent file data
        ByteBuffer temp = ByteBuffer.wrap(torrentFileData);
        temp.position(0);
        System.out.println("Storing the torrent file data......."+temp.limit()+"bytes");
        totalSize+=temp.limit();
        totalSize+=4;	// for uploaded
        
        ByteBuffer bf = ByteBuffer.allocate(totalSize);
        bf.position(0);
        bf.putInt(temp.limit());
        bf.put(temp);
        bf.putInt(uploaded);
        int had = 0;
        for(Pieces pc : pieces)
        {
            if(piecesHad.get(pc.getIndex()))
            {
                bf.putInt(pc.getIndex());
                bf.putInt(pc.getSize());
                bf.put(pc.getByteBuffer());
                had++;
            }
        }
        bf.flip();
        String name = "";
        
        for(int i = 0; i<tiObject.file_name.length();i++)
        {
        	if(Character.isLetterOrDigit(tiObject.file_name.charAt(i)))
        	{
        		name+=tiObject.file_name.charAt(i);
        	}else{
        		break;
        	}
        }
        name+="hist.txt";
        
        try{
            File store = new File(name);
            FileChannel channel = new FileOutputStream(store, false).getChannel();
            channel.write(bf);
            channel.close();
            if(had==1)  
                System.out.println("Piece 0 is saved for the next download.");
            else 
                System.out.println("Piece 0~" +(had-1)+" are saved for the next download.");
        }catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }catch(IOException e)
        {
            e.printStackTrace();
        }
    }


    /*
     * This method loads the pieces that were downloaded from previous download
     */
    public static Download loadDownloadHistory(String name)
    {
        URL url = ClassLoader.getSystemResource(name);
        byte[] data = null;
        if(url == null)
        {
            System.out.println("\nNo piece was downloaded before. Starting new download now.");
            return null;
        }
        Path path = Paths.get(url.getPath());
        try{
            data = Files.readAllBytes(path);
    
        }catch(Exception e)
        {
            System.out.println("\nError: Cannot read the Downloaded History!"); 
            return null;
        }

        ByteBuffer history = ByteBuffer.wrap(data);
        if(history.limit()<=0) return null;
        
        
        int torrentFileLength = history.getInt();
        byte[] temp = new byte[torrentFileLength];
        
        
        history.get(temp);
        Download cl = new Download(ByteBuffer.wrap(temp));

        cl.uploaded += history.getInt();

        
        int had = 0;

        while(history.hasRemaining())
        {
            int index = history.getInt();
            int size = history.getInt();
            byte[] piece = new byte[size];
            history.get(piece, 0, size);
            Pieces pc = cl.pieces.get(index);
            ((ByteBuffer)pc.getByteBuffer().position(0)).put(ByteBuffer.wrap(piece));
            cl.putPiece(pc);
            had++;
        }
        if(had==1)  
            System.out.println("\nPiece 0 is loaded from the previous download");
        else 
            System.out.println("\nPiece 0~"+(had-1)+" are loaded from the previous download");
        
        return cl;
    }

public boolean loadHistory(String name)
    {
        URL url = ClassLoader.getSystemResource(name);
        byte[] data = null;
        if(url == null)
        {
            System.out.println("\nNo piece was downloaded before. Starting new download now.");
            return false;
        }
        Path path = Paths.get(url.getPath());
        try{
            data = Files.readAllBytes(path);
    
        }catch(IOException e)
        {
            System.out.println("\nError: Cannot read the Downloaded History!"); 
            return false;
        }

        ByteBuffer history = ByteBuffer.wrap(data);
        if(history.limit()<=0) return false;
        int torrentFileLength = history.getInt();
        byte[] temp = new byte[torrentFileLength];
        history.get(temp);
        uploaded += history.getInt();

        int had = 0;

        while(history.hasRemaining())
        {
            int index = history.getInt();
            int size = history.getInt();
            byte[] piece = new byte[size];
            history.get(piece, 0, size);
            Pieces pc = pieces.get(index);
            ((ByteBuffer)pc.getByteBuffer().position(0)).put(ByteBuffer.wrap(piece));
            putPiece(pc);
            had++;
        }
        if(had==1)  
            System.out.println("\nPiece 0 is loaded from the previous download");
        else 
            System.out.println("\nPiece 0~"+(had-1)+" are loaded from the previous download");
        
        return true;
    }

    /*
     * Send requests to other peers
     */
    private  void processFreePeers() {
        for (Peer p : peers.values()) {
            if (!p.handshook)
                continue;
            if (!p.choked && p.outstandingRequests < 5) {
                Pieces pc = choosePiece(p);
                if (pc == null) { // There's no piece to download from this peer...
                    continue;
                }
                int slice = pc.getNextSlice();
                if (slice != -1) {
                    p.outstandingRequests++;
                    p.connection.sendRequest(pc.getIndex(), pc.getBeginOfSlice(slice), pc.getLengthOfSlice(slice));
                }
            }
        }
    }

    
    /*
    Get the bitfield in current state
    */
    public  ByteBuffer getBitField() {
        synchronized (fileLock) {
            byte[] bf = new byte[(pieces.size() + 8 - 1) / 8]; // Ceiling(pieces.size() / 8)
            for (int i = 0; i < pieces.size(); ++i) {
                bf[i/8] |= (pieces.get(i).getState() == Pieces.TorrentFilePiecesState.COMPLETE) ? 0x80 >> (i % 8) : 0;
            }
            boolean fail = false;
            for (int i = 0; i < pieces.size()/8 && !fail; ++i) {
                fail = (bf[i] != 0);
            }
            if (fail)
                return ByteBuffer.wrap(bf);
            return null;
        }
    }
    
    /*
     * choose a piece for a specified peer
     */
    private  Pieces choosePiece(Peer pr) {

    int[] pieceRanks = new int[pieces.size()];

        for(Pieces piece : pieces) {
            if (piece.getState() == Pieces.TorrentFilePiecesState.INCOMPLETE && pr.canGetPiece(piece.getIndex())) {
                pieceRanks[piece.getIndex()] = 0;
            } else {
                pieceRanks[piece.getIndex()] = -1;
            }
        }

        for (Peer peer : peers.values()) {
            for (Pieces piece : pieces) {
                if (peer.canGetPiece(piece.getIndex()) && pieceRanks[piece.getIndex()] != -1) {
                    pieceRanks[piece.getIndex()]++;
                }
            }
        }

        int leastPieceIndex = -1, leastPieceValue = -1;

        for (int i = 0; i < pieceRanks.length; i++) {
            if (leastPieceIndex == -1 && pieceRanks[i] != -1) {
                leastPieceIndex = i;
                leastPieceValue = pieceRanks[i];
            }
            else if (leastPieceValue != -1 && leastPieceValue > pieceRanks[i] && pieceRanks[i] != -1) {
                leastPieceIndex = i;
                leastPieceValue = pieceRanks[i];
            }
        }
        if (leastPieceIndex == -1)
            return null;

        return pieces.get(leastPieceIndex);        
    }

    /**
     * Write the piece data to the piece buffer
     *
     * @param piece A piece object representation to be added
     * @return whether or not this piece validated.
     */
    public boolean putPiece(Pieces piece) {
        MessageDigest md;
        byte[] sha1 = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            sha1 = md.digest(piece.getByteBuffer().array());
        }
        catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        synchronized (peerLock) {
            synchronized (fileLock) {
                if (Arrays.equals(sha1, piece.getHash())) {
                    fileByteBuffer.position(piece.getIndex() * tiObject.piece_length);
                    fileByteBuffer.put(piece.getByteBuffer());
                    piece.setState(Pieces.TorrentFilePiecesState.COMPLETE);
                    piecesHad.set(piece.getIndex());
                    for (Peer p : peers.values()) {
                        p.connection.sendHave(piece.getIndex());
                        BitSet tmp = ((BitSet)piecesHad.clone());
                        tmp.flip(0, piecesHad.size());
                        if (!tmp.intersects(p.getAvailablePieces())) {
                            p.interestFromPeer = false;
                            p.connection.sendUnInterested();
                        }
                    }
                    // Update stats
                    downloaded += piece.getSize();
                    left -= piece.getSize();
                    int completed = 100*downloaded / (downloaded+left);
                    
                    int count=0;
                    if(completed==0 && count==0){
                    	initiateTime = (int) System.currentTimeMillis();
                    	count=1;
                    }
                    System.out.print("\r" + completed + "% completed    (Type <quit> or <q> to quit the program)");
                    
                    if(completed==100){
                    	finishTime = (int) System.currentTimeMillis();
                    	
  
                    	System.out.println("\t It took "+ milliToMin(initiateTime,finishTime) + " minutes to download the complete file");	
                    }
                    if(downloaded /(downloaded+left) >=1 )
                    {
                        isCompleted = true;
                        System.out.println("Seeding now.	Type <quit> or <q> to quit the program");
                        //running = false;
                    }

                } else {
                    piece.clearSlices();
                    piece.setState(Pieces.TorrentFilePiecesState.INCOMPLETE);
                    return false;
                }
            }
        }
        if (piecesHad.nextClearBit(0) == pieces.size() && !sentComplete) {
            sentComplete = true;
            try {
                announce("Complete", peerId, port, uploaded, downloaded, left, hash);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (BencodingException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    
    private float milliToMin(long initiateTime, long finishTime){
    	return (float)(finishTime - initiateTime)/(float)60000;
    }
    
/**
    Verifies the file and updates what pieces we have.
*/
private void verify() {
    int offset = 0;
    MessageDigest md;
    byte[] sha1;
    try {
        for (Pieces pc : pieces) {
            md = MessageDigest.getInstance("SHA-1");
            ByteBuffer bb = ByteBuffer.allocate(pc.getSize());
            bb.put((ByteBuffer) fileByteBuffer.duplicate().position(offset).limit(offset + pc.getSize())).flip();
            offset += pc.getSize();
            sha1 = md.digest(bb.array());
            if (Arrays.equals(sha1, pc.getHash())) {
                left -= pc.getSize();
                pc.setData(bb);
                pc.setState(Pieces.TorrentFilePiecesState.COMPLETE);
                piecesHad.set(pc.getIndex());
            }
        }
    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
    }
}
    /*
    Generates a peer id that doesn't start with prefix RUBT
     @return A peer id as a string
*/
private String generateId() {
    String peerID = "";
    Random generator;

    /* Bank of appropriate peer ID characters */
    char[] listOfChars = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    /* Initiate random sequence */
    generator = new Random();

    /* ID cannot start with RUBT */
    peerID += listOfChars[generator.nextInt(61)];
    while (peerID.equalsIgnoreCase("RUBT")) {
        peerID = "";
        peerID += listOfChars[generator.nextInt(61)];
    }

    /* Complete random sequence */
    for (int i = 1; i < 20; i++) {
        peerID += listOfChars[generator.nextInt(61)];
       }

    return peerID;
}

/**
    Calculates and creates an arraylist of pieces to be downloaded for a given torrent
    @return An arraylist of pieces
*/
private  ArrayList<Pieces> generatePieces() {
    ArrayList<Pieces> al = new ArrayList<Pieces>();
    int total = tiObject.file_length;
    for (int i = 0; i < tiObject.piece_hashes.length; ++i, total -= tiObject.piece_length) {
        al.add(new Pieces(i, Math.min(total, tiObject.piece_length), tiObject.piece_hashes[i]));
    }
    piecesHad = new BitSet(al.size());
    this.pcTotal = al.size();
    return al;
}
    
    /**
     * Converts InfoHashByteArray to Hax stings
     * @param infoHashByteArray
     * @return hax string
     */
    private  String infoHash(byte[] infoHashByteArray)
    {
        StringBuilder sb= new StringBuilder();
        for (byte b : infoHashByteArray) {
            sb.append(String.format("%%%02X", b));
        }
        return sb.toString();
    }
    
    /*
    a method adding all the messages sent from the connection thread to the arraylist
    */
    public void recvMessage(Message message) {
        this.messages.add(message);
    }

    public void addPeer(Peer pr)
    {
        this.peers.put(pr.peerId, pr);
    }

    /**
     * Send an HTTP GET request to the tracker at the IP address and port
     * specified by the TorrentFile object to obtain Tracker Response Object
     * @param event
     * @param peerId Our peerId
     * @param port Port on which we are listening.
     * @param uploaded Amount we have uploaded
     * @param downloaded Amount we have downloaded
     * @param left Amount we have left to download
     * @param infoHash Info hash of the torrent we want.
     * @return Tracker response, which is a Map object
     * @throws IOException
     * @throws BencodingException
     */
    public Map<ByteBuffer,Object> announce(String event, String peerId, int port, int uploaded, int downloaded, int left, String infoHash) throws IOException, BencodingException {
        URL url = new URL(trackerURL.toString() +
                "?info_hash=" + infoHash +
                "&peer_id=" + peerId +
                "&port=" + port +
                "&uploaded=" + uploaded +
                "&downloaded=" + downloaded +
                "&left=" + left +
                "&event=" + (event != null ? event : "")
        );
        //System.out.println(url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        DataInputStream dis = new DataInputStream(con.getInputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); // Like a baos

        int reads;
        while ((reads = dis.read()) != -1) {
            baos.write(reads);
        }
        dis.close();
        HashMap<ByteBuffer, Object> res = (HashMap<ByteBuffer, Object>) Bencoder2.decode(baos.toByteArray());
        baos.close();
        return res;
    }
    

}
