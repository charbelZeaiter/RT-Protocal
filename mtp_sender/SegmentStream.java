import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is used prepare the data to be sent (from the file that the sender is pointed to) 
 * and segment it into an array of segments to be later used in the senders send algorithm.
 * @author Charbel Zeaiter
 */
public class SegmentStream {
    
    // Class Fields /////////////////////////////////////////////////
    private ArrayList<Triple> mySegmentStream;
    private Sender mySender;
    
    // Class Constuctor /////////////////////////////////////////////
    
    /**
     * Class Constructor.
     * @param aSender Sender, A reference to the 'global' sender object where data will be updated and used.
     */
    public SegmentStream(Sender aSender)
    {   
        this.mySender = aSender;
        
        // Create empty segment stream.
        this.mySegmentStream = new ArrayList<Triple>();
        
        // Read in Bytes from text file.
        try 
        {
            FileInputStream newInputStream = new FileInputStream(this.mySender.FILENAME);
            
            // Setting variables and calculations.
            int totalFileBytes = newInputStream.available();
            int numFullSeg = totalFileBytes/this.mySender.MAX_SEGMENT_SIZE;
            int lastByteSegLength = totalFileBytes - (this.mySender.MAX_SEGMENT_SIZE*numFullSeg);
            int counter = 1;
            
            // Segmentation algorithm.
            
            byte[] aDataSegment;
            
            // Make an ajustment for first segment.
            if(numFullSeg == 0)
            {
                aDataSegment = new byte[lastByteSegLength];
            }
            else
            {
                aDataSegment = new byte[this.mySender.MAX_SEGMENT_SIZE];
            }
            
            // Go through data in file and assign segments.
            while(newInputStream.read(aDataSegment) != -1)
            {
                Triple newPair = new Triple(0, aDataSegment);
                this.mySegmentStream.add(newPair);
                
                counter++;
                
                if(counter == numFullSeg+1)
                {
                    aDataSegment = new byte[lastByteSegLength];
                }
                else
                {
                    aDataSegment = new byte[this.mySender.MAX_SEGMENT_SIZE];
                }
                
            }
            
            // Assign sequence number to each data segment.
            this.giveSeqToSeg();
            
        }
        catch (FileNotFoundException e) 
        {
            // Get current line number.
            int lineNumber = new Exception().getStackTrace()[0].getLineNumber();
            
            // Handle exception.
            this.processException(e, lineNumber);
        }
        catch(IOException e)
        {
            // Get current line number.
            int lineNumber = new Exception().getStackTrace()[0].getLineNumber();
            
            // Handle exception.
            this.processException(e, lineNumber);
        }
        
        
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * Gets the reference of the data segment array.
     * @return ArrayList<Triple>
     */
    public ArrayList<Triple> getDataSegmentArray()
    {
        return this.mySegmentStream;
    }
    
    /**
     * Gets the last segment sequence number.
     * @return Integer
     */
    public int getLastSegSeqNum()
    {
        int segmentArrayLength = this.mySegmentStream.size();
        Triple lastSeg = this.mySegmentStream.get(segmentArrayLength-1);
        return lastSeg.getSequenceNumber();
    }
    
    /**
     * Assigns sequence numbers to all segments in the segment stream.
     */
    private void giveSeqToSeg()
    {   
        int currentSeqNum = this.mySender.sequenceNumber;
        
        for(Triple element : mySegmentStream)
        {
            element.setSequenceNumber(currentSeqNum);
            
            currentSeqNum = this.mySender.myMTPCalculation.getNextSeqNum(currentSeqNum, element.getDataSegment().length);
        }
        
    }
    
    /**
     * Used to process exceptions.
     * @param e IOException
     * @param aLineNumber Integer
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
