package bitTorrents;
public class Constants {
    final private byte CHOKE = 0;
    final private byte UNCHOKE = 1;
    final private byte INTERESTED = 2;
    final private byte NOT_INTERESTED = 3;
    final private byte HAVE = 4;
    final private byte BITFIELD = 5;
    final private byte REQUEST = 6;
    final private byte PIECE = 7;

    public byte getCHOKE() {
        return CHOKE;
    }

    public byte getUNCHOKE() {
        return UNCHOKE;
    }

    public byte getINTERESTED() {
        return INTERESTED;
    }

    public byte getNOT_INTERESTED() {
        return NOT_INTERESTED;
    }

    public byte getHAVE() {
        return HAVE;
    }

    public byte getBITFIELD() {
        return BITFIELD;
    }

    public byte getREQUEST() {
        return REQUEST;
    }

    public byte getPIECE() {
        return PIECE;
    }


}
