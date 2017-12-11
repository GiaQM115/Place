package place.client;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;

import static place.network.PlaceExchange.*;
import static place.network.PlaceRequest.RequestType.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Observer;

public class NetworkClient extends Thread {

    public static boolean loginFailed;

    private String username;
    private Socket connection;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private BoardModel model;
    private Observer ui;

    public NetworkClient(String u, Socket con, ObjectOutputStream o, ObjectInputStream i, Observer observer) {
        username = u;
        connection = con;
        out = o;
        in = i;
        ui = observer;
    }

    /**
     * what is this clients username?
     * @return String with the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * used by the UI when a PlaceRequest needs to be sent from there rather than in NetworkClient
     * @return the output stream associated with this clients connection
     */
    public ObjectOutputStream getOutStream() {
        return out;
    }

    /**
     * is this connection still active?
     * @return true if the Socket is open, else false
     */
    public boolean running() {
        return !(connection.isClosed());
    }

    /**
     * what does the clients board look like?
     * @return this clients PlaceBoard object
     */
    public PlaceBoard getBoard() {
        if(model == null) {
            return null;
        }
        return model.getBoard();
    }

    /**
     * used when login is failed, just re-sends login request with the new name
     * @param command the new name to try with
     */
    public void retryLogin(String command) {
        writeTo(out,new PlaceRequest(LOGIN,command));
    }

    /**
     * tells the server this user is logging out and exits
     */
    public void logout() {
        writeTo(out,new PlaceRequest(ERROR,LOGGED_OUT));
        System.exit(1);
    }

    @Override
    public void run() {
        writeTo(out,new PlaceRequest(LOGIN,username));
        while(!connection.isClosed()) {
            PlaceRequest request = receiveRequest(in);
            switch (request.getType()) {

                case LOGIN_SUCCESS:
                    String data = request.getData().toString();
                    String ip = connection.getLocalSocketAddress().toString();
                    username = data.substring(LOGGED_IN.length(),(data.length() - ip.length()));
                    loginFailed = false;
                    System.out.println(data);
                    break;

                case BOARD:
                    model = new BoardModel((PlaceBoard)request.getData());
                    model.addObserver(ui);
                    try {
                        this.sleep(500);
                    } catch(InterruptedException ie) {
                        System.out.println(ie.getMessage());
                        writeTo(out,new PlaceRequest(ERROR,LOGGED_OUT));
                        System.exit(1);
                    }
                    break;

                case TILE_CHANGED:
                    PlaceTile current = (PlaceTile)request.getData();
                    model.update(current);
                    model.notifier(current);
                    break;

                case ERROR:
                    System.out.println(request.getData().toString());
                    if(request.getData().toString().equals(BAD_USERNAME)) {
                        loginFailed = true;
                    }
                    else if(!request.getData().toString().equals(DATA_NOT_VALID)) {
                        System.exit(1);
                    }
                    break;

                default:
                    writeTo(out,new PlaceRequest(ERROR,INVALID_TYPE + request.getType().toString()));
                    break;
            }
        }
    }
}
