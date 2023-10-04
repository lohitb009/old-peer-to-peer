# BitTorrent
## Instructions to run:
After logging into a CISE Linux server, we use the below command to go to the desired path:
cd BitTorrent/BitTorrents/src

To run the program for all 5 peers in 5 different Linux servers:
```
java bitTorrents/peer_1001/PeerProcess1001 1001
java bitTorrents/peer_1002/PeerProcess1002 1002
java bitTorrents/peer_1003/PeerProcess1003 1003
java bitTorrents/peer_1004/PeerProcess1004 1004
java bitTorrents/peer_1005/PeerProcess1005 1005
```
## Brief Description:

Every peer follows the below steps-
1. Get id from command line argument
2. Set configuration from Common.cfg file
3. Check which peers are in network from PeerInfo.cfg, start server and accept other peer connections, and connect with them by sending handshake messages
4. Process the file into an optimal data structure if peer has the file
5. Read the messages received and send the appropriate responses based on protocol
6. Change neighbors after every unchoking and optimistically unchoking interval.

## Protocol Description:

1. After Handshake, the peers will send BITFIELD message to each other
2. If there are pieces with one peer other peer doesnt have, that peer will send INTRESTED message else, the peer will send NOT_INTRESTED message. 
3. Once HAVE/BITFIELD is received, the peer will send REQUEST message with piece number peer requesting. 
4. Once REQUEST message is received, the peer will send PIECE message with the requested piece as payload.
5. Once PIECE message is reveived, the peer will update its bitfield and store that piece.
6. After every unchoking/optimistically unchoking intervals, the current neighbours are choked and will receive CHOKE message and the new neighbours will be unchoked and will receive UNCHOKE message.
7. Whenever a peer receive HAVE message from other peer, the peer will check if it has that piece or not and will send INTERESTED/NOT_INTRESTED message accordingly. 

## Termination:
When all the peers have received the file, the connections are terminated.
