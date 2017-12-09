package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceExchange;
import place.network.PlaceRequest;

import java.io.IOException;
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

    protected synchronized boolean login(String name) {
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

    private synchronized void attemptLogin(String name) {
        System.out.println("Logging in " + name);
        if(login(name)) {
            username = name;
            PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.LOGIN_SUCCESS,PlaceExchange.LOGGED_IN + name + client.getRemoteSocketAddress().toString()));
            PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.BOARD,board));
        }
        else {
            PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.ERROR,PlaceExchange.BAD_USERNAME));
        }
    }

    private void logout() {
        try {
            in.close();
            out.close();
            client.close();
        } catch(IOException ioe) {
            System.out.println(ioe.getMessage());
        } finally {
            System.exit(1);
        }
    }

    private synchronized void updateBoard(PlaceTile tile) {
        tile.setOwner(username);
        board.setTile(tile);
       broadcast(new PlaceRequest(PlaceRequest.RequestType.TILE_CHANGED,tile));
    }

    private synchronized void broadcast(PlaceRequest request) {
        for(String key : PlaceServer.users.keySet()) {
            ClientThread ct = (ClientThread)PlaceServer.users.get(key);
            PlaceExchange.writeTo(ct.out,request);
        }
    }

    @Override
    public void run() {
        while(!client.isClosed()) {
            PlaceRequest request = PlaceExchange.receiveRequest(in);
            switch (request.getType()) {

                case LOGIN:
                    attemptLogin(request.getData().toString());
                    break;

                case CHANGE_TILE:
                    updateBoard((PlaceTile) request.getData());
                    break;

                case ERROR:
                    System.out.println(request.getData().toString());
                    if(request.getData().toString().equals(PlaceExchange.LOGGED_OUT)) {
                        logout();
                    }
                    else if(!request.getData().toString().equals(PlaceExchange.DATA_NOT_VALID)) {
                        System.exit(1);
                    }
                    break;

                default:
                    PlaceExchange.writeTo(out,new PlaceRequest(PlaceRequest.RequestType.ERROR,PlaceExchange.INVALID_TYPE + request.getType().toString()));
                    break;
            }
        }
    }

}
