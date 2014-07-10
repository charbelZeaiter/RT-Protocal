import java.util.BitSet;

/**
 * This Class is used to get and set values of created bit headers used in MTP segments.
 * @author Charbel Zeaiter, z3419481.
 */
public class MTPHeader {
    
    // Class Fields /////////////////////////////////////////////////
    
    private BitSet newHeader;
    private boolean[] bitArray16Wide;
    private boolean[] bitArray32Wide;
    
    private static final int HEADER_BIT_SIZE = 163;
    private static final int BIT_WIDTH_16 = 16;
    private static final int BIT_WIDTH_32 = 32;
    private final int INDEX_OF_SYN;
    private final int INDEX_OF_ACK;
    private final int INDEX_OF_DEST_PORT;
    private final int INDEX_OF_SRC_PORT;
    private final int INDEX_OF_SEQUENCE_NUMBER;
    private final int INDEX_OF_ACK_NUMBER;
    private final int INDEX_OF_MSS;
    private final int INDEX_OF_MWS;
    private final int INDEX_OF_PAYLOAD_BYTE_LENGTH;
    
    private final int INDEX_OF_END_OF_HEADER_BIT;
    
    // Class Constructor ////////////////////////////////////////////
    
    /**
     * Class constructor.
     * @param aBitSet BitSet, The reference of the BitSet to manipulate.
     */
    public MTPHeader(BitSet aBitSet)
    {   
        // Set all class fields.
        this.newHeader = aBitSet;
        
        this.INDEX_OF_SYN = 0;
        this.INDEX_OF_ACK = 1;
        this.INDEX_OF_DEST_PORT = 2;
        this.INDEX_OF_SRC_PORT = 18;
        this.INDEX_OF_SEQUENCE_NUMBER = 34;
        this.INDEX_OF_ACK_NUMBER = 66;
        this.INDEX_OF_MSS = 98;
        this.INDEX_OF_MWS = 114;
        this.INDEX_OF_PAYLOAD_BYTE_LENGTH = 130;
        this.INDEX_OF_END_OF_HEADER_BIT = 162;
        
        this.bitArray16Wide = new boolean[16];
        this.bitArray32Wide = new boolean[32];
        
        this.newHeader.set(this.INDEX_OF_END_OF_HEADER_BIT);
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * Sets the SYN header flag.
     */
    public void setSYN()
    {
        this.newHeader.set(this.INDEX_OF_SYN);
    }
    
    /**
     * Clears the SYN header flag.
     */
    public void clearSYN()
    {
        this.newHeader.clear(this.INDEX_OF_SYN);
    }

    /**
     * Returns the value of the SYN flag bit.
     * @return Boolean, If flag is set or cleared.
     */
    public Boolean getSYN()
    {
        return this.newHeader.get(this.INDEX_OF_SYN);
    }
    
    /**
     * Sets the ACK flag in the header.
     */
    public void setACK()
    {
        this.newHeader.set(this.INDEX_OF_ACK);
    }
    
    /**
     * Clears the ACK flag in the header.
     */
    public void clearACK()
    {
        this.newHeader.clear(this.INDEX_OF_ACK);
    }
    
    /**
     * Returns the bit in the ACK flag.
     * @return Boolean
     */
    public boolean getACK()
    {
        return this.newHeader.get(this.INDEX_OF_ACK);
    }
    
    /**
     * Sets the destination port value in the header.
     * @param aDestPort Integer, The destination port.
     */
    public void setDestPort(int aDestPort)
    {
        // Convert port number to binary.
        convertIntToBinary(aDestPort, BIT_WIDTH_16);
        
        // Store binary string in header.
        int currentIndex = this.INDEX_OF_DEST_PORT;
        for(boolean element : this.bitArray16Wide)
        {
            if(element)
            {
                this.newHeader.set(currentIndex);
            }
            else
            {
                this.newHeader.clear(currentIndex);
            }
            
            currentIndex++;
        }

    }
    
    /**
     * Gets the destination port value from the header.
     * @return Integer, Destination port.
     */
    public int getDestPort()
    {
        // Extract binary data from header and store in class array.
        for(int i=0;i<BIT_WIDTH_16;i++)
        {   
            int currentBitIndex = this.INDEX_OF_DEST_PORT+i;
            
            if(this.newHeader.get(currentBitIndex))
            {
                this.bitArray16Wide[i] = true;
            }
            else
            {
                this.bitArray16Wide[i] = false;
            }
            
            currentBitIndex++;
        }
        
        // Convert binary data in class array into integer.
        int destPort = convertBinaryToInt(BIT_WIDTH_16);
        
        return destPort;
    }
    
    /**
     * Sets the source port number value in the header.
     * @param aSRCPort Integer, A source port number.
     */
    public void setSRCPort(int aSRCPort)
    {
        // Convert port number to binary and store in class array.
        convertIntToBinary(aSRCPort, BIT_WIDTH_16);
        
        // Store binary string in header.
        int currentIndex = this.INDEX_OF_SRC_PORT;
        for(boolean element : this.bitArray16Wide)
        {
            if(element)
            {
                this.newHeader.set(currentIndex);
            }
            else
            {
                this.newHeader.clear(currentIndex);
            }
            
            currentIndex++;
        }

    }
    
    /**
     * Gets the source port value from the header.
     * @return Integer, Source port.
     */
    public int getSRCPort()
    {
        // Extract binary data from header and store in class array.
        for(int i=0;i<BIT_WIDTH_16;i++)
        {   
            int currentBitIndex = this.INDEX_OF_SRC_PORT+i;
            
            if(this.newHeader.get(currentBitIndex))
            {
                this.bitArray16Wide[i] = true;
            }
            else
            {
                this.bitArray16Wide[i] = false;
            }
            
            currentBitIndex++;
        }
        
        // Convert binary data in class array into integer.
        int srcPort = convertBinaryToInt(BIT_WIDTH_16);
        
        return srcPort;
    }    
    
    /**
     * Sets the sequence number value in the header.
     * aSequenceNumber Integer, A sequence number.
     */
    public void setSequenceNumber(int aSequenceNumber)
    {
        // Convert port number to binary and store in class array.
        convertIntToBinary(aSequenceNumber, BIT_WIDTH_32);
        
        // Store binary string in header.
        int currentIndex = this.INDEX_OF_SEQUENCE_NUMBER;
        for(boolean element : this.bitArray32Wide)
        {
            if(element)
            {
                this.newHeader.set(currentIndex);
            }
            else
            {
                this.newHeader.clear(currentIndex);
            }
            
            currentIndex++;
        }

    }
    
    /**
     * Gets the sequence number value from MTP header.
     * @return Integer, A sequence number value.
     */
    public int getSequenceNumber()
    {
        // Extract binary data from header and store in class array.
        for(int i=0;i<BIT_WIDTH_32;i++)
        {   
            int currentBitIndex = this.INDEX_OF_SEQUENCE_NUMBER+i;
            
            if(this.newHeader.get(currentBitIndex))
            {
                this.bitArray32Wide[i] = true;
            }
            else
            {
                this.bitArray32Wide[i] = false;
            }
            
            currentBitIndex++;
        }
        
        // Convert binary data in class array into integer.
        int sequenceNumber = convertBinaryToInt(BIT_WIDTH_32);
        
        return sequenceNumber;
    }

    /**
     * Sets the ACK Number value in the MTP header.
     * @param aAckNumber Integer, The ACK Number value to put in the header.
     */
    public void setAckNumber(int aAckNumber)
    {
        // Convert port number to binary.
        convertIntToBinary(aAckNumber, BIT_WIDTH_32);
        
        // Store binary string in header.
        int currentIndex = this.INDEX_OF_ACK_NUMBER;
        for(boolean element : this.bitArray32Wide)
        {
            if(element)
            {
                this.newHeader.set(currentIndex);
            }
            else
            {
                this.newHeader.clear(currentIndex);
            }
            
            currentIndex++;
        }

    }
    
    /**
     * Gets the ACK Number value from the MTP header.
     * @return Integer, ACK Number value.
     */
    public int getAckNumber()
    {
        // Extract binary data from header and store in class array.
        for(int i=0;i<BIT_WIDTH_32;i++)
        {   
            int currentBitIndex = this.INDEX_OF_ACK_NUMBER+i;
            
            if(this.newHeader.get(currentBitIndex))
            {
                this.bitArray32Wide[i] = true;
            }
            else
            {
                this.bitArray32Wide[i] = false;
            }
            
            currentBitIndex++;
        }
        
        // Convert binary data in class array into integer.
        int ackNumber = convertBinaryToInt(BIT_WIDTH_32);
        
        return ackNumber;
    }
    
    /**
     * Sets the MSS value in the MTP header.
     * @param aMSS Integer, A MSS value.
     */
    public void setMSS(int aMSS)
    {
        // Convert port number to binary and store in class array.
        convertIntToBinary(aMSS, BIT_WIDTH_16);
        
        // Store binary string in header.
        int currentIndex = this.INDEX_OF_MSS;
        for(boolean element : this.bitArray16Wide)
        {
            if(element)
            {
                this.newHeader.set(currentIndex);
            }
            else
            {
                this.newHeader.clear(currentIndex);
            }
            
            currentIndex++;
        }

    }
    
    /**
     * Gets the MSS value from the MTP header.
     * @return Integer, MSS value.
     */
    public int getMSS()
    {
        // Extract binary data from header and store in class array.
        for(int i=0;i<BIT_WIDTH_16;i++)
        {   
            int currentBitIndex = this.INDEX_OF_MSS+i;
            
            if(this.newHeader.get(currentBitIndex))
            {
                this.bitArray16Wide[i] = true;
            }
            else
            {
                this.bitArray16Wide[i] = false;
            }
            
            currentBitIndex++;
        }
        
        // Convert binary data in class array into integer.
        int mss = convertBinaryToInt(BIT_WIDTH_16);
        
        return mss;
    }
    
    /**
     * Sets the MWS value in the MTP header.
     * @param aMWS Integer, A MWS value.
     */
    public void setMWS(int aMWS)
    {
        // Convert port number to binary and store in class array.
        convertIntToBinary(aMWS, BIT_WIDTH_16);
        
        // Store binary string in header.
        int currentIndex = this.INDEX_OF_MWS;
        for(boolean element : this.bitArray16Wide)
        {
            if(element)
            {
                this.newHeader.set(currentIndex);
            }
            else
            {
                this.newHeader.clear(currentIndex);
            }
            
            currentIndex++;
        }

    }
    
    /**
     * Gets the MWS value from the MTP header.
     * @return Integer, header MWS value.
     */
    public int getMWS()
    {
        // Extract binary data from header and store in class array.
        for(int i=0;i<BIT_WIDTH_16;i++)
        {   
            int currentBitIndex = this.INDEX_OF_MWS+i;
            
            if(this.newHeader.get(currentBitIndex))
            {
                this.bitArray16Wide[i] = true;
            }
            else
            {
                this.bitArray16Wide[i] = false;
            }
            
            currentBitIndex++;
        }
        
        // Convert binary data in class array into integer.
        int mws = convertBinaryToInt(BIT_WIDTH_16);
        
        return mws;
    }
    
    /**
     * Sets the payload value in the MTP header.
     * @param aPayloadByteLength Integer, A payload value.
     */
    public void setPayloadByteLength(int aPayloadByteLength)
    {
        // Convert port number to binary and store in class array.
        convertIntToBinary(aPayloadByteLength, BIT_WIDTH_32);
        
        // Store binary string in header.
        int currentIndex = this.INDEX_OF_PAYLOAD_BYTE_LENGTH;
        for(boolean element : this.bitArray32Wide)
        {
            if(element)
            {
                this.newHeader.set(currentIndex);
            }
            else
            {
                this.newHeader.clear(currentIndex);
            }
            
            currentIndex++;
        }

    }
    
    /**
     * Gets the payload value from the MTP header.
     * @return Integer, The payload value 
     */
    public int getPayloadByteLength()
    {
        // Extract binary data from header and store in class array.
        for(int i=0;i<BIT_WIDTH_32;i++)
        {   
            int currentBitIndex = this.INDEX_OF_PAYLOAD_BYTE_LENGTH+i;
            
            if(this.newHeader.get(currentBitIndex))
            {
                this.bitArray32Wide[i] = true;
            }
            else
            {
                this.bitArray32Wide[i] = false;
            }
            
            currentBitIndex++;
        }
        
        // Convert binary data in class array into integer.
        int payloadByteLength = convertBinaryToInt(BIT_WIDTH_32);
        
        return payloadByteLength;
    }
    
    // Helper Subroutines ///////////////////////////////////////////
     
    /**
     * Converts an integer to binary and stores this result in the specified class array field
     * @param aInt Integer, A number to convert.
     * @param aBitSize Integer, The length of the array to use which specifies a class array field.
     */
    private void convertIntToBinary(int aInt, int aBitSize)
    {
        String binaryString = Integer.toBinaryString(aInt);
        int binaryLength = binaryString.length();
        
        boolean[] binaryExpression;
        
        if(aBitSize == 16)
        {
            binaryExpression = this.bitArray16Wide;
        }
        else
        {
            binaryExpression = this.bitArray32Wide;
        }
        
        // First set all bits to zero.
        for(int i=0;i<aBitSize;i++)
        {
            binaryExpression[i] = false;                
        }
        
        // Add to bit array the non padded binary figure.
        for(int i=0;i<binaryLength;i++)
        {
            if(binaryString.charAt(i) == '1')
            {
                binaryExpression[aBitSize-binaryLength+i] = true;
            }
        }
        
    }
    
    /**
     * Converts the bit data in one of the class bit array fields to an integer. 
     * @param aBitSize Integer, The length of the bits to convert which also chooses which bit array is used.
     * @return Integer
     */
    private int convertBinaryToInt(int aBitSize)
    {
        boolean[] binaryExpression;
        
        if(aBitSize == 16)
        {
            binaryExpression = this.bitArray16Wide;
        }
        else
        {
            binaryExpression = this.bitArray32Wide;
        }
        
        // Build a binary string to easily convert binary to integer.
        StringBuilder builder = new StringBuilder();
        for (boolean element : binaryExpression) 
        {
            if(element)
            {
                builder.append("1");
            }
            else
            {
                builder.append("0");
            }
        }
        
        String binaryString = builder.toString();
        int number = Integer.parseInt(binaryString, 2);
        
        return number;
    }
    
    /**
     * Prints out the binary associated with a given Class bit array field.
     * @param aBitSize Integer, The length of the binary string to print which also chooses which bit array is used.
     */
    private void printBinary(int aBitSize)
    {
        boolean[] binaryExpression;
        
        if(aBitSize == 16)
        {
            binaryExpression = this.bitArray16Wide;
        }
        else
        {
            binaryExpression = this.bitArray32Wide;
        }
        
        // Printing to terminal.
        System.out.print("Binary result : ");
        
        for(boolean element : binaryExpression)
        {
            if(element == true)
            {
                System.out.print('1');
            }
            else
            {
                System.out.print('0');
            }
        }
        System.out.print("\n");
    }
    
    
    
    
    
    
    
}
