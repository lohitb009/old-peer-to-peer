package bitTorrents.peer_1002;

import bitTorrents.*;
import java.util.*;
import java.io.*;

public class PeerProcess1002 {
    public static void main(String[] args) throws Exception{
        /*
         * 1) Get the port # from the cmd line arguments
         * 2) Connect to all the peers the came before this peer(Get info from PeerInfo.cfg(Main Thread)
         * 3) Start the server(Thread t1) and then continously read if there are any messages received(Thread t2)
         *    Send the messages accordingly after receiving
         * 4) If the user has file, divide the file into chunks(Main Thread)
         * 5) Start the timer for neighbour(Thread t3) and optimistically unchoked neighbour(Thread t4)
         */

        /* Get data from common.cfg */
        int id = Integer.parseInt(args[0]);
        FileInputStream common = new FileInputStream("bitTorrents/Common.cfg");
        Properties properties = new Properties();
        properties.load(common);int port = 0;
        int numberOfPrefferedNeighbours = Integer.parseInt(properties.getProperty("NumberOfPreferredNeighbors"));
        int n = Integer.parseInt(properties.getProperty("UnchokingInterval"));
        int m = Integer.parseInt(properties.getProperty("OptimisticUnchokingInterval"));
        int pieceSize = Integer.parseInt(properties.getProperty("PieceSize"));
        int fileSize = Integer.parseInt(properties.getProperty("FileSize"));
        String fileName = properties.getProperty("FileName",null);

        PeerProcess peerProcess = new PeerProcess(6008,id,numberOfPrefferedNeighbours,n,m,pieceSize);

        /* ****** */

        /* Get data from PeerInfo.cfg and connect */
        FileInputStream inputStream = new FileInputStream("bitTorrents/PeerInfo.cfg");
        String data = "";int i = 0;
        while((i=inputStream.read()) != -1){data += (char)i;}
        String[] peers = data.split("\n");
        for(String peer:peers){
            String[] content = peer.split(" ");
            if(Integer.parseInt(content[0]) == id){
                port = Integer.parseInt(content[2]);
                peerProcess.setPORT(port);
                if(content[content.length-1].equals("1")) {
                    int size = peerProcess.processFile("bitTorrents/peer_1002/"+fileName);
                    peerProcess.setBitfield((int) (Math.ceil((double) size / (double) pieceSize)), content[content.length - 1]);
                }
                else{
                    peerProcess.setBitfield((int) (Math.ceil((double) fileSize / (double) pieceSize)), content[content.length - 1]);
                }
                break;
            }
            String peerHost = content[1];
            int peerPort = Integer.parseInt(content[2]);

            peerProcess.connect(peerHost,peerPort);
        }
        peerProcess.changeNeighbours();
        peerProcess.changeOptimisticallyNeighbours();
        /* ****** */

        /* Define all Threads */
        peerProcess.t1 = new Thread(()->{
            peerProcess.startServer();
        });
        peerProcess.t2 = new Thread(()->{
            peerProcess.read();
        });
        peerProcess.t3 = new Thread(()->{
            peerProcess.unchokingInterval();
        });
        peerProcess.t4 = new Thread(()->{
            peerProcess.optimisticUnchokedInterval();
        });
        /* ****** */

        /* Start all Threads */
        peerProcess.t1.start();peerProcess.t2.start();
        peerProcess.t3.start();
        peerProcess.t4.start();
        /* ****** */
    }
}



