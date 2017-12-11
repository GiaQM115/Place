package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static place.server.PlaceServer.*;
import static place.network.PlaceExchange.*;
import static place.network.PlaceRequest.RequestType.*;

public class ClientThread extends Thread{

    private String username;
    private Socket client;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private PlaceBoard board;

    public ClientThread(String u, Socket c, ObjectInputStream i, ObjectOutputStream o, PlaceBoard b) {
        username = u;
        client = c;
        in = i;
        out = o;
        board = b;
    }

    /**
     * login procedure; verifies the username is unique, and adds to the map of users
     * @param name the username trying to log in
     * @return true if the name is unique, false if it's already taken
     */
    private synchronized boolean login(String name) {
        if(users.isEmpty()) {
            users.put(hash(name),this);
        }
        else if(users.containsKey(hash(name))) {
            return false;
        }
        else {
            users.put(hash(name),this);
        }
        System.out.println("Login success!");
        System.out.println("Users online: " + users.keySet().size());
        return true;
    }

    /**
     * doctors the name (replaces spaced with an underscore)
     * sends the new name to login
     * sends a PlaceRequest according to the outcome (whether or not the user was logged in)
     * @param nameSent the name the user wants to log in as
     */
    private synchronized void attemptLogin(String nameSent) {
        String name = "";
        for(int i = 0; i < nameSent.length(); i++) {
            if((int)nameSent.charAt(i) != (int)' ') {
                name += nameSent.charAt(i);
            }
            else {
                name += '_';
            }
        }
        System.out.println("Attemping to log in new user " + name + "\n");
        if(login(name)) {
            username = name;
            writeTo(out,new PlaceRequest(LOGIN_SUCCESS,LOGGED_IN + name + client.getRemoteSocketAddress().toString()));
            writeTo(out,new PlaceRequest(BOARD,board));
        }
        else {
            System.out.println("Username unavailable\n");
            writeTo(out,new PlaceRequest(ERROR,BAD_USERNAME));
        }
    }

    /**
     * closes streams and Socket
     * exits program
     */
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

    /**
     * sets the user who sent this tile as the owner, regardless of what the user logged as the name
     * updates own board
     * broadcasts TILE_CHANGED
     * @param tile the tile that a user wants to change
     */
    private synchronized void updateBoard(PlaceTile tile) {
        tile.setOwner(username);
        board.setTile(tile);
       broadcast(new PlaceRequest(TILE_CHANGED,tile));
    }

    /**
     * sends the passed in PlaceRequest to all active users
     * @param request the request to be sent
     */
    private synchronized void broadcast(PlaceRequest request) {
        for(String key : users.keySet()) {
            ClientThread ct = users.get(key);
            writeTo(ct.out,request);
        }
    }

    @Override
    public void run() {
        while(!client.isClosed()) {
            PlaceRequest request = receiveRequest(in);
            switch (request.getType()) {

                case LOGIN:
                    attemptLogin(request.getData().toString());
                    break;

                case CHANGE_TILE:
                    updateBoard((PlaceTile)request.getData());
                    break;

                case ERROR:
                    System.out.println(request.getData().toString());
                    if(request.getData().toString().equals(LOGGED_OUT)) {
                        logout();
                    }
                    break;

                default:
                    writeTo(out,new PlaceRequest(ERROR,INVALID_TYPE + request.getType().toString()));
                    break;
            }
        }
    }

}
