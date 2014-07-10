import java.util.BitSet;

/**
 * This Class is used to store all the buffered segments and related data as
 * one piece. (Used in the receivers 'out of order' segment buffer).
 * @author Charbel Zeaiter, z3419481
 */
public class QueueFour implements Comparable<QueueFour> {
    
    // Class Fields /////////////////////////////////////////////////
    private int sequenceNumber;
    private BitSet header;
    private String payLoad;
    private int payLoadByteLength;
    
    // Class Contructor /////////////////////////////////////////////
    
    /**
     * Class Constructor.
     * @param aSequenceNumber Integer, The sequence number of the segment.
     * @param aHeader BitSet, The associated bit header of the segment.
     * @param aPayload String, The associated data payload of the segment. 
     * @param aPayloadByteLength Integer, The payload byte length of the segment. 
     */
    public QueueFour(int aSequenceNumber, BitSet aHeader, String aPayload, int aPayloadByteLength)
    {
        this.sequenceNumber = aSequenceNumber;
        this.header = aHeader;
        this.payLoad = aPayload;
        this.payLoadByteLength = aPayloadByteLength;
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * Gets to sequence number of the buffered entry.
     * @return Integer
     */
    public int getSequenceNumber()
    {
        return this.sequenceNumber;
    }
    
    /**
     * Gets the header bits of the buffered entry.
     * @return BitSet
     */
    public BitSet getHeader()
    {
       return this.header; 
    }
    
    /**
     * Gets the payload of the buffered entry.
     * @return String
     */
    public String getPayLoad()
    {
       return this.payLoad; 
    }
    
    /**
     * Gets the payoad byte length of the buffered entry.
     * @return Integer
     */
    public int getPayLoadByteLength()
    {
       return this.payLoadByteLength; 
    }
    
    @Override
    public int compareTo(QueueFour aQueueTuple)
    {
        if(this.sequenceNumber > aQueueTuple.sequenceNumber)
        {
            return 1;
        }
        else if(this.sequenceNumber < aQueueTuple.sequenceNumber)
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }
    
}
