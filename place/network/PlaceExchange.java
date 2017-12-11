package place.network;

import place.PlaceColor;

import static place.PlaceColor.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PlaceExchange {

    public static final String LOGGED_IN = "Login successful! Welcome, "; // append with username and IP
    public static final String BAD_USERNAME = "This username is already in use.";
    public static final String LOGGED_OUT = "This client has logged out.";
    public static final String DATA_NOT_VALID = "Received invalid data\nNo changes made to state of board";
    public static final String INVALID_TYPE = "Cannot handle requests of type "; // append with request type

    /**
     * array of colors used for tiles
     */
    public static final PlaceColor[] COLORS = {
            BLACK,GRAY,SILVER,WHITE,
            MAROON,RED,OLIVE,YELLOW,
            GREEN,LIME,TEAL,AQUA,
            NAVY,BLUE,PURPLE,FUCHSIA
    };

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

    /**
     * hash function used in username verification
     * concatenates a new String of the ASCII values for each character in this name
     * @param n the name to be hashed
     * @return the hashed name
     */
    public static String hash(String n) {
        String h = "";
        for(int i = 0; i < n.length(); i++) {
            h += (int)n.charAt(i);
        }
        return h;
    }
}
