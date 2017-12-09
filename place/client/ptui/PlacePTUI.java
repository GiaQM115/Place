package place.client.ptui;

import place.PlaceBoard;
import place.PlaceTile;
import place.client.BoardModel;
import place.client.NetworkClient;

import java.io.*;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

public class PlacePTUI extends Thread implements Observer{

    private String username;
    private BufferedReader userInput;
    private BoardModel model;
    private NetworkClient worker;

    public PlacePTUI(String u, Socket con, ObjectOutputStream o, ObjectInputStream i) {
        username = u;
        userInput = new BufferedReader(new InputStreamReader(System.in));
        worker = new NetworkClient(u, con, o, i);
    }

    public void setBoard(PlaceBoard b) {
        model = new BoardModel(b);
        model.addObserver(this);
        updateView();
    }

    public void update(Observable t, Object o) {
        ((BoardModel)t).update((PlaceTile)o);
        updateView();
    }

    public void updateView() {
        System.out.println(model.getBoard().toString());
    }

    public static void main(String[] args) {
        if(args.length != 3) {
            System.out.println("usage $ java PlacePTUI host port username");
            System.exit(1);
        }
        try {
            Socket connection = new Socket(args[0], Integer.parseInt(args[1]));
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            PlacePTUI ui = new PlacePTUI(args[2],connection,out,in);
            ui.worker.start();
            ui.sleep(1000);
            ui.setBoard(ui.worker.getBoard());
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
        }
    }

}
