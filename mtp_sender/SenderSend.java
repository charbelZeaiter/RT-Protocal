import java.util.BitSet;
import java.util.Timer;

/**
 * This Class is used to run the senders 'Send' algorithm within it own thread.
 * @author Charbel Zeaiter
 *
 */
public class SenderSend implements Runnable{
    
    // Class Fields /////////////////////////////////////////////////
    private Sender mySender;
    
    // Class Consturctor ////////////////////////////////////////////
    
    /**
     * Class Consructor.
     * @param aSender Sender, A reference to the 'global' sender object to update/get values from.
     */
    public SenderSend(Sender aSender)
    {
        this.mySender = aSender;
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * The senders 'send' algorithm that runs as a thread.
     */
    public void run()
    {   
        // Set up algorithm control values.
        int index = 0;
        int streamLength = this.mySender.dataSegmentStream.size();
        this.mySender.sendBase = this.mySender.dataSegmentStream.get(index).getSequenceNumber();;
        this.mySender.sequenceNumber = this.mySender.dataSegmentStream.get(index).getSequenceNumber();
        int n = mySender.MAX_SEGMENT_SIZE*mySender.MAX_WINDOW_SIZE;
        
        // Keep looping until all segments have been transmitted.
        boolean transmittedAll = false;
        while(!transmittedAll)
        {   
            // Check if all segments have been transmitted.
            if( (index >= streamLength) || (this.mySender.socketUDP.isClosed()))
            {
                transmittedAll = true;
                
            }
            else
            {   
                
                if(this.mySender.sequenceNumber< (this.mySender.sendBase+n) )
                {
                    
                    // Creating new MTP header.
                    BitSet newMTPHeaderBitSet = new BitSet(this.mySender.HEADER_BIT_LENGTH);
                    MTPHeader newMTPHeader = new MTPHeader(newMTPHeaderBitSet);
                    
                    // Setting header contents.
                    newMTPHeader.setDestPort(this.mySender.RECEIVER_HOST_PORT);
                    newMTPHeader.setSRCPort(this.mySender.SENDER_HOST_PORT);
                    newMTPHeader.setSequenceNumber(this.mySender.sequenceNumber);
                    newMTPHeader.setAckNumber(this.mySender.ackNumber);
                    
                    byte[] bytePayLoad = this.mySender.dataSegmentStream.get(index).getDataSegment();
                    int payloadByteSize = bytePayLoad.length;
                    newMTPHeader.setPayloadByteLength(payloadByteSize);
                    
                    // Lock thread.
                    this.mySender.myLock.lock();
                    
                    try{
                        
                        if(!this.mySender.socketUDP.isClosed())
                        {
                           
                            // Output sender status/event to text log file.
                            this.mySender.senderLogWriter.print("\n\nTIME: "+this.mySender.getCurrentSeconds()+"ms");
                            this.mySender.senderLogWriter.print("\nEVENT: Attempting to Send Segment of Seq: "+newMTPHeader.getSequenceNumber());
                            this.mySender.senderLogWriter.print("\nHEADER: SYN="+newMTPHeader.getSYN()+", ACK="+newMTPHeader.getACK()+", SrcPort="+newMTPHeader.getSRCPort()
                                                        +", DestPort="+newMTPHeader.getDestPort()+", SeqNum="+newMTPHeader.getSequenceNumber()
                                                        +", AckNum="+newMTPHeader.getAckNumber()+", MSS="+newMTPHeader.getMSS()+", MWS="+newMTPHeader.getMWS()
                                                        +", Len="+newMTPHeader.getPayloadByteLength());
                            this.mySender.senderLogWriter.print("\nPAYLOAD: "+new String(bytePayLoad));
                            this.mySender.senderLogWriter.flush();
                            
                            // Send Segment.
                            this.mySender.sendMTPSegment(newMTPHeaderBitSet, bytePayLoad, true);
                        }
                        
                    } finally {
                        // Unlock thread.
                        this.mySender.myLock.unlock();
                    }
                        
                    // Change Status of Segment in stream.
                    this.mySender.dataSegmentStream.get(index).setStatus(SegmentStatus.SENT);
                    
                    if(this.mySender.sendBase == this.mySender.sequenceNumber)
                    {
                        // Start Timer. 
                        this.mySender.timer = new Timer();
                        this.mySender.timer.schedule(new SenderTimeoutTask(this.mySender), this.mySender.TIMEOUT_VALUE);
                        this.mySender.timerOn = true;
                    }
                    
                    index++;
                    if(index < streamLength)
                    {
                        this.mySender.sequenceNumber = this.mySender.dataSegmentStream.get(index).getSequenceNumber(); 
                    }
                    
                }
            }
        }
        
    
    }
    
}
