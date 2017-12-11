package place.client.bots;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.network.PlaceRequest;

import static place.network.PlaceExchange.*;
import static place.network.PlaceRequest.RequestType.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ReflectBot extends Thread{

    private ObjectOutputStream out;
    private String name;

    public ReflectBot(ObjectOutputStream o, String n) {
        out = o;
        name = n;
    }

    /**
     * makes a tile change of a random color using the location of the last tile change
     * if the last change was (row,col), this one will be (col,row)
     */
    private void addToBoard(int r, int c) {
        PlaceColor color = COLORS[(int)(Math.random()*15)];
        writeTo(out,new PlaceRequest(CHANGE_TILE,new PlaceTile(r,c,name,color)));
    }

    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("usage $ java ReflectBot host port");
            System.exit(1);
        }
        try(Socket con = new Socket(args[0],Integer.parseInt(args[1]));
            ObjectOutputStream out = new ObjectOutputStream(con.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(con.getInputStream());
        ) {
            String name = "_Reflect_Bot_"+System.currentTimeMillis();
            writeTo(out,new PlaceRequest(LOGIN,name));
            PlaceRequest log = (PlaceRequest)in.readUnshared();
            if(log.getType().equals(ERROR)) {
                System.exit(1);
            }
            ReflectBot bot = new ReflectBot(out,name);
            while(true) {
                if(((PlaceRequest)in.readUnshared()).getType().equals(TILE_CHANGED)) {
                    PlaceTile changed = (PlaceTile)((PlaceRequest)in.readUnshared()).getData();
                    int r = changed.getCol();
                    int c = changed.getRow();
                    bot.addToBoard(r,c);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException cnfe) {
            System.out.println(cnfe.getMessage());
            System.exit(1);
        }
    }

}

