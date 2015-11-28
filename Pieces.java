import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Timer;
import java.util.TimerTask;

public class Pieces {
	
	/**
	 * Get the begin position of a slice of a piece
	 * @param slice The slice index
	 * @return The Begin position of this slice
	 */
    public int getBeginOfSlice(int slice) {
        if (slice < 0 || slice >= maxSlices)
            return -1;
        return slice * SLICE_SIZE;
    }

    public int getLengthOfSlice(int slice) {
        if (slice < 0 || slice >= maxSlices)
            return -1;
        return Math.min(SLICE_SIZE,size - (slice * SLICE_SIZE));
    }

    public enum TorrentFilePiecesState{INCOMPLETE, COMPLETE};

    public static final int SLICE_SIZE = 2<<13;
    private int index;
    private int size;
    private byte[] hash;
    private BitSet slices;
    private BitSet loadingSlices;
    private int maxSlices;
    private byte[] data;
    private TorrentFilePiecesState state = TorrentFilePiecesState.INCOMPLETE;
    private Timer loadingTimer;
    private TimerTask[] loadingTasks;

    /**
        @param index piece index
        @param size size of piece
        @param hash SHA hash of piece
     */

    public Pieces(int index, int size, ByteBuffer hash) {
        this.hash = hash.array();
        this.index = index;
        this.size = size;
        this.data = new byte[size];
        this.maxSlices = (size + (SLICE_SIZE) - 1)/(SLICE_SIZE); // Ceiling(size/sliceSize)
        this.slices = new BitSet(maxSlices);
        this.loadingSlices = new BitSet(maxSlices);
        this.loadingTimer = new Timer("Piece " + this.index + " timer", true);
        this.loadingTasks = new TimerTask[this.maxSlices];
        slices.clear();
    }

    //===========Getters & Setters=============//
    
    /**
     * Gets the integer index of the piece
     * 
     * @return The zero based index of this piece relative to other pieces in
     *         the file
     */
    public int getIndex() {
        return index;
    }

    /** 
     * @return The size of the piece in bytes
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the SHA-1 hash of the completed piece as a byte array
     * 
     * @return SHA-1 hash of the piece if completed, otherwise null
     */
    public byte[] getHash() {
        return hash;
    }

    public TorrentFilePiecesState getState() {
        return state;
    }

    public void setData(ByteBuffer bb) {
        bb.get(data);
    }

    public void setState(TorrentFilePiecesState st) {
        state = st;
    }

    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(data);
    }
    
    public int getNextSlice() {
        return getNextSlice(false);
    }

    public int getNextSlice(boolean repeats) {
        int slice = slices.nextClearBit(0);
        if (!repeats) {
            while (loadingSlices.get(slice)) {
                slice = slices.nextClearBit(slice+1);
            }
        }
        // If we've gotten all the pieces, return -1
        if (slice >= maxSlices)
            return -1;

        loadingSlices.set(slice);
        final int sl2 = slice;
        loadingTasks[slice] = new TimerTask() {
            @Override
            public void run() {
                loadingSlices.clear(sl2);
            }
        };
        loadingTimer.schedule(loadingTasks[slice],30000);
        return slice;
    }

    public void putSlice(int idx) {
        slices.set(idx, true);
        loadingSlices.set(idx, false);
        loadingTasks[idx].cancel();
    }

    public void clearSlices() {
        this.slices.clear();
        this.loadingSlices.clear();
    }

    public boolean isLoadingSlices() {
        return !loadingSlices.isEmpty();
    }
}