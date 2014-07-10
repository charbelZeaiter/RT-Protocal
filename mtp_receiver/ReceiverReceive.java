import java.util.BitSet;

/**
 * This Class performs all the receivers 'receive' functions running within its own thread.
 * @author Charbel Zeaiter
 */
public class ReceiverReceive implements Runnable {
    
    // Class Fields /////////////////////////////////////////////////
    private Receiver myReceiver;
    
    // Class Constructor ////////////////////////////////////////////
    
    /**
     * Class Constructor.
     * @param aReceiver Receiver, A reference to the 'global' receiver class to update/get data from.
     */
    public ReceiverReceive(Receiver aReceiver)
    {
        this.myReceiver = aReceiver;
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * The receiver 'receive' algorithm function (thread).
     */
    public void run()
    {   
        // While the receiver is connected to the sender.
        while(myReceiver.connected)
        {   
            // Listen for segments.
            byte[] byteSegment = this.myReceiver.receiveMTPSegment();
            
            // Extract header.
            byte[] headerPart = this.myReceiver.myMTPCalculation.extractHeader(byteSegment, this.myReceiver.HEADER_BYTE_SIZE);
            
            // Convert header into BitSet to access bits.
            BitSet receivedHeaderBits = this.myReceiver.myMTPCalculation.fromByteArray(headerPart);
            MTPHeader receivedMTPHeader = new MTPHeader(receivedHeaderBits);
            
            // Extract payload.
            int payLoadByteSize = receivedMTPHeader.getPayloadByteLength();
            byte[] payLoadPart = this.myReceiver.myMTPCalculation.extractPayLoad(byteSegment, this.myReceiver.HEADER_BYTE_SIZE, payLoadByteSize);
            int numberOfBytes = payLoadPart.length;
            
            // Convert byte payload to string.
            String payload = new String(payLoadPart);
            
            // Thread, lock next block of code.
            this.myReceiver.myLock.lock();
            
            try
            {
                // Output sender status/event to text log file.
                this.myReceiver.receiverLogWriter.print("\n\nTIME: "+this.myReceiver.getCurrentSeconds()+"ms");
                this.myReceiver.receiverLogWriter.print("\nEVENT: RECEIVED Data Segment of Seq: "+receivedMTPHeader.getSequenceNumber());
                this.myReceiver.receiverLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                            +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                            +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                            +", Len="+receivedMTPHeader.getPayloadByteLength());
                this.myReceiver.receiverLogWriter.print("\nPAYLOAD: "+new String(payLoadPart));
                this.myReceiver.receiverLogWriter.flush();
            
            } finally {
                // Unlock thread.
                this.myReceiver.myLock.unlock();
            }
            
            // Check if the received port is the correct one that was connected to previously.
            if(receivedMTPHeader.getSRCPort() == myReceiver.senderHostPort)
            {   
                int expectedIncommingSeqNum = myReceiver.ackNumber;
                int currentIncommingSeqNum = receivedMTPHeader.getSequenceNumber();
                
                // Check if the current sequence number matches the expected sequence number.
                if(currentIncommingSeqNum == expectedIncommingSeqNum)
                {   
                    // Testing purposes only.
                    //System.out.println("\n---------------HEADER---------------");
                    //System.out.println("Received Sequence Number: "+receivedMTPHeader.getSequenceNumber());
                    //System.out.println("Received Acknowladgement Number: "+receivedMTPHeader.getAckNumber());
                    //System.out.println("Received Source Port: "+receivedMTPHeader.getSRCPort());
                    //System.out.println("Received Destination Port: "+receivedMTPHeader.getDestPort());
                    //System.out.println("Received Payload byte length : "+receivedMTPHeader.getPayloadByteLength());
                    //System.out.println("---------------Payload---------------");
                    //System.out.println("-------------- Payload Byte length: "+numberOfBytes);
                    //System.out.println("'"+payload+"'");
                    
                    // Write payload in received segment to the created file.
                    this.myReceiver.myPrintWriter.print(payload);
                    this.myReceiver.myPrintWriter.flush();
                    
                    // Check the buffer to see if we should wait. So that segments we already have in the buffer can be ACK-ed
                    // instead of sending more requests for segments that we already have.
                    QueueFour newQueueFour = this.myReceiver.myReceiveBuffer.peek();
                    
                    // Check if buffer is empty.
                    if(newQueueFour != null)
                    {   
                        // Get stored sequence number.
                        int storedSeqNum = newQueueFour.getSequenceNumber();
                        
                        // Get next ack number value.
                        int newAckNum = this.myReceiver.myMTPCalculation.getNextSeqNum(receivedMTPHeader.getSequenceNumber(), numberOfBytes);
                        
                        // Update our new expected ack value.
                        this.myReceiver.ackNumber = newAckNum;
                        
                        // Check to see if the stored sequence number matches our expected ack value.
                        if(newAckNum == storedSeqNum)
                        {
                            // Testing purposes only.
                            //System.out.println("-Stored sequence number match next ack number...Waiting for 'ReceiverSend' to process!");
                        }
                        else
                        {   
                            // No match in buffer detected, so we send an ack for the immediate segment.
                            
                            // Recycling received header to be used for MTP ACK.
                            receivedMTPHeader.setACK();
                            receivedMTPHeader.setAckNumber(newAckNum);
                            receivedMTPHeader.setSequenceNumber(myReceiver.sequenceNumber);
                            receivedMTPHeader.setDestPort(myReceiver.senderHostPort);
                            receivedMTPHeader.setSRCPort(myReceiver.RECEIVER_HOST_PORT);
                            receivedMTPHeader.setPayloadByteLength(0);
                            
                            // Send Ack to sender.
                            myReceiver.sendMTPSegment(receivedHeaderBits, null);
                            
                            // Thread, lock next block of code.
                            this.myReceiver.myLock.lock();
                            
                            try
                            {
                                // Output sender status/event to text log file.
                                this.myReceiver.receiverLogWriter.print("\n\nTIME: "+this.myReceiver.getCurrentSeconds()+"ms");
                                this.myReceiver.receiverLogWriter.print("\nEVENT: SENT ACK of Seq: "+receivedMTPHeader.getSequenceNumber());
                                this.myReceiver.receiverLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                                            +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                                            +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                                            +", Len="+receivedMTPHeader.getPayloadByteLength());
                                this.myReceiver.receiverLogWriter.print("\nPAYLOAD: NO DATA");
                                this.myReceiver.receiverLogWriter.flush();
                            
                            } finally {
                                // Unlock thread.
                                this.myReceiver.myLock.unlock();
                            }
                            
                        }

                    }
                    else
                    {
                        // Nothing in buffer to we ack the segment straight away.
                        
                        // Send Ack to sender.
                        myReceiver.sendMTPSegment(receivedHeaderBits, null);
                        
                        // Recycling received header to be used for MTP ACK.
                        receivedMTPHeader.setACK();
                        int newAckNum = this.myReceiver.myMTPCalculation.getNextSeqNum(receivedMTPHeader.getSequenceNumber(), numberOfBytes);
                        receivedMTPHeader.setAckNumber(newAckNum);
                        receivedMTPHeader.setSequenceNumber(myReceiver.sequenceNumber);
                        receivedMTPHeader.setDestPort(myReceiver.senderHostPort);
                        receivedMTPHeader.setSRCPort(myReceiver.RECEIVER_HOST_PORT);
                        receivedMTPHeader.setPayloadByteLength(0);
                        
                        // Send Ack to sender.
                        myReceiver.sendMTPSegment(receivedHeaderBits, null);
                        
                        // Update ack number for new expected sequence number.
                        this.myReceiver.ackNumber = newAckNum;
                        
                        // Thread, lock next block of code.
                        this.myReceiver.myLock.lock();
                        
                        try
                        {
                            // Output sender status/event to text log file.
                            this.myReceiver.receiverLogWriter.print("\n\nTIME: "+this.myReceiver.getCurrentSeconds()+"ms");
                            this.myReceiver.receiverLogWriter.print("\nEVENT: SENT ACK of Seq: "+receivedMTPHeader.getSequenceNumber());
                            this.myReceiver.receiverLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                                        +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                                        +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                                        +", Len="+receivedMTPHeader.getPayloadByteLength());
                            this.myReceiver.receiverLogWriter.print("\nPAYLOAD: NO DATA");
                            this.myReceiver.receiverLogWriter.flush();
                        
                        } finally {
                            // Unlock thread.
                            this.myReceiver.myLock.unlock();
                        }
                        
                    }  
                    
                }
                else if(currentIncommingSeqNum > expectedIncommingSeqNum)
                {   
                    // Current sequence number is greater than expeced sequence number
                    // so it must be out of order. Therefore buffer.
                    
                    // Create a new buffer object and add to buffer.
                    QueueFour newQueueTriple = new QueueFour(currentIncommingSeqNum, receivedHeaderBits, payload, numberOfBytes);
                    myReceiver.myReceiveBuffer.add(newQueueTriple);
                    
                    // Recycling received header to be used for MTP ACK.
                    receivedMTPHeader.setACK();
                    receivedMTPHeader.setAckNumber(expectedIncommingSeqNum);
                    receivedMTPHeader.setSequenceNumber(myReceiver.sequenceNumber);
                    receivedMTPHeader.setDestPort(myReceiver.senderHostPort);
                    receivedMTPHeader.setSRCPort(myReceiver.RECEIVER_HOST_PORT);
                    receivedMTPHeader.setPayloadByteLength(0);
                    
                    // Send the same ack back as before.
                    myReceiver.sendMTPSegment(receivedHeaderBits, null);
                    
                    // Thread, lock next block of code.
                    this.myReceiver.myLock.lock();
                    
                    try
                    {
                        // Output sender status/event to text log file.
                        this.myReceiver.receiverLogWriter.print("\n\nTIME: "+this.myReceiver.getCurrentSeconds()+"ms");
                        this.myReceiver.receiverLogWriter.print("\nEVENT: NOT EXPECTING Sequence number: "+currentIncommingSeqNum+", SENT ACK for expected sequence number in segment of Seq: "+receivedMTPHeader.getSequenceNumber());
                        this.myReceiver.receiverLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                                    +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                                    +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                                    +", Len="+receivedMTPHeader.getPayloadByteLength());
                        this.myReceiver.receiverLogWriter.print("\nPAYLOAD: NO DATA");
                        this.myReceiver.receiverLogWriter.flush();
                    
                    } finally {
                        // Unlock thread.
                        this.myReceiver.myLock.unlock();
                    }
                    
                }
                else if(currentIncommingSeqNum < expectedIncommingSeqNum)
                {   
                    // Send back need sequence number.
                    
                    // Recycling received header to be used for MTP ACK.
                    receivedMTPHeader.setACK();
                    receivedMTPHeader.setAckNumber(expectedIncommingSeqNum);
                    receivedMTPHeader.setSequenceNumber(myReceiver.sequenceNumber);
                    receivedMTPHeader.setDestPort(myReceiver.senderHostPort);
                    receivedMTPHeader.setSRCPort(myReceiver.RECEIVER_HOST_PORT);
                    receivedMTPHeader.setPayloadByteLength(0);
                    
                    // Send Ack to sender.
                    myReceiver.sendMTPSegment(receivedHeaderBits, null);
                    
                    // Thread, lock next block of code.
                    this.myReceiver.myLock.lock();
                    
                    try
                    {
                        // Output sender status/event to text log file.
                        this.myReceiver.receiverLogWriter.print("\n\nTIME: "+this.myReceiver.getCurrentSeconds()+"ms");
                        this.myReceiver.receiverLogWriter.print("\nEVENT: SENT ACK of Seq: "+receivedMTPHeader.getSequenceNumber());
                        this.myReceiver.receiverLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                                    +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                                    +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                                    +", Len="+receivedMTPHeader.getPayloadByteLength());
                        this.myReceiver.receiverLogWriter.print("\nPAYLOAD: NO DATA");
                        this.myReceiver.receiverLogWriter.flush();
                    
                    } finally {
                        // Unlock thread.
                        this.myReceiver.myLock.unlock();
                    }
                }
                
            }
            
            // Force receive algorithm to hand over thread execution to send algorthim.
            try 
            {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
        }
        
    }
    
}
