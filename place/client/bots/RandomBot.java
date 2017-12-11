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

public class RandomBot extends Thread{

    private ObjectOutputStream out;
    private int dim;
    private String name;

    public RandomBot(ObjectOutputStream o, int d, String n) {
        out = o;
        dim = d;
        name = n;
    }

    /**
     * makes a random tile change every 3 seconds
     */
    private void addToBoard() {
        PlaceColor color = COLORS[(int)(Math.random()*15)];
        int row = (int)(Math.random()*this.dim)-1;
        int col = (int)(Math.random()*this.dim)-1;
        writeTo(out,new PlaceRequest(CHANGE_TILE,new PlaceTile(row,col,name,color)));
        try {
            sleep(3000);
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("usage $ java RandomBot host port");
            System.exit(1);
        }
        try(Socket con = new Socket(args[0],Integer.parseInt(args[1]));
            ObjectOutputStream out = new ObjectOutputStream(con.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(con.getInputStream());
        ) {
            String name = "_Random_Bot_"+System.currentTimeMillis();
            writeTo(out,new PlaceRequest(LOGIN,name));
            PlaceRequest log = (PlaceRequest)in.readUnshared();
            if(log.getType().equals(ERROR)) {
                System.exit(1);
            }
            int boardSize = ((PlaceBoard)((PlaceRequest)in.readUnshared()).getData()).DIM;
            RandomBot bot = new RandomBot(out,boardSize,name);
            while(((PlaceRequest)in.readUnshared()).getType().equals(ERROR) == false) {
                bot.addToBoard();
            }
            System.exit(1);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException cnfe) {
            System.out.println(cnfe.getMessage());
            System.exit(1);
        }
    }

}
