package place.server;

import place.PlaceBoard;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class PlaceServer {

    protected static HashMap<String,ClientThread> users = new HashMap<>();
    protected static PlaceBoard masterBoard;

    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("usage $ java PlaceServer port dim");
            System.exit(1);
        }
        else if(!(Integer.parseInt(args[1]) >= 1)) {
            System.out.println("dim must be at least 1");
            System.exit(1);
        }
        masterBoard = new PlaceBoard(Integer.parseInt(args[1]));
        System.out.println("Server started!");
        System.out.println("Waiting for new connections...\n");
            try {
                ServerSocket serverConnection = new ServerSocket(Integer.parseInt(args[0]));
                while(true) {
                    Socket clientConnection = serverConnection.accept();
                    ObjectInputStream in = new ObjectInputStream(clientConnection.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(clientConnection.getOutputStream());
                    System.out.println("Connection found!\n");
                    ClientThread worker = new ClientThread("", clientConnection, in, out, masterBoard);
                    worker.join();
                    worker.start();
                }
            } catch(IOException ioe) {
                System.out.println(ioe.getMessage());
                System.exit(1);
            } catch(InterruptedException ie) {
                System.out.println(ie.getMessage());
                System.exit(1);
            }
    }

}
