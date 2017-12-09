package place.client.ptui;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.client.BoardModel;
import place.client.NetworkClient;
import place.network.PlaceExchange;
import place.network.PlaceRequest;

import java.io.*;
import java.net.Socket;
import java.util.Date;
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
        System.out.println("Place: " + username + "\n" + model.getBoard().toString() + "\n");
    }

    private PlaceColor getColor(int c) {
        for(PlaceColor color : PlaceExchange.COLORS) {
            if(c == color.getNumber()) {
                return color;

            }
        }
        return PlaceColor.BLACK;
    }

    private void logout() {
        PlaceExchange.writeTo(worker.getOutStream(),new PlaceRequest(PlaceRequest.RequestType.ERROR,PlaceExchange.LOGGED_OUT));
        System.exit(1);
    }

    private void retryLogin(String command) {
        username = command;
        worker.setUsername(command);
        PlaceExchange.writeTo(worker.getOutStream(),new PlaceRequest(PlaceRequest.RequestType.LOGIN,username));
    }

    private void formatTile(String command, long time) {
        String[] cmd = command.split(" ");
        if (cmd.length == 3) {
            PlaceColor col = getColor(Integer.parseInt(cmd[2]));
            if (col.getNumber() == Integer.parseInt(cmd[2])) {
                PlaceTile t = new PlaceTile(Integer.parseInt(cmd[0]), Integer.parseInt(cmd[1]), username, col, time);
                PlaceExchange.writeTo(worker.getOutStream(), new PlaceRequest(PlaceRequest.RequestType.CHANGE_TILE, t));
            } else {
                PlaceExchange.writeTo(worker.getOutStream(), new PlaceRequest(PlaceRequest.RequestType.ERROR, PlaceExchange.DATA_NOT_VALID));
            }
        }
    }

    @Override
    public void run() {
        while(worker.running()) {
            System.out.println("\n");
            try {

                if(NetworkClient.loginFailed) {
                    System.out.println("Try another username, or -1 to exit. ");
                }
                else {
                    System.out.println("Move[row col color]: ");
                }
                String command = userInput.readLine();
                Long time = System.currentTimeMillis();

                if((command.length() == 2) && (Integer.parseInt(command) == -1)) {
                    logout();
                }

                else if(NetworkClient.loginFailed) {
                    retryLogin(command);
                }

                else {
                    formatTile(command, time);
                }

                sleep(500);
                model.notifier();

            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            } catch (InterruptedException ie) {
                System.out.println(ie.getMessage());
                PlaceExchange.writeTo(worker.getOutStream(),new PlaceRequest(PlaceRequest.RequestType.ERROR,PlaceExchange.LOGGED_OUT));
                System.exit(1);
            }
            System.out.println("\n");
        }
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
            ui.start();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
        }
    }

}
