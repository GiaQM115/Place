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

public class EraseBot extends Thread{

    private ObjectOutputStream out;
    private String name;

    public EraseBot(ObjectOutputStream o, String n) {
        out = o;
        name = n;
    }

    /**
     * waits 2 seconds after a user changes a tile, and then makes it white again
     */
    private void addToBoard(int r, int c) {
        try {
            sleep(2000);
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        writeTo(out,new PlaceRequest(CHANGE_TILE,new PlaceTile(r,c,name,PlaceColor.WHITE)));
    }

    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("usage $ java EraseBot host port");
            System.exit(1);
        }
        try(Socket con = new Socket(args[0],Integer.parseInt(args[1]));
            ObjectOutputStream out = new ObjectOutputStream(con.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(con.getInputStream());
        ) {
            String name = "_Erase_Bot_"+System.currentTimeMillis();
            writeTo(out,new PlaceRequest(LOGIN,name));
            PlaceRequest log = (PlaceRequest)in.readUnshared();
            if(log.getType().equals(ERROR)) {
                System.exit(1);
            }
            EraseBot bot = new EraseBot(out,name);
            while(true) {
                if(((PlaceRequest)in.readUnshared()).getType().equals(TILE_CHANGED)) {
                    PlaceTile changed = (PlaceTile)((PlaceRequest)in.readUnshared()).getData();
                    int c = changed.getCol();
                    int r = changed.getRow();
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

