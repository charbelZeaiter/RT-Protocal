import java.util.BitSet;

/**
 * This Class is used to run the receivers 'Send' algorithm within it own thread.
 * @author Charbel Zeaiter
 *
 */
public class ReceiverSend implements Runnable{
    
    // Class Fields /////////////////////////////////////////////////
    private Receiver myReceiver;
    
    // Class Constructor ////////////////////////////////////////////
    
    /**
     * Class Constructor.
     * @param aReceiver Receiver, A reference to the 'global' receiver object to update/get values from.
     */
    public ReceiverSend(Receiver aReceiver)
    {
        this.myReceiver = aReceiver;
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * The receivers 'send' algorithm that runs as a thread.
     */
    public void run()
    {   
        // Initialise some algorithm control variables.
        boolean accumulateOn = false;
        int accumulativeAck = 0;
        
        boolean loop = true;
        while(loop)
        {   
            // Output running status to terminal.
            System.out.println("...");
            
            // Make expected sequence number easily accessible.
            int expectedSeqNum = this.myReceiver.ackNumber;
            
            // Get any buffered segments in the buffer.
            QueueFour newQueueFour = this.myReceiver.myReceiveBuffer.peek();
            
            // Check if buffer is empty.
            if(newQueueFour != null)
            {   
                // Buffer not empty.
                // If buffer remains not empty and next sequence number is equal to expected sequence number then loop.
                while( (newQueueFour != null) && (newQueueFour.getSequenceNumber() == expectedSeqNum) )
                {
                    // Update to control variables.
                    accumulateOn = true;
                    int newAckNum = this.myReceiver.myMTPCalculation.getNextSeqNum(newQueueFour.getSequenceNumber(), newQueueFour.getPayLoadByteLength());
                    accumulativeAck = newAckNum;
                    
                    // Write data payload to receivers created file.
                    this.myReceiver.myPrintWriter.print(newQueueFour.getPayLoad());
                    this.myReceiver.myPrintWriter.flush();
                    
                    // Take processed segment out of the buffer and delete.
                    QueueFour popOff = this.myReceiver.myReceiveBuffer.poll();
                    popOff = null;
                    
                    // Update loop control variable to next segment in buffer.
                    QueueFour checkQueueFour = this.myReceiver.myReceiveBuffer.peek();
                    
                    if(checkQueueFour != null)
                    {
                        newQueueFour = checkQueueFour;
                    }
                    
                    // Update other variables accordingly.
                    expectedSeqNum = newAckNum;
                    myReceiver.ackNumber = newAckNum;
                }
                
                // Now if segment processing in the buffered occurred we send an accumulated ack.
                if(accumulateOn)
                {
                    accumulateOn = false;
                    
                    // Create a new Ack segment.
                    BitSet newAccuAckHeaderBits = new BitSet(this.myReceiver.HEADER_BIT_LENGTH);
                    MTPHeader accuAckMTPHeader = new MTPHeader(newAccuAckHeaderBits);
                      
                    accuAckMTPHeader.setACK();                        
                    accuAckMTPHeader.setAckNumber(accumulativeAck);
                    accuAckMTPHeader.setSequenceNumber(myReceiver.sequenceNumber);
                    accuAckMTPHeader.setDestPort(myReceiver.senderHostPort);
                    accuAckMTPHeader.setSRCPort(myReceiver.RECEIVER_HOST_PORT);
                    accuAckMTPHeader.setPayloadByteLength(0);
                    
                    // Lock the thread.
                    this.myReceiver.myLock.lock();
                        
                    try
                    {
                        // Output sender status/event to text log file.
                        this.myReceiver.receiverLogWriter.print("\n\nTIME: "+this.myReceiver.getCurrentSeconds()+"ms");
                        this.myReceiver.receiverLogWriter.print("\nEVENT: SENT Accumulated ACK from send buffer with Seq: "+accuAckMTPHeader.getSequenceNumber());
                        this.myReceiver.receiverLogWriter.print("\nHEADER: SYN="+accuAckMTPHeader.getSYN()+", ACK="+accuAckMTPHeader.getACK()+", SrcPort="+accuAckMTPHeader.getSRCPort()
                                                    +", DestPort="+accuAckMTPHeader.getDestPort()+", SeqNum="+accuAckMTPHeader.getSequenceNumber()
                                                    +", AckNum="+accuAckMTPHeader.getAckNumber()+", MSS="+accuAckMTPHeader.getMSS()+", MWS="+accuAckMTPHeader.getMWS()
                                                    +", Len="+accuAckMTPHeader.getPayloadByteLength());
                        this.myReceiver.receiverLogWriter.print("\nPAYLOAD: NO DATA");
                        this.myReceiver.receiverLogWriter.flush();
                        
                    } finally {
                        //Unlock the thread.
                        this.myReceiver.myLock.unlock();
                    }
                        
                    this.myReceiver.sendMTPSegment(newAccuAckHeaderBits, null);    
                        
                }
                
            }
            
        }
        
    }
    
}
