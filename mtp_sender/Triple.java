
/**
 * Class is used as a holder/collector for entries in the segment stream.
 * @author Charbel Zeaiter.
 */
public class Triple {
    
    // Class Fields /////////////////////////////////////////////////
    private int sequenceNumber;
    private byte[] dataSegment;
    private SegmentStatus status;
    
    // Class Constructor ////////////////////////////////////////////
    
    /**
     * Class Constructor.
     * @param aSequenceNumber Integer, A sequence number.
     * @param aDataSegment byte[], A byte array of data.
     */
    public Triple(int aSequenceNumber, byte[] aDataSegment)
    {
        this.sequenceNumber = aSequenceNumber;
        this.dataSegment = aDataSegment;
        this.status = SegmentStatus.USABLE;
    }
    
    /**
     * Gets the holders sequence number.
     * @return Integer
     */
    public int getSequenceNumber()
    {
        return this.sequenceNumber;
    }
    
    /**
     * Sets the holders sequence number.
     * @param aSeqNum
     */
    public void setSequenceNumber(int aSeqNum)
    {
        this.sequenceNumber = aSeqNum;
    }
    
    /**
     * Gets the holders data/payload.
     * @return byte[]
     */
    public byte[] getDataSegment()
    {
        return this.dataSegment;
    }
    
    /**
     * Gets the status of the holder.
     * @return SegmentStatus
     */
    public SegmentStatus getStatus()
    {
        return this.status;
    }
    
    /**
     * Sets the status of the holder.
     * @param aStatus SegementStatus
     */
    public void setStatus(SegmentStatus aStatus)
    {
        this.status = aStatus;
    }
    
    
}
