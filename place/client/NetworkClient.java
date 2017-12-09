package place.client;

import place.PlaceBoard;
import place.network.PlaceExchange;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkClient extends Thread {

    private String username;
    private Socket connection;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private PlaceBoard board;

    public NetworkClient(String u, Socket con, ObjectOutputStream o, ObjectInputStream i) {
        username = u;
        connection = con;
        out = o;
        in = i;
    }

    public PlaceBoard getBoard() {
        return board;
    }

    @Override
    public void run() {
        PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.LOGIN,username));
        while(!connection.isClosed()) {
            PlaceRequest request = PlaceExchange.receiveRequest(in);
            switch (request.getType()) {
                case LOGIN_SUCCESS:
                    System.out.println(request.getData().toString());
                    break;
                case BOARD:
                    board = (PlaceBoard)request.getData();
                    try {
                        this.sleep(500);
                    } catch(InterruptedException ie) {
                        System.out.println(ie.getMessage());
                        PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.ERROR,PlaceExchange.LOGGED_OUT));
                        System.exit(1);
                    }
                    break;
                case TILE_CHANGED:
                    System.out.println(request.getData().toString());
                    break;
                case ERROR:
                    System.out.println(request.getData().toString());
                    if(!(request.getData().toString().equals(PlaceExchange.BAD_USERNAME))) {
                        System.exit(1);
                    }
                    break;
                default:
                    PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.ERROR,"Cannot handle request of type " + request.getType().toString()));
                    break;
            }
        }
    }
}
