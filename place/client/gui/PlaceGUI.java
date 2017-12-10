package place.client.gui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
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

    private List<String> params = null;
    private Stage stage;

    private HashMap<String,Scene> scenes;
    private String username;
    private PlaceBoard board;
    private NetworkClient worker;

    public PlaceGUI(String u) {
        username = u;
        scenes = new HashMap<>();
    }

    public void initBoard() {
        board = worker.getBoard();
        updateView("PLACE");
    }

    public void setWorker(Socket con, ObjectOutputStream o, ObjectInputStream i) {
        worker = new NetworkClient(username, con, o, i, this);
    }

    public void updateView(String scene) {
        javafx.application.Platform.runLater( () -> {
            stage.setScene(scenes.get(scene));
        });
    }

    public void refresh() {
        // refreshes tiles on board
    }

    private Scene initial() {
        BorderPane wrapper = new BorderPane();

        HBox top = new HBox(0.5);
        HBox bottom = new HBox(0.5);
        VBox left = new VBox(0);
        VBox right = new VBox(0);
        VBox center = new VBox();
        Label welcome = new Label("Welcome to Place!");
        Button enter = new Button("Enter");
        enter.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent event) {
                // next scene
            }
        });

        for(int i = 0; i < 10; i++) {
            if(i < 3) {
                top.getChildren().add(new Rectangle(60, 60, Color.web(COLORS[i].toString())));
            }
            else if(i < 5) {
                right.getChildren().add(new Rectangle(60, 60, Color.web(COLORS[i].toString())));
            }
            else if(i < 8) {
                bottom.getChildren().add(new Rectangle(60, 60, Color.web(COLORS[i].toString())));
            }
            else {
                left.getChildren().add(new Rectangle(60, 60, Color.web(COLORS[i].toString())));
            }
        }

        // add label and button to vbox and vbox to gridpane

        welcome.setTextAlignment(TextAlignment.CENTER);
        welcome.setAlignment(Pos.CENTER);

        wrapper.setTop(top);
        wrapper.setBottom(bottom);
        wrapper.setLeft(left);
        wrapper.setRight(right);

        return new Scene(wrapper);
    }

    public void update(Observable t,Object o) {

    }

    @Override
    public void init() {
        try {
            super.init();
            params = super.getParameters().getUnnamed();
            if (params.size() != 3) {
                System.out.println("Usage $ java PlaceGUI host port username");
                System.exit(1);
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void start(Stage s) {
        stage = s;
        stage.setScene(initial());
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
