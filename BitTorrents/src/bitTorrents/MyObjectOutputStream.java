package bitTorrents;
import java.io.*;
public class MyObjectOutputStream extends ObjectOutputStream {

    // Constructor of ths class
    // 1. Default
    public MyObjectOutputStream() throws IOException
    {

        // Super keyword refers to parent class instance
        super();
    }

    // Constructor of ths class
    // 1. Parameterized constructor
    public MyObjectOutputStream(OutputStream o) throws IOException
    {
        super(o);
    }

    // Method of this class
    @Override
    public void writeStreamHeader() throws IOException
    {
        return;
    }
}
