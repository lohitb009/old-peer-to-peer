package bitTorrents;
import java.io.Serializable;
public class HandshakeMessage implements Serializable{
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int id;
    private String handshakeHeader;
    private String zeroBits;

    public HandshakeMessage(int id) {
        this.id = id;
        this.handshakeHeader = "P2PFILESHARINGPROJ";
        this.zeroBits = "0000000000";
    }
}

