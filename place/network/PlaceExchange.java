package place.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PlaceExchange {

    public static final String LOGGED_IN = "Place: "; // append with username and IP
    public static final String BAD_USERNAME = "This username is already in use.";
    public static final String LOGGED_OUT = "This client has logged out.";

    /**
     * used by clients and server to send packets back and forth
     * @param writer the ObjectOutputStream linked to this object
     * @param request the PlaceRequest that is to be sent
     */
    public static void writeTo(ObjectOutputStream writer, PlaceRequest request) {
        try {
            writer.writeUnshared(request);
            writer.flush();
        } catch(IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    /**
     * used by clients and server to read packets sent back and forth
     * @param reader the ObjectInputReader linked to this object
     * @return a PlaceRequest to continue the communication
     */
    public static PlaceRequest receiveRequest(ObjectInputStream reader) {
        PlaceRequest request = new PlaceRequest(PlaceRequest.RequestType.ERROR,"An error occurred while reading the packet");
        try {
            Object o = reader.readUnshared();
            if(o instanceof PlaceRequest) {
                return (PlaceRequest)o;
            }
        } catch(IOException ioe) {
            System.out.println(ioe.getMessage());
        } catch(ClassNotFoundException cnfe) {
            System.out.println(cnfe.getMessage());
        }
        return request;
    }

    public static String hash(String n) {
        String h = "";
        for(int i = 0; i < n.length(); i++) {
            h += (int)n.charAt(i);
        }
        return h;
    }
}
