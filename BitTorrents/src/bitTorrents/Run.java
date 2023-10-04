package bitTorrents;
import java.net.*;
public class Run {
    public static void main(String[] args){
        try {
            String workingDir = (System.getProperty("user.dir"));
            System.out.println(workingDir);
            Runtime.getRuntime().exec("ssh " + "lin114-00.cise.ufl.edu" + " cd " + workingDir + " ; " +
                    "bitTorrents/peer_1001/PeerProcess1001" + " 1003");
        }catch(Exception e){e.printStackTrace();}
    }
}
