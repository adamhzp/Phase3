import java.nio.ByteBuffer;
import java.util.BitSet;

public class Peer {

    public boolean handshook = false;
    public boolean interested = false;
    public boolean interestFromPeer = false;
    public boolean choked = true;
    public int outstandingRequests = 0;
    public boolean choking = true;

    public ConnectionToPeer connection;
    public ByteBuffer peerId;
    private BitSet availablePieces = new BitSet();
    public int downloaded = 0;
    public int uploaded = 0;
    public int startTime = 0;
    private Download dl = null;

    /**
     * Constructor Peer object
     * @param peerId The peer id for this peer object
     * @param peerConnection Peer Connection with the specified ip and port
     */
    public Peer(ByteBuffer peerId, ConnectionToPeer peerConnection, int pieces, Download dl) {
        this.peerId = peerId.duplicate();
        this.connection = peerConnection;
        this.dl = dl;
    }

    /**
     * Set the available pieces from the bitfield
     * @param availablePieces
     */
    public void setAvailablePieces(BitSet availablePieces) {
        this.availablePieces = availablePieces;
    }

    /**
     * @return What pieces are available from this peer
     */
    
    public BitSet getAvailablePieces() {
        return availablePieces;
    }

    /**
     * Set the piece of index is available from this peer
     * @param index Index of the available piece
     */
    public void setPieceAvailable(int index) {
        this.availablePieces.set(index);  
    }

    public double getPerformance(int endTime)
    {
        double pfmc = 0;

        if(this.uploaded == 0 && this.downloaded != 0)
        {
            pfmc = ((double)this.downloaded)/((double)(endTime - this.startTime));
            return pfmc;
        }
        else if(this.uploaded != 0 && this.downloaded ==0)
        {
            pfmc = 0-((double)this.uploaded)/((double)(endTime - this.startTime));
            return pfmc;
        }
        else{
            //need to be modified
            return pfmc;
        }

    }

    /**
        Used to know if a peer can provide a certain piece
        @param index Index of a piece relative to a torrent
        @return Whether or not the peer has the piece
     */
    public boolean canGetPiece(int index) {
        try {
            return availablePieces.get(index);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        }
    }
}
