import java.util.Random;

/**
 * This Class is used as the Packet Loss Delay module.
 * @author Charbel Zeaiter
 *
 */
public class PLD {
    
    // Class Fields /////////////////////////////////////////////////
    float probOfDrop;
    Random newRGen;
    
    // Class Contructor /////////////////////////////////////////////
    
    /**
     * Class Constructor.
     * @param aProbOfDrop Float, The decimal probability of packet dropping. 
     * @param aSeedValue Integer, The seed value to be used for the random number generator.
     */
    public PLD(float aProbOfDrop, int aSeedValue)
    {
        this.probOfDrop = aProbOfDrop;
        this.newRGen = new Random(aSeedValue);
    }
    
    // Class Methods ////////////////////////////////////////////////
    
    /**
     * Generated on the fly this function returns whether the packet is dropped or not. 
     * @return Boolean, If the packet is dropped.
     */
    public boolean getToDrop()
    {
        float newProb = this.newRGen.nextFloat();
        
        if(newProb > this.probOfDrop)
        {
            return false;
        }
        else
        {
            return true;
        }
        
    }
    
}
