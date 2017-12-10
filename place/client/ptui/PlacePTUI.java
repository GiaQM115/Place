package place.client.ptui;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.client.NetworkClient;
import place.network.PlaceRequest;

import static place.PlaceColor.*;
import static place.network.PlaceExchange.*;
import static place.network.PlaceRequest.RequestType.*;;

import java.io.*;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

public class PlacePTUI extends Thread implements Observer{

    private String username;
    private BufferedReader userInput;
    private PlaceBoard board;
    private NetworkClient worker;

    public PlacePTUI(String u) {
        username = u;
        userInput = new BufferedReader(new InputStreamReader(System.in));
    }

    public void initBoard() {
        board = worker.getBoard();
        updateView();
    }

    public void setWorker(Socket con, ObjectOutputStream o, ObjectInputStream i) {
        worker = new NetworkClient(username, con, o, i, this);
    }

    public void update(Observable t, Object o) {
        board.setTile((PlaceTile)o);
        String owner = ((PlaceTile)o).getOwner();
        if(!owner.equals(username)) {
            System.out.println("\n" + owner + " changed a tile!");
        }
        updateView();
        if(!owner.equals(username)) {
            System.out.println("\nMove[row col color]: ");
        }
    }

    public void updateView() {
        System.out.println("Place: " + username + board.toString() + "\n");
    }

    private PlaceColor getColor(int c) {
        for(PlaceColor color : COLORS) {
            if(c == color.getNumber()) {
                return color;
            }
        }
        return BLACK;
    }

    private void formatTile(int[] cmd, long time) {
        if (cmd.length == 3) {
            PlaceColor col = getColor(cmd[2]);
            if (col.getNumber() == (cmd[2])) {
                PlaceTile t = new PlaceTile((cmd[0]), (cmd[1]), username, col, time);
                writeTo(worker.getOutStream(), new PlaceRequest(CHANGE_TILE, t));
            } else {
                writeTo(worker.getOutStream(), new PlaceRequest(ERROR, DATA_NOT_VALID));
            }
        }
    }

    @Override
    public void run() {
        while(worker.running()) {
            try {

                if(NetworkClient.loginFailed) {
                    System.out.println("Type another username, or -1 to exit.");
                }
                else {
                    if(!username.equals(worker.getUsername())) {
                        username = worker.getUsername();
                    }
                    if(board == null) {
                        initBoard();
                    }
                    System.out.println("Move[row col color]: ");
                }
                String command = userInput.readLine();
                Long time = System.currentTimeMillis();

                if((command.length() == 2) && (Integer.parseInt(command) == -1)) {
                    worker.logout();
                }

                else if(NetworkClient.loginFailed) {
                    worker.retryLogin(command);
                }

                else {
                    String[] temp = command.split(" ");
                    int[] cmd = {Integer.parseInt(temp[0]), Integer.parseInt(temp[1]), Integer.parseInt(temp[2])};
                    if((cmd[0] >= 0) && (cmd[0] < board.DIM) && (cmd[1] >= 0) && (cmd[1] < board.DIM)) {
                        formatTile(cmd,time);
                    }
                    else {
                        writeTo(worker.getOutStream(),new PlaceRequest(ERROR,DATA_NOT_VALID));
                        System.out.println(DATA_NOT_VALID);
                    }
                }

                sleep(500);

            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            } catch (InterruptedException ie) {
                System.out.println(ie.getMessage());
                writeTo(worker.getOutStream(),new PlaceRequest(ERROR,LOGGED_OUT));
                System.exit(1);
            }
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
            PlacePTUI ui = new PlacePTUI(args[2]);
            ui.setWorker(connection,out,in);
            ui.worker.start();
            ui.sleep(1000);
            ui.start();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage() + "\nExiting.");
            System.exit(1);
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage() + "\nExiting.");
            System.exit(1);
        }
    }

}
