package place.server;

import place.PlaceBoard;
import place.network.PlaceExchange;
import place.network.PlaceRequest;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientThread extends Thread{

    private String username;
    private Socket client;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private PlaceBoard board;

    public ClientThread(String u, Socket c, ObjectInputStream i, ObjectOutputStream o, int d) {
        username = u;
        client = c;
        in = i;
        out = o;
        board = new PlaceBoard(d);
    }

    protected boolean login(String name) {
        if(PlaceServer.users.isEmpty()) {
            PlaceServer.users.put(PlaceExchange.hash(name),this);
        }
        else if(PlaceServer.users.containsKey(PlaceExchange.hash(name))) {
            return false;
        }
        else {
            PlaceServer.users.put(PlaceExchange.hash(name),this);
        }
        return true;
    }

    @Override
    public void run() {
        while(!client.isClosed()) {
            PlaceRequest request = PlaceExchange.receiveRequest(in);
            switch (request.getType()) {
                case LOGIN:
                    String name = request.getData().toString();
                    System.out.println("Logging in " + name);
                    if(login(name)) {
                        PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.LOGIN_SUCCESS,PlaceExchange.LOGGED_IN + name + client.getRemoteSocketAddress().toString()));
                        PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.BOARD,board));
                    }
                    else {
                        PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.ERROR,PlaceExchange.BAD_USERNAME));
                    }
                    break;
                case CHANGE_TILE:
                    System.out.println(request.getData().toString());
                    break;
                case ERROR:
                    System.out.println(request.getData().toString());
                    if(!(request.getData().toString().equals(PlaceExchange.LOGGED_OUT))) {
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
