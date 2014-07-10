
/**
 * Used as the program launch class (with main).
 * @author Charbel Zeaiter.
 */
public class mtp_sender {
    
    public static void main(String[] args)
    {   
        if(args.length == 8)
        {
            // Extract parameters from terminal arguments.
            String receiverHostIP = args[0];
            int receiverPort = Integer.parseInt(args[1]);
            String filename = args[2];
            int maxWindowSize = Integer.parseInt(args[3]);
            int maxSegmentSize = Integer.parseInt(args[4]);
            int timeoutValue = Integer.parseInt(args[5]);
            float probOfDrop = Float.parseFloat(args[6]);
            int seedValue = Integer.parseInt(args[7]);
            
            // Launch sender. 
            Sender newSender = new Sender(receiverHostIP, receiverPort, filename, 
                                          maxWindowSize, maxSegmentSize, timeoutValue,
                                          probOfDrop, seedValue);
            // Execute sender modes.
            newSender.handShake();
            newSender.exchange();
            
        }
        else
        {   
            // Exit program. 
            System.out.println("Need correct parameters");
            System.exit(0);
        }
    }
  
}
