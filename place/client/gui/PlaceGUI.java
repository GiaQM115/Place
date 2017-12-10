package place.client.gui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import javafx.stage.Window;
import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.client.NetworkClient;
import place.network.PlaceRequest;

import static java.lang.Thread.sleep;
import static place.PlaceColor.*;
import static place.network.PlaceExchange.*;
import static place.network.PlaceRequest.RequestType.*;;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class PlaceGUI extends Application implements Observer{

    private BorderPane wrapperInit = new BorderPane();
    private HBox topBoxes = new HBox(0.5);
    private HBox bottomBoxes = new HBox(0.5);
    private VBox leftBoxes = new VBox(0);
    private VBox rightBoxes = new VBox(0);
    private VBox centerBoxes = new VBox();
    private Label welcome = new Label("Welcome to Place!");
    private Button enter = new Button("Enter");
    private TextField newName = new TextField();

    private Stage stage;
    private List<String> params = null;

    private HashMap<String,Scene> scenes = new HashMap<>();
    private String username = "";
    private PlaceBoard board = null;
    private NetworkClient worker;


    public void initBoard() {
        while(worker.getBoard() == null) {
            try {
                sleep(100);
            } catch(InterruptedException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        }
        board = worker.getBoard();
        updateView("PLACE");
    }

    public void updateView(String scene) {
        System.out.println(scene);
        javafx.application.Platform.runLater( () -> {
            stage.setScene(scenes.get(scene));
        });
    }

    public void refresh() {
        // refreshes tiles on board
    }

    public void update(Observable t,Object o) {
    }

    public void connect(String host, int port, String name) throws IOException{
        Socket connection = new Socket(host,port);
        ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
        worker = new NetworkClient(name,connection,out,in,this);
        worker.start();
    }

    @Override
    public void init() {
        try {
            super.init();
            params = super.getParameters().getUnnamed();
            if(params.size() != 3) {
                System.out.println("Usage $ java PlaceGUI host port username");
                System.exit(1);
            }
            String host = params.get(0);
            int port = Integer.parseInt(params.get(1));
            String name = params.get(2);
            connect(host, port, name);
        } catch(IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } catch(Exception e2) {
            System.out.println(e2.getMessage());
            System.exit(1);
        }
    }

    public void start(Stage s) {
        stage = s;
        stage.setResizable(false);
        stage.sizeToScene();
        initial();
        if(worker.loginFailed) {
            relog();
        }
        else {
            username = worker.getUsername();
        }
        stage.setScene(scenes.get("INITIAL"));
        stage.setTitle("Place: " + username);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    private void initial() {
        for(int i = 0; i < 16; i++) {
            Color c = Color.rgb(COLORS[i].getRed(),COLORS[i].getGreen(),COLORS[i].getBlue());
            if(i < 5) {
                topBoxes.getChildren().add(new Rectangle(100, 100, c));
            }
            else if(i < 8) {
                rightBoxes.getChildren().add(new Rectangle(100, 100, c));
            }
            else if(i < 13) {
                bottomBoxes.getChildren().add(new Rectangle(100, 100, c));
            }
            else {
                leftBoxes.getChildren().add(new Rectangle(100, 100, c));
            }
        }

        welcome.setTextAlignment(TextAlignment.CENTER);
        welcome.setTextFill(Color.BLACK);
        welcome.setAlignment(Pos.CENTER);
        welcome.setFont(Font.font("Verdana",24));

        centerBoxes.getChildren().addAll(welcome,enter);
        centerBoxes.setAlignment(Pos.CENTER);
        centerBoxes.setStyle("-fx-background-color: #FFFFFF");

        enter.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent event) {
                if(worker.loginFailed) {
                    worker.retryLogin(newName.getText());
                }
                else {
                    initBoard();
                }
            }
        });
        enter.setTextFill(Color.BLACK);
        enter.setFont(Font.font("Verdana",12));

        wrapperInit.setTop(topBoxes);
        wrapperInit.setBottom(bottomBoxes);
        wrapperInit.setLeft(leftBoxes);
        wrapperInit.setCenter(centerBoxes);
        wrapperInit.setRight(rightBoxes);

        scenes.put("INITIAL",new Scene(wrapperInit));
    }

    private void relog() {
        welcome.setText("Username taken");
        welcome.setFont(Font.font("Verdana",14));

        centerBoxes.getChildren().add(1,newName);
    }
}
