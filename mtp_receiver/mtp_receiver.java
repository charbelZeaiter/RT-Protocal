
/**
 * Used as the program launch class (with main).
 * @author Charbel Zeaiter.
 */
public class mtp_receiver {
    
    public static void main(String[] args)
    {    
        if(args.length == 2)
        {
            // Extract parameters from terminal arguments.
            int receiverPort = Integer.parseInt(args[0]);
            String filename = args[1];
            
            // Launch receiver.
            Receiver newReceiver = new Receiver(receiverPort, filename);
            
            // Perform receiver actions.
            newReceiver.handShakeReceive();
            newReceiver.exchange();
            
            
        }
        else
        {   
            // Exit program.
            System.out.println("Need correct parameters");
            System.exit(0);
        }
    }
    
}
