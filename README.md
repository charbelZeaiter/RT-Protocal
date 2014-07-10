RT-Protocal
===========

My Reliable Transport Protocal

Using UDP as a base; I researched a small subset of TCP reliability features and built
my own primitive reliable transport protocol. Note: multi-threading has been used.  

=== Features ===  

- Initial Random segment sequence number.
- Initial three way handshake for connection establishment.
- Sender timeout.
- Sender “fast retransmit”.
- Receiver “cumulative acknowledgements”.
- Receiver “send buffer”.
- Sender & receiver log generation.
- Packet Loss Delay Module.


=== My Segment Structure ===

1. SYN Field:
- 1 bit in length.
- Used for setting up the connection in the initial client/server handshake.
- The first segment sent to the server will have the SYN field/bit set.
2. Ack Field:
- 1 bit in length.
- Set when the sender/receiver is replying/ack-ing data back to the original sender.
- Used in file transfer as well as the initial handshake.
3. DestPort Field:
- 16 bits in length.
- This is the destination port of the segment which is used to de-multiplex to the correct socket running the desired process on the receiver program.
4. SrcPort Field:
- 16 bits in length.
- Used to inform the receiver of the port that needs to be sent to when replying to the sender program.
5. Sequence Number Field:
- 32 bits in length.
- Used to identify the segment that has been transmitted.
- For example it can be used to compare against an expected sequence number in the case of packet loss.
6. Ack Number Field:
- 32 bits in length.
- Used to confirm the transmission of up to the sequence number that was successfully transmitted and to request the next expected sequence number.•
7. MSS Field:
- 16 bits in length.
- Used to relay the maximum segment size that’s being used to the receiver so it can perform some receiving calculations/adjustments.
8. MWS Field:
- 16 bits in length.
- Used to relay the maximum window size that’s being used to the receiver so it can perform some receiving calculations/adjustments in order to receive properly.
9. Payload Length Field:
- 32 bits in length.
- Used to relay the data payload size in the segment to that the receiver can perform the correct calculations and operations to receive. For example, the correct payload size must be known by the receiver to extract the exact portion of bytes form total received segment to not get any extra characters in file that the receiver is creating.
9. End Bit Field:
- 1 bit in length.
- Used as a marker to know when the header ends in case extra options in the future where to be used.
- Also used in bit traversal.
- I.e. this is just an extra field remaining in case the capabilities of the program where to be further expanded etc.


=== Creation ===  

By: Charbel Zeaiter  
Year: 2013  
