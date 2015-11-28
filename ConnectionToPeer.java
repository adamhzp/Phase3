import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
This is a class that runs a thread to build sockets to peers.
Recieve and send message to peers
*/
public class ConnectionToPeer implements Runnable {
    public static final byte[] PROTOCOL_HEADER = new byte[]{'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l'};
    private final byte[] KEEP_ALIVE = new byte[]{0, 0, 0, 0};
    private final byte[] CHOKE = new byte[]{0, 0, 0, 1, 0};
    private final byte[] UNCHOKE = new byte[]{0, 0, 0, 1, 1};
    private final byte[] INTERESTED = new byte[]{0, 0, 0, 1, 2};
    private final byte[] UNINTERESTED = new byte[]{0, 0, 0, 1, 3};
    private final byte[] HAVE = new byte[]{0, 0, 0, 5, 4};
    private final byte[] BITFIELD = new byte[]{0, 0, 0, 0, 5};
    private final byte[] REQUEST = new byte[]{0, 0, 0, 13, 6};
    @SuppressWarnings("unused")
    private final byte[] PIECE = new byte[]{0, 0, 0, 0, 7};

    private Socket socket;
    public String ip;
    private int port;
    public boolean running = true;
    private Timer keepaliveTimer;
    private boolean handshakeDone = false;
    private ByteBuffer peerId = null;
    private boolean isIncomingConnection = false;


    private ByteBuffer selfInfoHash = null;
    private ByteBuffer selfPeerId = null;
    private int pieces = 0;
    private ConcurrentLinkedQueue<ByteBuffer> messages = new ConcurrentLinkedQueue<ByteBuffer>();
    private byte[] buffer = new byte[2<<14];


    /**
        Creates a new ConnectionToPeer, but does not start it.
        @param t The torrent object that messages are to be passed to
        @param ip The ip address of the peer to connect to
        @param port The port of the peer to connect to
        @param peerId The peerId of the peer to connect to
    */

    public ConnectionToPeer(String ip, int port, ByteBuffer peerId, ByteBuffer infoHash, ByteBuffer id) {
        this.ip = ip;
        this.peerId = peerId.duplicate();
        this.port = port;
        this.selfInfoHash = infoHash.duplicate();
        this.selfPeerId = id.duplicate();
        this.keepaliveTimer = new Timer(ip + " keepaliveTimer", true);
    }


    /**
     * This is a constructor added in Phase 2. It's for incoming connection.
     * @param incomingConnection The socket that is built in Listener class
     * @param infoHash The info hash extract from the torrent
     * @param id The peer id of RUBTClient
     * @param pieces The total number of pieces 
     */

    public ConnectionToPeer(Socket incomingConnection, ByteBuffer infoHash, ByteBuffer id, int pieces)
    {

        socket = incomingConnection;
        this.port = socket.getPort();
        this.isIncomingConnection = true;
        this.ip = socket.getRemoteSocketAddress().toString();
        this.selfInfoHash = infoHash.duplicate();
        this.selfPeerId = id.duplicate();
        this.pieces = pieces;
        this.keepaliveTimer = new Timer(ip + " keepaliveTimer", true);

        System.out.println("\t connected to incomming: "+this.ip);
    }


    /**
        Peer thread run loop. Responsible for sending messages when they are ready to be sent and receiving and reassembling messages.
     */

    @SuppressWarnings("resource")
    @Override
    public void run() {
        try {
            if(!isIncomingConnection){      //if it's a incoming connection, the socket is built in the listener class
                System.out.println("connected to " + ip);
                socket = new Socket(ip, port);  
            }
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            int len;
            ByteBuffer writingBuffer = ByteBuffer.wrap(buffer);
            writingBuffer.position(0);
            /*
                The TCP stream comes coherently, so this loop reasesmble messages.
                once the bytebuffer is large enough to be a message, it creates a message object and send it to the main thread
                handshaking message is different protocal so it has to be handled differently
            */
            while (running) {
                Thread.sleep(10);
                ByteBuffer msg = messages.poll();
                try {
                    if (msg != null) { // We have a message to send.
                        outputStream.write(msg.array());
                    }
                } catch (Exception e) {
                    messages.clear();
                    System.out.println("rebuilding socket to " +this.ip);
                    try{
                    	socket = new Socket(ip, port);
                    }catch(Exception ex)
                    {
                    	System.err.println("Lost connection to "+ this.ip);
                    }
                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();
                    writingBuffer.position(0);
                }
                if (inputStream.available() > 0) { // We have bytes to read...
                    byte[] tbuf = new byte[1024];
                    len = inputStream.read(tbuf);
                    writingBuffer.put(tbuf, 0, len);
                }
                if (writingBuffer.position() <= 4) continue; // Not enough to do anything yet...
                if (!handshakeDone) {
                    if (writingBuffer.position() >= 68) { // handshake message is 68 bytes
                        int ol = writingBuffer.position();
                        writingBuffer.position(68).flip(); // Grab the first 68 bytes.
                        ByteBuffer msgBuf = ByteBuffer.allocate(68);
                        msgBuf.put(writingBuffer);
                        msgBuf.flip();
                        msgBuf.position(0);
                        writingBuffer.limit(ol);
                        writingBuffer.compact();

                        if(!isIncomingConnection){
                            // Pass the message to the Peer object.
                            Message peerMessage = processHandshake(msgBuf);
                            if (peerMessage != null) {
                                RUBTClient.recvMessage(peerMessage);
                            }
                            handshakeDone = true;
                        }
                        else{
                            //this is for incoming connection, get the peer id, send a handshake and create a new Peer obj
                            ByteBuffer temp = ByteBuffer.allocate(68);
                            temp.put(msgBuf);

                            //Taking the peer Id and create the peer obj
                            msgBuf.flip();
                            msgBuf.position(48);
                            byte[] pid = new byte[20];
                            msgBuf.get(pid,0, 20);
                            this.peerId = ByteBuffer.wrap(pid);
                            sendHandshake(this.selfInfoHash, this.selfPeerId);

                            Peer newP = new Peer(this.peerId, this, this.pieces);
                            newP.handshook = false;
                            temp.flip();
                            temp.position(0);

                            //add the peer to RUBTClient thread
                            RUBTClient.addPeer(newP);
                            Message peerMessage = processHandshake(temp);
                            if (peerMessage != null) {
                                RUBTClient.recvMessage(peerMessage);
                            }
                            
                            handshakeDone = true;
                        }

                    }
                } else {        //handling any other message other than handshake
                    while (writingBuffer.position() >= 4 && writingBuffer.position() >= (len = ByteBuffer.wrap(buffer).getInt()+4)) { // We have a full message now!
                        ByteBuffer msgBuf;
                        int ol = writingBuffer.position();
                        writingBuffer.position(len).flip();
                        msgBuf = ByteBuffer.allocate(len);
                        msgBuf.put(writingBuffer);
                        msgBuf.flip();

                        writingBuffer.limit(ol);
                        writingBuffer.compact();

                        Message peerMessage = processMessage(msgBuf);
                        if (peerMessage != null) {
                            RUBTClient.recvMessage(peerMessage);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } 
        catch(ConnectException e) {
            System.out.println("ConnectException");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (NullPointerException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
        Convert a handshake network message into a Message
        handshake message is different so it's handled seperately
        @param msg A ByteBuffer containing the handshake message
        @return A Message representing a handshake.
    */
    private Message processHandshake(ByteBuffer msg) {
        return Message.Handshake(peerId, msg);
    }

    /**
        Convert a network message into a Message to send back to the main thread
        @param msg A ByteBuffer containing the message
        @return A Message object.  Keep-Alive messages will be ignored.
     */
    private Message processMessage(ByteBuffer msg) {
        int len = msg.getInt();
        if (len == 0) // ignore keep alive message
            return null;
        byte type = msg.get();
        switch (type) {
            case 0: // Choke
                return new Message(peerId, msg, Message.MessageType.Choke);
            case 1: // Unchoke
                return new Message(peerId, msg, Message.MessageType.Unchoke);
            case 2: // Interested
                return new Message(peerId, msg, Message.MessageType.Interested);
            case 3: // UnInterested
                return new Message(peerId, msg, Message.MessageType.UnInterested);
            case 4: // Have
                msg.compact();
                msg.flip();
                return new Message(peerId, msg, Message.MessageType.Have);
            case 5: // Bitfield
                len -= 1;
                BitSet pcs = new BitSet(len*8);
                byte b = 0;
                // Turn the bitfield into a BitSet
                for (int j = 0; j < len * 8; ++j) {
                    if (j % 8 == 0) b = msg.get();
                    pcs.set(j, ((b << (j % 8)) & 0x80) != 0);
                }
                return Message.Bitfield(peerId, pcs);
            case 6: // Request
                int idx = msg.getInt();
                int begin = msg.getInt();
                int length = msg.getInt();
                return Message.Request(peerId, idx, begin, length);
            case 7: // Piece
                msg.compact();
                msg.flip();
                return new Message(peerId, msg, Message.MessageType.Piece);
            default:
                return null;
        }
    }

    private TimerTask tsk = null;
    /**
        Updates the keepalive timer. Called every time a message is queued.
    */
    private void resetKeepAlive() {
        if (tsk != null) {
            tsk.cancel();
            keepaliveTimer.purge();
        }
        tsk = new TimerTask() {
            @Override
            public void run() {
                messages.add(ByteBuffer.wrap(KEEP_ALIVE));
                resetKeepAlive();
            }
        };
        keepaliveTimer.schedule(tsk, 120000);
    }

    /**
        add any message to the queue and all these messages will be sent in the run() inside the while loop
        @param msg message to add to the queue
        @return Whether or not the message was successfully added.
     */
    private boolean sendMessage(ByteBuffer msg) {
        resetKeepAlive();
        return messages.add(msg);
    }

    /**
        Sends Choke message
        @return whether or not the message was queued to be sent
    */
    public boolean sendChoke() {
        return sendMessage(ByteBuffer.wrap(CHOKE));
    }

    /**
        Sends Unchoke message
        @return whether or not the message was queued to be sent
    */
    public boolean sendUnchoke() {
        return sendMessage(ByteBuffer.wrap(UNCHOKE));
    }

    /**
     *  send the piece to other peers
     *  @return whether or not the message was queue to be sent
     */
    public boolean sendPiece(int index, int begin, int length, ByteBuffer data)
    {
        
        ByteBuffer bb = ByteBuffer.allocate(13+length);

        bb.put(PIECE);
        bb.putInt(0, 9+length);
        bb.putInt(index);
        bb.putInt(begin);
        bb.put(data.array(),begin, length);
        bb.flip();

        return sendMessage(bb);

    }



    /**
        Sends Have message
        @param index index of the piece that i
        @return whether or not the message was queued to be sent
    */
    public boolean sendHave(int index) {
        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.put(HAVE);
        bb.putInt(index);
        bb.flip();
        return sendMessage(bb);
    }

    /**
        Sends Request message
        @param index index of the piece being requested
        @param begin beginning index of the piece data
        @param length number of bytes of the piece requested
        @return whether or not the message was queued to be sent
     */
    public boolean sendRequest(int index, int begin, int length) {
        ByteBuffer bb = ByteBuffer.allocate(17);
        bb.put(REQUEST);
        bb.putInt(index);
        bb.putInt(begin);
        bb.putInt(length);
        bb.flip();
        return sendMessage(bb);
    }

    /**
        Sends Interested message
        @return whether or not the message was queued to be sent
    */
    public boolean sendInterested() {
        return sendMessage(ByteBuffer.wrap(INTERESTED));
    }

    /**
        Sends UnInterested message
        @return whether or not the message was queued to be sent
     */
    public boolean sendUnInterested() {
        return sendMessage(ByteBuffer.wrap(UNINTERESTED));
    }

    /**
        Sends Bitfield message
        @param bitfield Bytebuffer containing bitfield data
        @return whether or not the message was queued to be sent
    */
    public boolean sendBitfield(ByteBuffer bitfield) {
        ByteBuffer bb = ByteBuffer.allocate(bitfield.limit()+5);
        bb.put(BITFIELD);
        bb.putInt(0, bitfield.limit() + 1);
        bb.put(bitfield);
        bb.flip();
        return sendMessage(bb);
    }

    /**
     * Sends Handshake message to peer
     * @param infoHash ByteBuffer representation of infoHash of the torrent for which we are connecting
     * @param peerId ByteBuffer representation of peerId
     * @return Boolean value to see whether or not the message was queued to be sent
     */
    public boolean sendHandshake(ByteBuffer infoHash, ByteBuffer peerId) {
        ByteBuffer handshakeBuffer = ByteBuffer.allocate(68); // 49 bytes + sizeof PROTOCOL_HEADER
        infoHash.position(0);
        peerId.position(0);

        handshakeBuffer.put((byte) 19);
        handshakeBuffer.put(PROTOCOL_HEADER);
        handshakeBuffer.putInt(0); // Two ints are 8 bytes
        handshakeBuffer.putInt(0);
        handshakeBuffer.put(infoHash);
        handshakeBuffer.put(peerId);

        return sendMessage(handshakeBuffer);
    }

}