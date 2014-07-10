import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Timer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This Class is used as the sender and stores all the sender information to be 
 * accessed/modified in a 'global' fashion by other sender associated classes.
 * @author Charbel Zeaiter, z3419481
 */
public class Sender {
    
    // Class Fields /////////////////////////////////////////////////
    public final int SENDER_HOST_PORT;
    public final String RECEIVER_HOST_IP;
    public final int RECEIVER_HOST_PORT;
    public final String FILENAME;
    public final int MAX_WINDOW_SIZE;
    public final int MAX_SEGMENT_SIZE;
    public final int TIMEOUT_VALUE;
    public final float PROB_OF_DROP;
    public final int SEED_VALUE;
    
    public final int HEADER_BIT_LENGTH;
    public final int HEADER_BYTE_SIZE;
    public final int MAX_INT_NUMBER;
    
    public boolean connected;
    public int sequenceNumber;
    public int ackNumber;
    public int sendBase;
    
    public MTPCalculation myMTPCalculation;
    public DatagramSocket socketUDP;
    public ArrayList<Triple> dataSegmentStream;
    public Timer timer;
    public boolean timerOn;
    public PLD myPLDModule;
    public ReentrantLock myLock;
    public boolean firstDuplicateAck;
    public int lastAck;
    public int duplicateAckCount;
    public int lastSeqmentSeqNum;
    public PrintWriter senderLogWriter;
    public long startingSystemTime;
    
    
    public Sender(String aReceiverHostIP, int aReceiverPort, String aFilename,
                  int aMaxWindowSize, int aMaxSegmentSize, int aTimeoutValue,
                  float aProbOfDrop, int aSeedValue)
    {
        // Setting all the field values.
        this.RECEIVER_HOST_IP = aReceiverHostIP;
        this.RECEIVER_HOST_PORT = aReceiverPort;
        this.FILENAME = aFilename;
        this.MAX_WINDOW_SIZE = aMaxWindowSize;
        this.MAX_SEGMENT_SIZE = aMaxSegmentSize;
        this.TIMEOUT_VALUE = aTimeoutValue;
        this.PROB_OF_DROP = aProbOfDrop;
        this.SEED_VALUE = aSeedValue;
        
        this.SENDER_HOST_PORT = 50001;
        
        this.HEADER_BIT_LENGTH = 164;
        this.HEADER_BYTE_SIZE = 21;
        this.MAX_INT_NUMBER = 2147483647;
        
        this.connected = false;
        
        this.myMTPCalculation = new MTPCalculation(this.SEED_VALUE, this.MAX_INT_NUMBER);
        this.myPLDModule = new PLD(this.PROB_OF_DROP, this.SEED_VALUE);
        
        this.timer = new Timer();
        this.timerOn = false;
        
        this.myLock = new ReentrantLock();
        
        this.duplicateAckCount = 0;
        this.firstDuplicateAck = false;
        
        // Create senders text log file..
        try {
            this.senderLogWriter = new PrintWriter("mtp_sender_log.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        this.startingSystemTime = System.currentTimeMillis();
        
        // Creating needed sockets to send and listen on.
        try {
            this.socketUDP = new DatagramSocket(this.SENDER_HOST_PORT);
        } catch (IOException e) {
            // Get current line number.
            int lineNumber = new Exception().getStackTrace()[0].getLineNumber();
            
            // Handle exception.
            this.processException(e, lineNumber);
        }
        
    }
    
    public void handShake()
    {   
        // Output sender status/event to text log file.
        this.senderLogWriter.print("TIME: "+this.getCurrentSeconds()+"ms");
        this.senderLogWriter.print("\nEntering hanshaking mode");
        this.senderLogWriter.flush();
        
        // Output Host State.
        System.out.println("State: CLOSED");
        
        // Creating MTP header.
        BitSet newMTPHeaderBitSet = new BitSet(this.HEADER_BIT_LENGTH);
        MTPHeader newMTPHeader = new MTPHeader(newMTPHeaderBitSet);
        
        // Set Header values.
        newMTPHeader.setSYN();
        newMTPHeader.setDestPort(this.RECEIVER_HOST_PORT);
        newMTPHeader.setSRCPort(this.SENDER_HOST_PORT);
        newMTPHeader.setMSS(this.MAX_SEGMENT_SIZE);
        newMTPHeader.setMWS(this.MAX_WINDOW_SIZE);
        
        // Set initial sequence number bits. 
        int initalSeqNumber = this.myMTPCalculation.getStartingSeqNum();
        this.sequenceNumber = initalSeqNumber;
        newMTPHeader.setSequenceNumber(initalSeqNumber);
        
        // Output sender status/event to text log file.
        this.senderLogWriter.print("\n\nTIME: "+this.getCurrentSeconds()+"ms");
        this.senderLogWriter.print("\nEVENT: Attempting to Send SYN segment of Seq: "+initalSeqNumber);
        this.senderLogWriter.print("\nHEADER: SYN="+newMTPHeader.getSYN()+", ACK="+newMTPHeader.getACK()+", SrcPort="+newMTPHeader.getSRCPort()
                                    +", DestPort="+newMTPHeader.getDestPort()+", SeqNum="+newMTPHeader.getSequenceNumber()
                                    +", AckNum="+newMTPHeader.getAckNumber()+", MSS="+newMTPHeader.getMSS()+", MWS="+newMTPHeader.getMWS()
                                    +", Len="+newMTPHeader.getPayloadByteLength());
        this.senderLogWriter.print("\nPAYLOAD: NO DATA");
        this.senderLogWriter.flush();
        
        // Send initial SYN Segment.
        this.sendMTPSegment(newMTPHeaderBitSet, null, false);
        
        // Output Host State.
        System.out.println("State: SYN_SENT");
        
        // Listen for SYNACK segment.
        byte[] byteSegment = this.receiveMTPSegment();
        
        // Extract Header.
        byteSegment = this.myMTPCalculation.extractHeader(byteSegment, this.HEADER_BYTE_SIZE);
        
        // Create header BitSet to access bits.
        BitSet receivedHeaderBits = this.myMTPCalculation.fromByteArray(byteSegment);
        MTPHeader receivedMTPHeader = new MTPHeader(receivedHeaderBits);
        
        // Check if  SYN and ACK flags are set.
        if(receivedMTPHeader.getSYN() && receivedMTPHeader.getACK())
        {   
            if(receivedMTPHeader.getAckNumber() >= this.sequenceNumber)
            {   
                // Update control varibales.
                this.ackNumber = this.myMTPCalculation.getNextSeqNum(receivedMTPHeader.getSequenceNumber(), 1);
                this.sequenceNumber = this.myMTPCalculation.getNextSeqNum(this.sequenceNumber, 1);
                
                // Output sender status/event to text log file.
                this.senderLogWriter.print("\n\nTIME: "+this.getCurrentSeconds()+"ms");
                this.senderLogWriter.print("\nEVENT: RECEIVED SYN ACK segment of Seq: "+receivedMTPHeader.getSequenceNumber());
                this.senderLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                            +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                            +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                            +", Len="+receivedMTPHeader.getPayloadByteLength());
                this.senderLogWriter.print("\nPAYLOAD: NO DATA");
                this.senderLogWriter.flush();
                
                // Output Host State.
                System.out.println("State: ESTABLISHED");
                
                this.connected = true; 
                
                // Re-using received header.
                receivedMTPHeader.setDestPort(this.RECEIVER_HOST_PORT);
                receivedMTPHeader.setSRCPort(this.SENDER_HOST_PORT);
                receivedMTPHeader.setSequenceNumber(this.sequenceNumber);
                receivedMTPHeader.setAckNumber(this.ackNumber);
                receivedMTPHeader.clearSYN();
                        
                // Output sender status/event to text log file.
                this.senderLogWriter.print("\n\nTIME: "+this.getCurrentSeconds()+"ms");
                this.senderLogWriter.print("\nEVENT: Attempting to Send 3rd segment in handshake of Seq: "+receivedMTPHeader.getSequenceNumber());
                this.senderLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                            +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                            +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                            +", Len="+receivedMTPHeader.getPayloadByteLength());
                this.senderLogWriter.print("\nPAYLOAD: NO DATA");
                this.senderLogWriter.flush();
                
                // Send segment.
                this.sendMTPSegment(receivedHeaderBits, null, false);
                
                // Generate a new stream of data segments from file.
                SegmentStream newSegmentSream = new SegmentStream(this);
                this.dataSegmentStream = newSegmentSream.getDataSegmentArray();
                
                // Setting last Sequence number of segment array.
                this.lastSeqmentSeqNum = newSegmentSream.getLastSegSeqNum();
  
                // Output Host State.
                System.out.println("State: SENT_CONNECTION_GRANTED_SEGMENT");  
            }
        }
        
        
    }
    
    /**
     * Used to enter into data transfer mode after handshake.
     */
    public void exchange()
    {   
        // Output sender status/event to text log file.
        this.senderLogWriter.print("\n\nTIME: "+this.getCurrentSeconds()+"ms");
        this.senderLogWriter.print("\nStarting To Transfer Data!");
        this.senderLogWriter.flush();
        
        // Creating send and receive threads to operate at same time.
        Runnable newSenderSend = new SenderSend(this);
        Runnable newSenderReceive = new SenderReceive(this);
        
        Thread thread1 = new Thread(newSenderSend);
        Thread thread2 = new Thread(newSenderReceive);
        
        // MTP send protocol is started.
        thread1.start();
        thread2.start();
    }
    

    
    // Helper Subroutines ///////////////////////////////////////////
    
    /**
     * Used as a global sending function to send any data the sender or any dependent
     * classes might have.
     * @param aHeaderBitSet BitSet, The set of header bits to send.
     * @param aBytePayLoad byte[], The byte array of payload data to be sent. 
     * @param isDroppable Boolean, If the segment to be sent is droppable or not.
     */
    public void sendMTPSegment(BitSet aHeaderBitSet, byte[] aBytePayLoad, boolean isDroppable)
    {   
        // Check if droppable.
        if(isDroppable)
        {   
            // Check if segment should actually be dropped, based on generated random value.
            if(!this.myPLDModule.getToDrop())
            {   
                // Don't drop and execute send again with drop being false.
                this.sendMTPSegment(aHeaderBitSet, aBytePayLoad, false);

            }
            else
            {   
                //  Drop segment and send nothing.
                
                // Output sender status/event to text log file.
                this.senderLogWriter.print("\n---- Was Dropped!");
                this.senderLogWriter.flush();
            }
                    
        }
        else
        {   
            // If not droppable then just send.
            
            // Convert BitSet header into a byte array;
            byte[] byteHeader = this.myMTPCalculation.toByteArray(aHeaderBitSet);
      
            // Prepare total byte msg.
            byte[] byteMsg = byteHeader;
            
            // Check if payload is being sent also and adjust byte array accordingly.
            if(aBytePayLoad != null)
            {   
                // Combines header byte array and payload byte array.
                byteMsg = this.myMTPCalculation.getFullSegment(byteHeader, aBytePayLoad);
            }
            
            // Try and send segment.
            try 
            {
                
                // Create IP Address reference for segment preparation.
                InetAddress destIP = InetAddress.getByName(this.RECEIVER_HOST_IP);
                
                // Make actual UDP segment.
                DatagramPacket newUDPSegment = new DatagramPacket(byteMsg, byteMsg.length, destIP, this.RECEIVER_HOST_PORT);
                
                if(!this.socketUDP.isClosed())
                {
                    // Send UDP segment.
                    this.socketUDP.send(newUDPSegment);
                    
                    // Output sender status/event to text log file.
                    this.senderLogWriter.print("\n++++ Was Successfully Sent!");
                    this.senderLogWriter.flush();
                }
            }
            catch (IOException e) 
            {
                // Get current line number.
                int lineNumber = new Exception().getStackTrace()[0].getLineNumber();
                
                // Handle exception.
                this.processException(e, lineNumber);
            }
        }
    }
    
    /**
     * Used as a global receiving function to receive any data the sender or any dependent
     * classes might have.
     * @return byte[], The segment byte array received. 
     */
    public byte[] receiveMTPSegment()
    {
        try
        {   
            // Create a datagram packet to hold incoming UDP packets.
            DatagramPacket requestStore = new DatagramPacket(new byte[this.HEADER_BYTE_SIZE], this.HEADER_BYTE_SIZE);
            
            // Receive/Listen for packet.
            this.socketUDP.receive(requestStore);
            
            // Get byte array from transmitted data.
            byte[] byteSegment = requestStore.getData();
            
            return byteSegment;
        }
        catch (IOException e) 
        {
            // Get current line number.
            int lineNumber = new Exception().getStackTrace()[0].getLineNumber();
            
            // Handle exception.
            this.processException(e, lineNumber);
        }
        
        return null;
    }
    
    /**
     * Used to get the current amount of milliseconds that have elapsed since the start of the
     * program.
     * @return Long, The number of ms passed.
     */
    public long getCurrentSeconds()
    {
        return (System.currentTimeMillis() - this.startingSystemTime);
    }
    
    /**
     * Used to process any transmission exceptions that might occur.
     * @param e IOException, The exception object.
     * @param aLineNumber Integer, The line number where the exception occurred.
     */
    private void processException(IOException e, int aLineNumber)
    {
        // Get current line number to isolate catch.
        System.out.println(aLineNumber);
        
        // Print exception message.
        System.out.println(e.getMessage());
        
        // Print stack trace.
        for(StackTraceElement element : e.getStackTrace())
        {
            System.out.println(element);
        }
        
        // System exit.
        System.exit(0);
    }
    
}
