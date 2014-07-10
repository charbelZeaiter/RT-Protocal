import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.BitSet;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This Class is used as the receiver and stores all the receiver information to be 
 * accessed/modified in a 'global' fashion by other receiver associated classes.
 * @author Charbel Zeaiter, z3419481
 */
public class Receiver {
    
    // Class Fields /////////////////////////////////////////////////
    
    public final int HEADER_BYTE_SIZE;
    public final int HEADER_BIT_LENGTH;
    public final int RECEIVER_HOST_PORT;
    public final int SEED_VALUE;
    public final int MAX_INT_NUMBER;
    public final String FILENAME;
    public boolean connected;
    public int senderHostPort;
    public final String CLIENT_HOST_IP;
    public int sequenceNumber;
    public int ackNumber;
    public int mss;
    public int mws;
    public MTPCalculation myMTPCalculation;
    public DatagramSocket socketUDP;
    public PriorityQueue<QueueFour> myReceiveBuffer;
    public ReentrantLock myLock;
    public PrintWriter myPrintWriter;
    public PrintWriter receiverLogWriter;
    public long startingSystemTime;
    
    // Class Constructor ////////////////////////////////////////////
    
    /**
     * Constructor 
     * @param aReceiverPort Integer, Port used to listen and send on.
     * @param aFilename String, The Filename of received file.
     */
    public Receiver(int aReceiverPort, String aFilename)
    {   
        // Set all default values & create default objects.
        this.RECEIVER_HOST_PORT = aReceiverPort;
        this.FILENAME = aFilename;
        this.HEADER_BIT_LENGTH = 164;
        this.HEADER_BYTE_SIZE = 21;
        this.MAX_INT_NUMBER = 2147483647;
        this.SEED_VALUE = 50;
        this.CLIENT_HOST_IP = "localhost";
        this.connected = false;
        
        this.myMTPCalculation = new MTPCalculation(this.SEED_VALUE, this.MAX_INT_NUMBER);
        this.myReceiveBuffer = new PriorityQueue<QueueFour>();
        this.myLock = new ReentrantLock();
        
        // Creating a UDP socket to send and listen on.
        try
        { 
            this.socketUDP = new DatagramSocket(this.RECEIVER_HOST_PORT);
        } catch (IOException e) {
            // Get current line number.
            int lineNumber = new Exception().getStackTrace()[0].getLineNumber();
                
            // Handle exception.
            this.processException(e, lineNumber);
        }
        
        // Create receivers text log file reference.
        try 
        {
            this.receiverLogWriter = new PrintWriter("mtp_receiver_log.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        // Get program starting time.
        this.startingSystemTime = System.currentTimeMillis();
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * Executes the receivers handshaking mode to be ready to handshake with a
     * sender and establish a connection.
     */
    public void handShakeReceive()
    {   
        // Output sender status/event to text log file.
        this.receiverLogWriter.print("TIME: "+this.getCurrentSeconds()+"ms");
        this.receiverLogWriter.print("\nEntering hanshaking mode");
        this.receiverLogWriter.flush();
        
        // Output Host State.
        System.out.println("State: SYN_WAIT");
        
        byte[] byteSegment = this.receiveMTPSegment();
        
        // Extract Header.
        byteSegment = this.myMTPCalculation.extractHeader(byteSegment, this.HEADER_BYTE_SIZE);
        
        // Create header BitSet to access bits.
        BitSet receivedHeaderBits = this.myMTPCalculation.fromByteArray(byteSegment);
        MTPHeader receivedMTPHeader = new MTPHeader(receivedHeaderBits);
            
        // Check to see if SYN bit is set.
        if(receivedMTPHeader.getSYN())
        {
            // Update receiver with appropriate configuration values from sender.
            this.senderHostPort = receivedMTPHeader.getSRCPort();
            this.ackNumber = this.myMTPCalculation.getNextSeqNum(receivedMTPHeader.getSequenceNumber(), 1);
            this.mss = receivedMTPHeader.getMSS();
            this.mws = receivedMTPHeader.getMWS();
            this.sequenceNumber = this.myMTPCalculation.getStartingSeqNum();
            
            // Output sender status/event to text log file.
            this.receiverLogWriter.print("\n\nTIME: "+this.getCurrentSeconds()+"ms");
            this.receiverLogWriter.print("\nEVENT: RECEIVED SYN segment of Seq: "+receivedMTPHeader.getSequenceNumber());
            this.receiverLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                        +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                        +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                        +", Len="+receivedMTPHeader.getPayloadByteLength());
            this.receiverLogWriter.print("\nPAYLOAD: NO DATA");
            this.receiverLogWriter.flush();
            
            // Set Receiver to connected status.
            this.connected = true;
            
            // Output Host State.
            System.out.println("State: ESTABLISHED");    
            
            // Re-using received header.
            receivedMTPHeader.setDestPort(this.senderHostPort);
            receivedMTPHeader.setSRCPort(this.RECEIVER_HOST_PORT);
            receivedMTPHeader.setSequenceNumber(this.sequenceNumber);
            receivedMTPHeader.setAckNumber(this.ackNumber);
            receivedMTPHeader.setACK();
            
            // Send back SYNACK segment.
            this.sendMTPSegment(receivedHeaderBits, null);
            
            // Output sender status/event to text log file.
            this.receiverLogWriter.print("\n\nTIME: "+this.getCurrentSeconds()+"ms");
            this.receiverLogWriter.print("\nEVENT: SENT SYN ACK segment of Seq: "+receivedMTPHeader.getSequenceNumber());
            this.receiverLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                        +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                        +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                        +", Len="+receivedMTPHeader.getPayloadByteLength());
            this.receiverLogWriter.print("\nPAYLOAD: NO DATA");
            this.receiverLogWriter.flush();
            
            // Create Text file to hold text data.
            try {
                this.myPrintWriter = new PrintWriter(this.FILENAME, "UTF-8");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            
            // Receive final ACK for Connection-Granted segment.
            byteSegment = this.receiveMTPSegment(); 
            
            // Extract Header.
            byteSegment = this.myMTPCalculation.extractHeader(byteSegment, this.HEADER_BYTE_SIZE);
            
            // Create header BitSet to access bits.
            receivedHeaderBits = this.myMTPCalculation.fromByteArray(byteSegment);
            receivedMTPHeader = new MTPHeader(receivedHeaderBits);
            
            // Check to see if this is the 3rd segment in the handshake.
            if(!receivedMTPHeader.getSYN() && receivedMTPHeader.getACK())
            {    
                // Check if the segment comes from the correct process.
                if(this.senderHostPort == receivedMTPHeader.getSRCPort() && this.connected)
                {   
                    // Output sender status/event to text log file.
                    this.receiverLogWriter.print("\n\nTIME: "+this.getCurrentSeconds()+"ms");
                    this.receiverLogWriter.print("\nEVENT: RECEIVED 3rd segment in handshake of Seq: "+receivedMTPHeader.getSequenceNumber());
                    this.receiverLogWriter.print("\nHEADER: SYN="+receivedMTPHeader.getSYN()+", ACK="+receivedMTPHeader.getACK()+", SrcPort="+receivedMTPHeader.getSRCPort()
                                                +", DestPort="+receivedMTPHeader.getDestPort()+", SeqNum="+receivedMTPHeader.getSequenceNumber()
                                                +", AckNum="+receivedMTPHeader.getAckNumber()+", MSS="+receivedMTPHeader.getMSS()+", MWS="+receivedMTPHeader.getMWS()
                                                +", Len="+receivedMTPHeader.getPayloadByteLength());
                    this.receiverLogWriter.print("\nPAYLOAD: NO DATA");
                    this.receiverLogWriter.flush();
                    
                    // Increase receivers sequence number.
                    this.sequenceNumber = this.myMTPCalculation.getNextSeqNum(this.sequenceNumber, 1);
                    
                    // Output Host State.
                    System.out.println("State: RECEIVED_CONNECTION_GRANTED_SEGMENT");  
                    
                }
            }
                   
        }
        else
        {   
            // Keep looping for a SYN segment.
            this.handShakeReceive();
        }
        
    }
    
    /**
     * Used to enter into data receive and send mode. This is where the MTP receive protocol gets
     * activated. 
     */
    public void exchange()
    {   
        // Output sender status/event to text log file.
        this.receiverLogWriter.print("\n\nTIME: "+this.getCurrentSeconds()+"ms");
        this.receiverLogWriter.print("\nReady to Transfer Data!");
        this.receiverLogWriter.flush();
        
        // Set up the receivers multi-threading to send and receive at the same time.
        Runnable newReceiverReceive = new ReceiverReceive(this);
        Runnable newReceiverSend = new ReceiverSend(this);
        
        Thread thread1 = new Thread(newReceiverReceive);
        Thread thread2 = new Thread(newReceiverSend);
        
        // MTP receive protocol is started.
        thread1.start();
        thread2.start();
    }
    
    /**
     * Used as a global sending function to send any data the receiver or any dependent
     * classes might have.
     * @param aHeaderBitSet BitSet, The set of header bits to send.
     * @param aBytePayLoad byte[], The byte array of payload data to be sent. 
     */
    public void sendMTPSegment(BitSet aHeaderBitSet, byte[] aBytePayLoad)
    {
        // Convert BitSet header into a byte array;
        byte[] byteHeader = this.myMTPCalculation.toByteArray(aHeaderBitSet);
        
        // Prepare total byte msg.
        byte[] byteMsg = byteHeader;
        
        // Check if there is a payload.
        if(aBytePayLoad != null)
        {
            // Combine header and payload array into one byte array.
            byteMsg = this.myMTPCalculation.getFullSegment(byteHeader, aBytePayLoad);
        }
        
        // Attempting to send segment.
        try 
        {   
            // Create IP Address reference for segment preparation.
            InetAddress destIP = InetAddress.getByName(this.CLIENT_HOST_IP);
            
            // Make actual UDP segment.
            DatagramPacket newUDPSegment = new DatagramPacket(byteMsg, byteMsg.length, destIP, this.senderHostPort);
            
            // Send UDP segment.
            this.socketUDP.send(newUDPSegment);
        }
        catch (IOException e) 
        {
            // Get current line number.
            int lineNumber = new Exception().getStackTrace()[0].getLineNumber();
            
            // Handle exception.
            this.processException(e, lineNumber);
        }
        
    }
    
    /**
     * Used as a global receiving function to receive any data the receiver or any dependent
     * classes might have.
     * @return byte[], The segment byte array received. 
     */
    public byte[] receiveMTPSegment()
    {   
        // Attempt to receive.
        try
        {   
            // Create a datagram packet to hold incoming UDP packets.
            DatagramPacket requestStore = new DatagramPacket(new byte[this.HEADER_BYTE_SIZE+this.mss], this.HEADER_BYTE_SIZE+this.mss);
            
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
