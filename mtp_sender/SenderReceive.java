import java.util.BitSet;
import java.util.Timer;

/**
 * This Class performs all the senderss 'receive' functions running within its own thread.
 * @author Charbel Zeaiter
 */
public class SenderReceive implements Runnable {
    
    // Class Fields /////////////////////////////////////////////////
    private Sender mySender;
    
    // Class Constructor ////////////////////////////////////////////
    
    /**
     * Class Constructor.
     * @param aSender Sender, A reference to the 'global' sender class to update/get data from.
     */
    public SenderReceive(Sender aSender)
    {
        this.mySender = aSender;
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * The sender 'receive' algorithm function (thread).
     */
    public void run()
    {
        while(this.mySender.connected)
        {
            // Listen for segments.
            byte[] byteSegment = this.mySender.receiveMTPSegment();
            
            // Extract header.
            byte[] headerPart = this.mySender.myMTPCalculation.extractHeader(byteSegment, this.mySender.HEADER_BYTE_SIZE);
            
            // Convert header into BitSet to access bits.
            BitSet receivedHeaderBits = this.mySender.myMTPCalculation.fromByteArray(headerPart);
            MTPHeader receivedMTPHeader = new MTPHeader(receivedHeaderBits);
            
            // Check if ACK flag is set.
            if(receivedMTPHeader.getACK())
            {   
                int ackValue = receivedMTPHeader.getAckNumber();
                
                if(ackValue > this.mySender.sendBase)
                {   
                    
                    // Change status of all 'sent' segments to acked given the accumulated ack received.
                    this.updateSegmentsToAcked(ackValue);
                    
                    // Update new send base.
                    this.mySender.sendBase = ackValue;
                    
                    // Start timer if again if un-acked segments exist.
                    if(this.mySender.timerOn)
                    {   
                        this.mySender.timer.cancel();
                        this.mySender.timerOn = false;
                    }
                    
                    // Check if there are any un-acked segments left.
                    if(this.anyUnAcked())
                    {   
                        // Create new timer.
                        this.mySender.timer = new Timer();
                        this.mySender.timer.schedule(new SenderTimeoutTask(this.mySender), this.mySender.TIMEOUT_VALUE);
                        this.mySender.timerOn = true;
                    }
                    
                    // Lock thread.
                    this.mySender.myLock.lock();
                    
                    try
                    {                    
                        // Output sender status/event to text log file.
                        this.mySender.senderLogWriter.print("\n\nTIME: "+this.mySender.getCurrentSeconds()+"ms");
                        this.mySender.senderLogWriter.print("\nEVENT: RECEIVED ACK segment of Seq: "+receivedMTPHeader.getSequenceNumber());
                        this.mySender.senderLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                                    +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                                    +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                                    +", Len="+receivedMTPHeader.getPayloadByteLength());
                        this.mySender.senderLogWriter.print("\nPAYLOAD: NO DATA");
                        this.mySender.senderLogWriter.flush();
                    
                    } finally {
                        // Unlock thread.
                        this.mySender.myLock.unlock();
                    }
                    
                    // Check if the ack was for the last segment in the stream.
                    if(ackValue >= this.mySender.lastSeqmentSeqNum)
                    {   
                        //If timer is on turn it off.
                        if(this.mySender.timerOn)
                        {
                            this.mySender.timerOn = false;
                            this.mySender.timer.cancel();
                        }
                        
                        // Disconnect from receiver and close socket.
                        this.mySender.connected = false;
                        this.mySender.socketUDP.close();
                        
                        // Output sender status/event to text log file.
                        this.mySender.senderLogWriter.print("\n\nTIME: "+this.mySender.getCurrentSeconds()+"ms");
                        this.mySender.senderLogWriter.print("\n>>> ALL FILES HAVE BEEN TRANSMITED!.....SOCKET CLOSED!");
                        this.mySender.senderLogWriter.flush();
                        
                        // Exit program.
                        System.exit(0);
                    }
                }
                else
                {
                    // Receiving duplicate acks now.
                    
                    // Lock thread.
                    this.mySender.myLock.lock();
                                        
                    try
                    { 
                        // Output sender status/event to text log file.
                        this.mySender.senderLogWriter.print("\n\nTIME: "+this.mySender.getCurrentSeconds()+"ms");
                        this.mySender.senderLogWriter.print("\nEVENT: RECEIVED DUPLICATE ACK segment of Seq: "+receivedMTPHeader.getSequenceNumber());
                        this.mySender.senderLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                                    +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                                    +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                                    +", Len="+receivedMTPHeader.getPayloadByteLength());
                        this.mySender.senderLogWriter.print("\nPAYLOAD: NO DATA");
                        this.mySender.senderLogWriter.flush();
                        
                    } finally {
                        // Unlock thread.
                        this.mySender.myLock.unlock();
                    }
                    
                    // If this is not a duplicate ack then set control varibales.
                    if(!this.mySender.firstDuplicateAck)
                    {
                        this.mySender.firstDuplicateAck = true;
                        this.mySender.lastAck = receivedMTPHeader.getAckNumber();
                        this.mySender.duplicateAckCount++;
                    }
                    else
                    {
                        
                        int currentlyReceivedAck = receivedMTPHeader.getAckNumber();
                        
                        // If ack has been encountered before then increment counter.
                        if(currentlyReceivedAck == this.mySender.lastAck)
                        {
                            this.mySender.duplicateAckCount++;
                            
                            // If counter reaches 3 then execute fast retransmit.
                            if(this.mySender.duplicateAckCount >= 3)
                            {
                                // Fast retransmit.
                                
                                Triple newTriple = this.findSegWithSeq(currentlyReceivedAck);
                                
                                // Creating new MTP header.
                                BitSet newMTPHeaderBitSet = new BitSet(this.mySender.HEADER_BIT_LENGTH);
                                MTPHeader newMTPHeader = new MTPHeader(newMTPHeaderBitSet);
                                
                                // Setting header contents.
                                newMTPHeader.setDestPort(this.mySender.RECEIVER_HOST_PORT);
                                newMTPHeader.setSRCPort(this.mySender.SENDER_HOST_PORT);
                                newMTPHeader.setSequenceNumber(newTriple.getSequenceNumber());
                                newMTPHeader.setAckNumber(this.mySender.ackNumber);
                                
                                int payloadByteSize = newTriple.getDataSegment().length;
                                newMTPHeader.setPayloadByteLength(payloadByteSize);
                                
                                // Lock thread.
                                this.mySender.myLock.lock();
                                
                                try
                                {
                                    // Output sender status/event to text log file.
                                    this.mySender.senderLogWriter.print("\n\nTIME: "+this.mySender.getCurrentSeconds()+"ms");
                                    this.mySender.senderLogWriter.print("\nEVENT: FAST RETRANSMIT ATTEMPT! Segment of Seq: "+newMTPHeader.getSequenceNumber());
                                    this.mySender.senderLogWriter.print("\nHEADER: SYN="+newMTPHeader.getSYN()+", ACK="+newMTPHeader.getACK()+", SrcPort="+newMTPHeader.getSRCPort()
                                                                +", DestPort="+newMTPHeader.getDestPort()+", SeqNum="+newMTPHeader.getSequenceNumber()
                                                                +", AckNum="+newMTPHeader.getAckNumber()+", MSS="+newMTPHeader.getMSS()+", MWS="+newMTPHeader.getMWS()
                                                                +", Len="+newMTPHeader.getPayloadByteLength());
                                    this.mySender.senderLogWriter.print("\nPAYLOAD: "+new String(newTriple.getDataSegment()));
                                    this.mySender.senderLogWriter.flush();
                                    
                                    // Send Segment.
                                    this.mySender.sendMTPSegment(newMTPHeaderBitSet, newTriple.getDataSegment(), true);
                                
                                } finally {
                                    // Unlock thread.
                                    this.mySender.myLock.unlock();
                                }
                                
                                // Reset control variables.
                                this.mySender.duplicateAckCount = 0;
                                this.mySender.firstDuplicateAck = false;
                                
                            }
                            
                        }
                        else
                        {   
                            // Reset control variables.
                            this.mySender.lastAck = currentlyReceivedAck;
                            this.mySender.duplicateAckCount = 0;
                        }
                        
                    }
                    
                }
            
            }
        }

    }
    
    /**
     * Finds the reference of the segment with the specified sequence number.
     * @param aSeqNum Integer, A sequence number.
     * @return Triple, A holder of the segment data.
     */
    private Triple findSegWithSeq(int aSeqNum)
    {
        Triple result = null;
        
        // Loop through looking for match.
        for(Triple element : this.mySender.dataSegmentStream)
        {
            if(element.getSequenceNumber() == aSeqNum)
            {
                result = element;
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Checks if there are any un-acked segments in the segment stream.
     * @return Boolean
     */
    private boolean anyUnAcked()
    {
        boolean result = false;
        
        for(Triple element : this.mySender.dataSegmentStream)
        {
            if(element.getStatus() == SegmentStatus.SENT)
            {
                result = true;
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Updates all segments in segment stream smaller than specified value to 'ACKED' status. 
     * @param aAckValue Integer, Ack Numeber.
     */
    private void updateSegmentsToAcked(int aAckValue)
    {
        for(Triple element : this.mySender.dataSegmentStream)
        {   
            if(element.getStatus() == SegmentStatus.SENT)
            {
                if(element.getSequenceNumber() < aAckValue)
                {
                    element.setStatus(SegmentStatus.ACKED);
                }
            }
        }
        
    }
    
 
}
