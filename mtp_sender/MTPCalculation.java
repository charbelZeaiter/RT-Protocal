import java.util.BitSet;
import java.util.Random;

/**
 * This class is used by the global sender and associated classes to perform general 
 * transmission calculations and conversions. 
 * @author Charbel Zeaiter.
 *
 */
public class MTPCalculation {
    
    // Class Fields /////////////////////////////////////////////////
    private int seedValue;
    private int maxIntNumber;
    
    // Class Constructor ////////////////////////////////////////////
    
    /**
     * Class constructor.
     * @param aSeedValue Integer, Random number generator seed value.
     * @param aMaxIntNumber Integer, Maximum integer number before overflow.
     */
    public MTPCalculation(int aSeedValue, int aMaxIntNumber)
    {
        this.seedValue = aSeedValue;
        this.maxIntNumber = aMaxIntNumber;
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * Used to convert from an array of bytes to a bitset.
     * Taken from the Internet at "http://www.java2s.com/Code/Java/Language-Basics/ConvertingBetweenaBitSetandaByteArray.htm".
     * @param bytes Byte[], An array of bytes.
     * @return BitSet, The converted BitSet.
     */
    public BitSet fromByteArray(byte[] bytes) 
    {
          BitSet bits = new BitSet();
          for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
              bits.set(i);
            }
          }
          return bits;
     }

    /**
     * Used to convert from a BitSet to a Byte array.
     * Taken from the Internet at "http://stackoverflow.com/questions/14032437/java-bitset-and-byte-usage".
     * @param bits BitSet, The BitSet to be converted.
     * @return Byte[], The converted byte array.
     */
    public byte[] toByteArray(BitSet bits) {
        byte[] bytes = new byte[(bits.length() + 7) / 8];
        for (int i=0; i<bits.length(); i++) {
            if (bits.get(i)) {
                bytes[bytes.length-i/8-1] |= 1<<(i%8);
            }
        }
        return bytes;
    }
    
    /**
     * Generates a random starting sequence number for the receiver.
     * @return Integer, A sequence number.
     */
    public int getStartingSeqNum()
    {
        Random newRGen = new Random(this.seedValue);
        
        return newRGen.nextInt(this.maxIntNumber);
    }

    /**
     * Gets the header and payload data byte arrays and combines them into one byte array. 
     * @param aHeader byte[], The header byte array.
     * @param aPayLoad byte[], The payload byte array.
     * @return byte[], The Full segment byte array.
     */
    public byte[] getFullSegment(byte[] aHeader, byte[] aPayLoad)
    {
        int headerLength = aHeader.length;
        int payLoadLength = aPayLoad.length;
        
        // Create new array.
        byte[] fullSegment = new byte[headerLength+payLoadLength];
        
        // Copy over parts of arrays.
        System.arraycopy(aHeader, 0, fullSegment, 0, headerLength);
        System.arraycopy(aPayLoad, 0, fullSegment, headerLength, payLoadLength);
        
        return fullSegment;
    }
    
    /**
     * Gets a full segment byte array and extracts the header byte array from it.
     * @param aFullByteSegment byte[], The full segment byte array.
     * @param aHeaderSize Integer, The byte size of the header.
     * @return byte[], The header byte array.
     */
    public byte[] extractHeader(byte[] aFullByteSegment, int aHeaderSize)
    {
        byte[] header = new byte[aHeaderSize];
        
        // Copy the header part of the full array into the created array.
        System.arraycopy(aFullByteSegment, 0, header, 0, aHeaderSize);
        
        return header;
    }

    /**
     * Gets a full segment byte array and extracts the payload byte array from it.
     * @param aFullByteSegment byte[], The full segment byte array.
     * @param aHeaderSize Integer, The byte size of the header.
     * @param aPayloadSize Intger, The byte size of the payload.
     * @return byte[], The payload byte array.
     */
    public byte[] extractPayLoad(byte[] aFullByteSegment, int aHeaderSize, int aPayloadSize)
    {   
        
        byte[] payLoad = new byte[aPayloadSize];
        
        // Copy the payload part of the full array into the created array.
        System.arraycopy(aFullByteSegment, aHeaderSize, payLoad, 0, aPayloadSize);
        
        return payLoad;
    }
    
    /**
     * Used to get the next segment sequence number without overflowing the Integer used.  
     * @param aCurrentSeqNum Integer, The current sequence number.
     * @param aIncrement Integer, The number to increment by.
     * @return Integer, Next sequence number.
     */
    public int getNextSeqNum(int aCurrentSeqNum, int aIncrement)
    {
        int newSeqNum = aCurrentSeqNum + aIncrement;
        
        if(newSeqNum >= 0)
        {
            return newSeqNum;
        }
        else
        {   
            // The integer has overflowed, adding one by one now
            // till it overflows and adding the rest of the increments.
            for(int i=0;i<aIncrement;i++)
            {
                aCurrentSeqNum++;
                
                if(aCurrentSeqNum < 0)
                {
                    aCurrentSeqNum = 0;
                }
            }
            
            return aCurrentSeqNum;
        }
    }
        
}
