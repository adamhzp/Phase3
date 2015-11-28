import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;

/**
This is a model that i used to pass messages from connection thread to the main thread
Then, main thread will handle them and decide what message to send to peers
*/
public class Message implements Comparable<Message> {

    @Override
    public int compareTo(Message peerMessage) {
        return peerId.compareTo(peerMessage.peerId);
    }

    public enum MessageType {
        Handshake,
        Choke,          // <length prefix> is 1 and message ID is 0. There is no payload.
        Unchoke,        // <length prefix> is 1 and the message ID is 1. There is no payload.
        Interested,     // <length prefix> is 1 and message ID is 2. There is no payload.
        UnInterested,   // <length prefix> is 1 and message ID is 3. There is no payload.
        Have,           // <length prefix> is 5 and message ID is 4. The payload is a zero-based index of the piece that has just been downloaded and verified.
        Bitfield,       //
        Request,        // <length prefix> is 13 and message ID is 6. The payload is as follows: <index><begin><length>
        Piece,          // <length prefix> is 9+X and message ID is 7. The payload is as follows: <index><begin><block>
        Cancel,         //
    }

    private ByteBuffer peerId;
    private MessageType type;
    public ByteBuffer msg = null;
    public HashMap<String,Object> data;

    // constructor for all other message type
    public Message(ByteBuffer peerId, ByteBuffer msg, MessageType type)
    {
        this.peerId = peerId;
        this.msg = msg;
        this.type = type;
    }

    /**
     * constructor for handshake message
     * @param peerId The peer id of peer who is sending this message
     * @param msg The handshake message as a bytebuffer
     * @return m The handshake message need to be sent to the RUBTClient
     */
    
    public static Message Handshake(ByteBuffer peerId, ByteBuffer msg) {
        Message m = new Message(peerId);
        m.type = MessageType.Handshake;
        m.data.put("bytes",msg);
        return m;
    }

    /**
     * 	constructor for bitfield message
     * @param ip The ip of the sender
     * @param bitfield The bitfield of this Peer
     * @return m The message need to be sent to the RUBTClient
     */
    public static Message Bitfield(ByteBuffer ip, BitSet bitfield) {
        Message m = new Message(ip);
        m.type = MessageType.Bitfield;
        m.data.put("bitfield",bitfield);
        return m;
    }

    /**
     * constructor for request message
     * @param ip The ip of the sender
     * @param index The index of the piece
     * @param begin The begin position in the bytebuffer
     * @param length The length of this piece
     * @return m The message need to be sent to the RUBTClient
     */
    public static Message Request(ByteBuffer ip,int index, int begin, int length) {
        Message m = new Message(ip);
        m.type = MessageType.Request;
        m.data.put("index",index);
        m.data.put("begin",begin);
        m.data.put("length",length);
        return m;
    }

    /**
     * getter for peer id
     * @return peer id
     */
    public ByteBuffer getPeerId() {
        return peerId;
    }

    /**
     * getter for the type of the message
     * @return the type of this message
     */
    public MessageType getType() {
        return type;
    }

    /**
     * getter for the "length"
     * @return the length of the piece
     */
    public int getLength() {
        if (type == MessageType.Request || type == MessageType.Cancel)
            return (Integer) data.get("length");
        return -1;
    }
    /**
     * getter for the index of the piece
     * @return the piece index
     */
    public int getIndex() {
        if (type == MessageType.Have || type == MessageType.Request ||
            type == MessageType.Piece || type == MessageType.Cancel)
            return (Integer) data.get("index");
        return -1;
    }

    
    public int getBegin() {
        if (type == MessageType.Request || type == MessageType.Piece ||
            type == MessageType.Cancel)
            return (Integer) data.get("begin");
        return -1;
    }


    public BitSet getBitfield() {
        if (type == MessageType.Bitfield)
            return (BitSet)data.get("bitfield");
        return null;
    }

    public ByteBuffer getBytes() {
        if (type == MessageType.Piece || type == MessageType.Handshake)
            return (ByteBuffer)data.get("bytes");
        return null;
    }

    private Message(ByteBuffer peerId) {
        this.peerId = peerId;
        data = new HashMap<String, Object>();

    }
}