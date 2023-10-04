package bitTorrents;


import bitTorrents.Constants;
import bitTorrents.HandshakeMessage;
import bitTorrents.Message;
import bitTorrents.MyObjectOutputStream;
import bitTorrents.peer_1001.PeerProcess1001;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class PeerProcess {
    public Thread t1, t2, t3, t4;
    private Constants constants = new Constants();
    private ObjectInputStream objectInputStream = null;
    private ObjectOutputStream objectOutputStream = null;
    private ServerSocket serverSocket = null; //Socket to start the server
    private List<Socket> connectionsFrom = null; //Peers which are connecting to this server
    private List<Socket> connectedTo = null;//Peers which this peers is connecting to
    private int PORT;
    private int ID;
    private Set<Socket> neighbours = null;// All neighbours of this peer
    private Socket optimisticUnchokedNeighbour = null; //One Optimisically Unckhoked Neighbouiur
    int numberOfPrefferedNeighbours; // # of Prefereed Neighbor (neighbour.size() <= numberOfPrefferedNeighbours)
    int n, m; //n - UnchokingInterval, m - OptimisticUnchokingInterval
    int pieceSize;
    private HashMap<Integer, List<Integer>> fileContents;//Contents of file
    private HashMap<Socket, long[]> downloadRate = null;//Download rate of all peers
    private String bitfield = null;//if contains contains, all 1s 1
    private HashMap<Socket, String> bitfields = null;
    private List<Integer> requestedIndices = new ArrayList<>();
    private HashMap<Socket, ObjectOutputStream> objectOutputStreams = new HashMap<>();
    private HashMap<Socket, ObjectInputStream> objectInputStreams = new HashMap<>();
    private HashMap<Socket, Integer> ids = new HashMap<>();
    boolean flag;
    boolean optUc,Uc;
    Logger logger;

    //Empty Constructor
    public PeerProcess() {
    }

    public PeerProcess(int PORT, int ID, int numberOfPrefferedNeighbours, int n, int m, int pieceSize) {
        try {
            String loggerName = "bitTorrents/peer_"+ID+"/log_peer_"+ID+".log";
            new FileOutputStream(loggerName).write("".getBytes());
            this.logger = Logger.getLogger(PeerProcess.class.getName());
            FileHandler fileHandler = new FileHandler(loggerName,false);
            fileHandler.setFormatter(new MyCustomFormater());
            logger.addHandler(fileHandler);
            this.optUc = false;
            this.Uc = false;
            this.flag = false;
            this.PORT = PORT;
            this.ids = new HashMap<>();
            this.ID = ID;
            this.connectionsFrom = new ArrayList<>();
            this.connectedTo = new ArrayList<>();
            this.serverSocket = new ServerSocket(this.PORT);
            this.neighbours = new HashSet<>();
            this.optimisticUnchokedNeighbour = null;
            this.numberOfPrefferedNeighbours = numberOfPrefferedNeighbours;
            this.n = n;
            this.m = m;
            this.pieceSize = pieceSize;
            this.fileContents = new HashMap<>();
            this.bitfields = new HashMap<>();
            this.downloadRate = new HashMap<>();
            this.flag = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Set the bitfield
    public void setBitfield(int size, String file) {
        this.bitfield = new String();
        for (int i = 0; i < size; i++)
            this.bitfield += file;
//        logger.info("Bitfield:"+this.bitfield);
    }

    public List<Integer> processBitField(String bitfield) {
        List<Integer> pieces = new ArrayList<>();
        if (bitfield == null) return pieces;
        for (int i = 0; i <= bitfield.length() - 1; i++) {
            if (this.bitfield == null || (bitfield.charAt(i) == '1' && this.bitfield.charAt(i) == '0'))
                pieces.add(i);
        }
        return pieces;
    }

    //Process the file and add into list
    public int processFile(String filename) {
        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            int i, count = 0;
            while ((i = fileInputStream.read()) != -1) {
                if (!fileContents.containsKey(count / pieceSize))
                    fileContents.put(count / pieceSize, new ArrayList<>());
                this.fileContents.get(count / pieceSize).add(i);
                count++;
            }
            logger.info(count + " " + fileContents.get(fileContents.size() - 1).size());
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    //Start a thread and invoke this function
    public void startServer() {
        try {
            while (true) {
                logger.info("Listening to.." + this.PORT);
                Socket socket = serverSocket.accept();
                objectOutputStreams.put(socket, new ObjectOutputStream(socket.getOutputStream()));
                objectInputStreams.put(socket, new ObjectInputStream(socket.getInputStream()));
                connectionsFrom.add(socket);
                /*
                 * Send the handshake signal by invoking sendHandshake
                 * function
                 */
                sendHandshake(socket);
//                logger.info("Handshake Sent");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //read the data coming to the socket/peer continuously
    public void read() {
        while (true) {
            if (this.t2.isInterrupted()) {
                for (Socket socket : neighbours) {
                    Message message = new Message();
                    message.setMessageType(constants.getCHOKE());
                    send(socket, message);
                }
                if(Uc)this.changeNeighbours();
                if(optUc)this.changeOptimisticallyNeighbours();
                Thread.interrupted();
                this.Uc = false;this.optUc = false;
//                logger.info(this.t2.isInterrupted() + "hi");
                continue;
            }
            List<Socket> cT = new ArrayList<>(this.connectedTo);
            cT.addAll(this.connectionsFrom);
            for (Socket socket : cT) {
                try {
                    socket.setSoTimeout(1000);
//                    objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    objectInputStream = new ObjectInputStream(socket.getInputStream());
//                    logger.info(objectInputStream);
                    Object object = objectInputStream.readObject();
//                    logger.info(object.getClass().getName());
                    if (object instanceof bitTorrents.HandshakeMessage) {
                        HandshakeMessage handshakeMessage = (HandshakeMessage) object;
                        ids.put(socket, handshakeMessage.getId());
                        logger.info(System.currentTimeMillis() + ": Peer " + this.ID + " makes connection to Peer " + handshakeMessage.getId());
                        Message message = new Message();
                        message.setMessageType(constants.getBITFIELD());
                        message.setBitfield(this.bitfield);
                        send(socket, message);
                        /*
                         * Either send the handshake signal by invoking sendHandshake(socket) function
                         * or Process the message and send the appropriate message
                         * by creating a Message object and invoke send(socket,message) function
                         */

                    } else {

                        /*
                         * Process the message and send the appropriate message
                         * by creating a Message object and invoke send function
                         */
                        if (object instanceof Message) {
                            Message message = (Message) object;
                            if (message.getMessageType() == constants.getBITFIELD()) {
                                this.bitfields.put(socket, message.getBitfield());
                                List<Integer> pieces = processBitField(message.getBitfield());
                                if (pieces.size() == 0) {
                                    /* Send Not Interested Message */
                                    Message message1 = new Message();
                                    message1.setMessageType(constants.getNOT_INTERESTED());
                                    send(socket, message1);
                                } else {
                                    Message message1 = new Message();
                                    message1.setMessageType(constants.getINTERESTED());
                                    send(socket, message1);
                                    Random random = new Random();
                                    while (pieces.size() > 0) {
                                        int remove = random.nextInt(pieces.size());
                                        Message message2 = new Message();
                                        message2.setMessageType(constants.getREQUEST());
                                        message2.setIndexField(pieces.get(remove));
                                        send(socket, message2);
                                        downloadRate.put(socket, new long[]{System.nanoTime(), 0});
                                        pieces.remove(remove);
                                    }
                                }
                            } else if (message.getMessageType() == constants.getREQUEST()) {
                                int index = message.getIndexField();
                                List<Integer> piece = this.neighbours.contains(socket) ? fileContents.get(index) : new ArrayList<>();
                                Message message1 = new Message();
                                message1.setMessageType(constants.getPIECE());
                                message1.setIndexField(index);
                                message1.setPayload(piece);
                                send(socket, message1);
//                                logger.info(socket+" fhsghsodf "+downloadRate);
//                                logger.info("Piece Sent");
                            } else if (message.getMessageType() == constants.getPIECE()) {
//                                logger.info("Piece Received "+message.getIndexField());
//                                logger.info(socket);
                                int index = message.getIndexField();
                                char[] c = this.bitfield.toCharArray();
                                if (message.getPayload().size() != 0) {
                                    logger.info(System.currentTimeMillis() + ": Peer " + this.ID + " has downloaded the piece " + index + " from " + ids.get(socket));
                                    c[index] = '1';
                                    this.bitfield = new String(c);
                                    this.fileContents.put(index, message.getPayload());
                                }
                                boolean flag = true;
                                for (char x : c) {
                                    if (x == '0') {
                                        flag = false;
                                        break;
                                    }
                                }
                                if (flag && !this.flag) {
                                    logger.info("Processing file...");
                                    FileOutputStream fileOutputStream = new FileOutputStream("bitTorrents/peer_1001/tree.jpg");
                                    int size = fileContents.size();
                                    for (int i = 0; i < size; i++) {
                                        for (int j : fileContents.getOrDefault(i, new ArrayList<>()))
                                            fileOutputStream.write(j);
                                    }
                                    logger.info("File transfer complete");
                                }
                                this.flag = flag;
                                this.bitfield = new String(c);
                                if (requestedIndices.size() > 0) {
                                    Random random = new Random();
                                    int remove = random.nextInt(requestedIndices.size());
                                    Message message2 = new Message();
                                    message2.setMessageType(constants.getREQUEST());
                                    message2.setIndexField(requestedIndices.get(remove));
                                    send(socket, message2);
                                    downloadRate.put(socket, new long[]{System.nanoTime(), 0});
                                    requestedIndices.remove(remove);
//                                    logger.info("Request Sent "+remove);
                                }
                            } else if (message.getMessageType() == constants.getUNCHOKE()) {
                                /* bit is the bitfield what was stored  initially */
                                Message message1 = new Message();
                                message1.setMessageType((byte) 4);
                                message1.setBitfield(this.bitfield);
                                send(socket, message1);
                                logger.info(System.currentTimeMillis() + ": Peer " + this.ID + " is unchoked by " + ids.get(socket));
                            } else if (message.getMessageType() == -1) {
                                Message message1 = new Message();
                                send(socket, message1);
                            } else if (message.getMessageType() == constants.getHAVE()) {
                                this.bitfields.put(socket, message.getBitfield());
                                List<Integer> pieces = processBitField(message.getBitfield());
                                if (pieces.size() == 0) {
                                    /* Send Not Interested Message */
                                    Message message1 = new Message();
                                    message1.setMessageType(constants.getNOT_INTERESTED());
                                    send(socket, message1);
                                } else {
                                    Message message1 = new Message();
                                    message1.setMessageType(constants.getINTERESTED());
                                    send(socket, message1);
                                    Random random = new Random();
                                    while (pieces.size() > 0) {
                                        int remove = random.nextInt(pieces.size());
                                        Message message2 = new Message();
                                        message2.setMessageType(constants.getREQUEST());
                                        message2.setIndexField(pieces.get(remove));
                                        send(socket, message2);
                                        pieces.remove(remove);
                                    }
                                }
                                logger.info(System.currentTimeMillis() + ": Peer " + this.ID + " received 'have' message from " + ids.get(socket));
                            } else if (message.getMessageType() == constants.getINTERESTED()) {
                                Message message1 = new Message();
                                send(socket, message1);
                                logger.info(System.currentTimeMillis() + ": Peer " + this.ID + " received the ‘interested’ message from " + ids.get(socket));
                            } else if (message.getMessageType() == constants.getCHOKE()) {
                                Message message1 = new Message();
                                send(socket, message1);
                                logger.info(System.currentTimeMillis() + ": Peer " + this.ID + " is choked by " + ids.get(socket));
                            } else if (message.getMessageType() == constants.getNOT_INTERESTED()) {
                                Message message1 = new Message();
                                send(socket, message1);
                                logger.info(System.currentTimeMillis() + ": Peer " + this.ID + " received the ‘not interested’ message " + ids.get(socket));
                            } else send(socket, new Message());
                        }

                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }


    }

    //Change Neighbours wrt download rate
    public void changeNeighbours() {
        List<Socket> l = new ArrayList<>();
        l.addAll(this.connectedTo);
        l.addAll(this.connectionsFrom);
        this.neighbours = new HashSet<>();
        int size = l.size();
        while (this.neighbours.size() < Math.min(size, this.numberOfPrefferedNeighbours)) {
            this.neighbours.add(l.get(new Random().nextInt(l.size())));
        }
        for (Socket socket : this.neighbours) {
            Message message = new Message();
            message.setMessageType(constants.getUNCHOKE());
            send(socket, message);
        }
        String s = "";
        for (Socket socket : this.neighbours) s += ids.get(socket) + ",";
        if (s.length() > 1)
            logger.info(System.currentTimeMillis() + ": Peer " + this.ID + " has the preferred neighbors " + s.substring(0, s.length() - 1));
    }

    public void changeOptimisticallyNeighbours() {
        Random random = new Random();
        List<Socket> optimistic = new ArrayList<Socket>();
        optimistic.addAll(this.connectionsFrom);
        optimistic.addAll(this.connectedTo);
        if (optimistic.size() == 0) return;
        int index = random.nextInt(optimistic.size());
        while (this.optimisticUnchokedNeighbour != null && this.neighbours.contains(optimistic.get(index))) {
            index = random.nextInt(optimistic.size());
        }
        Message message = new Message();
        send(optimistic.get(index), message);
        this.optimisticUnchokedNeighbour = optimistic.get(index);
        logger.info(System.currentTimeMillis() + ": Peer " + this.ID + " has the optimistically unchoked neighbor " + ids.get(this.optimisticUnchokedNeighbour));
    }

    // Change Handshake Signal
    public void sendHandshake(Socket socket) {
        try {
            HandshakeMessage handshakeMessage = new HandshakeMessage(this.ID);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(handshakeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Send a messaage
    public void send(Socket socket, Message message) {
        try {
            Thread.sleep(1);
            socket.getOutputStream().flush();
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.flush();
            objectOutputStream.writeObject(message);
//            logger.info("Sent");
        } catch (Exception e) {
            return;
        }
    }

    //Unchoking Interval Countdown
    public void unchokingInterval() {
        while (true) {
            try {
                Thread.sleep((long)this.n * 1000);
                this.Uc = true;
                this.t2.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //optimistic Unchoked Interval Countdown
    public void optimisticUnchokedInterval() {
        while (true) {
            try {
                Thread.sleep((long)this.m * 1000);
                this.optUc = true;
                this.t2.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    //Connect this socket to other peer
    public boolean connect(String host, int PORT) {
        try {
            Socket socket = new Socket(host, PORT);
            connectedTo.add(socket);
            objectOutputStreams.put(socket, new ObjectOutputStream(socket.getOutputStream()));
            objectInputStreams.put(socket, new ObjectInputStream(socket.getInputStream()));
            /*
             * Send the handshake signal by invoking sendHandshake
             * function
             */
            sendHandshake(socket);
//            logger.info("Handshake Sent");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //Set Port number
    public void setPORT(int PORT) {
        this.PORT = PORT;
    }
}