import java.util.BitSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class serves as the timeout for the MTP sender.
 * @author Charbel Zeaiter
 */
public class SenderTimeoutTask extends TimerTask{
    
    // Class Fields /////////////////////////////////////////////////
    private Sender mySender;
    
    // Class Constuctor /////////////////////////////////////////////
    
    /**
     * Class Constructor.
     * @param aSender Sender, A reference to the 'global' sender object to update/get values from.
     */
    public SenderTimeoutTask(Sender aSender)
    {
        this.mySender = aSender;
        this.mySender.timerOn = false;
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * Timer timeout thread.
     */
    public void run()
    {   
        // Lock thread.
        this.mySender.myLock.lock();
        
        try
        {
            Triple minSegment = this.findMinSeqSeg();
            
            // If a min segment exists and connection hasnt been closed.
            if( (minSegment != null) && (!this.mySender.socketUDP.isClosed()) )
            {
                // Creating new MTP header.
                BitSet newMTPHeaderBitSet = new BitSet(this.mySender.HEADER_BIT_LENGTH);
                MTPHeader newMTPHeader = new MTPHeader(newMTPHeaderBitSet);
                
                // Setting header contents.
                newMTPHeader.setDestPort(this.mySender.RECEIVER_HOST_PORT);
                newMTPHeader.setSRCPort(this.mySender.SENDER_HOST_PORT);
                newMTPHeader.setSequenceNumber(minSegment.getSequenceNumber());
                newMTPHeader.setAckNumber(this.mySender.ackNumber);
                
                int payloadByteSize = minSegment.getDataSegment().length;
                newMTPHeader.setPayloadByteLength(payloadByteSize);
                
                // Output sender status/event to text log file.
                this.mySender.senderLogWriter.print("\n\nTIME: "+this.mySender.getCurrentSeconds()+"ms");
                this.mySender.senderLogWriter.print("\nEVENT: TIMEOUT! Attempting to Send Segment of Seq: "+newMTPHeader.getSequenceNumber());
                this.mySender.senderLogWriter.print("\nHEADER: SYN="+newMTPHeader.getSYN()+", ACK="+newMTPHeader.getACK()+", SrcPort="+newMTPHeader.getSRCPort()
                                            +", DestPort="+newMTPHeader.getDestPort()+", SeqNum="+newMTPHeader.getSequenceNumber()
                                            +", AckNum="+newMTPHeader.getAckNumber()+", MSS="+newMTPHeader.getMSS()+", MWS="+newMTPHeader.getMWS()
                                            +", Len="+newMTPHeader.getPayloadByteLength());
                this.mySender.senderLogWriter.print("\nPAYLOAD: "+new String(minSegment.getDataSegment()));
                this.mySender.senderLogWriter.flush();
                
                // Send Segment.
                this.mySender.sendMTPSegment(newMTPHeaderBitSet, minSegment.getDataSegment(), true);
                
                // Start Timer.
                this.mySender.timer = new Timer();
                this.mySender.timer.schedule(new SenderTimeoutTask(this.mySender), this.mySender.TIMEOUT_VALUE);
                this.mySender.timerOn = true;
                
            }
            
        } finally {
            // Unlock thread.
            this.mySender.myLock.unlock();
        }
        
    }
    
    /**
     * Gets the smallest un-acked sequence number.
     * @return Triple, A reference to the segment in the segment holder  stream. 
     */
    private Triple findMinSeqSeg()
    {
        Triple result = null;
        
        for(Triple element : this.mySender.dataSegmentStream)
        {
            if(element.getStatus() == SegmentStatus.SENT)
            {
                result = element;
                break;
            }
        }
        
        return result;
    }
    
}
