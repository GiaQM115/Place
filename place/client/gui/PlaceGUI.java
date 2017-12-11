package place.client.gui;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.client.NetworkClient;
import place.network.PlaceRequest;

import static java.lang.Thread.sleep;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static place.PlaceColor.*;
import static place.network.PlaceExchange.*;
import static place.network.PlaceRequest.RequestType.*;;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class PlaceGUI extends Application implements Observer{

    /*
    A COMPLETE MESS OF GARBAGE
    These are the elements that are accessed in more than one method without being passed in (IE what's updated on screen)
     */
    private BorderPane wrapperInit = new BorderPane();
    private HBox topBoxes = new HBox(0.5);
    private HBox bottomBoxes = new HBox(0.5);
    private VBox leftBoxes = new VBox(0);
    private VBox rightBoxes = new VBox(0);
    private VBox centerBoxes = new VBox();
    private Label welcome = new Label("Welcome to Place!");
    private Button enter = new Button("Enter");
    private TextField newName = new TextField();
    private VBox placeBox = new VBox();
    private Label mostRecent = new Label("No one has changed a tile since you logged on.");
    private TilePane boardGrid = new TilePane();
    private ToggleGroup colorToggles = new ToggleGroup();
    private HBox colors = new HBox();
    private HBox placeData = new HBox();
    private Label currentLoc = new Label("Current Tile");
    private Label currentOwner = new Label("Owner: ");
    private Label currentTime = new Label("Changed at: ");
    private Label currentColor = new Label("Color: ");
    VBox currentTile = new VBox();

    private String tileRow = "";
    private String tileCol = "";
    private String tileOwner = "";
    private String tileTime = "";
    private String tileColor = "";

    private PlaceColor placecol = WHITE;
    private ArrayList<Rectangle> rectangles = new ArrayList<>();

    private Stage stage;
    private List<String> params = null;

    private HashMap<String,Scene> scenes = new HashMap<>();
    private String username = "";
    private PlaceBoard board = null;
    private NetworkClient worker;


    /**
     * stores a copy of the current PlaceBoard when the worker thread receives it from the server
     */
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
        place();
        updateView("PLACE");
    }

    /**
     * changes graphics on screen
     * @param scene name of the scene to be displayed in HashMap
     */
    public void updateView(String scene) {
        javafx.application.Platform.runLater( () -> {
            currentLoc.setText("Current Tile at (" + tileRow + "," + tileCol + ")");
            currentOwner.setText("Owner: " + tileOwner);
            currentTime.setText("Changed at: " + tileTime);
            currentColor.setText("Color: " + tileColor);
            placeData.getChildren().remove(1);
            placeData.getChildren().add(new VBox(currentLoc,currentOwner,currentTime,currentColor));
            Color bg = Color.rgb(placecol.getRed(),placecol.getGreen(),placecol.getBlue());
            placeBox.setBackground(new Background(new BackgroundFill(bg,CornerRadii.EMPTY,Insets.EMPTY)));
            if(placecol.equals(BLACK)) {
                mostRecent.setTextFill(Color.rgb(255,255,255));
                currentColor.setTextFill(Color.rgb(255,255,255));
                currentOwner.setTextFill(Color.rgb(255,255,255));
                currentLoc.setTextFill(Color.rgb(255,255,255));
                currentTime.setTextFill(Color.rgb(255,255,255));
            }
            else {
                mostRecent.setTextFill(Color.rgb(0,0,0));
                currentColor.setTextFill(Color.grayRgb(0));
                currentOwner.setTextFill(Color.grayRgb(0));
                currentLoc.setTextFill(Color.grayRgb(0));
                currentTime.setTextFill(Color.grayRgb(0));
            }
            stage.setScene(scenes.get(scene));
            stage.show();
        });
    }

    /**
     * updates board with tile changes when model is changed and observers are notified
     * @param t BoardModel with PlaceBoard
     * @param o PlaceTile that was changed
     */
    public void update(Observable t,Object o) {
        board.setTile((PlaceTile)o);
        String owner = ((PlaceTile)o).getOwner();
        javafx.application.Platform.runLater( () -> {
            refreshTiles(((PlaceTile)o),owner);
        });
        updateView("PLACE");
    }

    /**
     * changes the colors of the tiles on screen to match the updated BoardModel
     * @param t the most recent tile change
     * @param o the owner of that tile
     */
    private void refreshTiles(PlaceTile t, String o) {
        String owns = o;
        if(username.equals(o)) {
            owns = "You";
        }
        for(int i = 0; i < rectangles.size(); i++) {
            if((i/board.DIM == t.getRow()) && (i%board.DIM == t.getCol())) {
                rectangles.get(i).setFill(Color.rgb(t.getColor().getRed(),t.getColor().getGreen(),t.getColor().getBlue()));
                break;
            }
        }
        mostRecent.setText(owns + " made the most recent tile change!");
    }

    /**
     * creates the connection between the server and worker thread, and begins login communications
     * @param host host name
     * @param port port number
     * @param name desired username
     * @throws IOException
     */
    public void connect(String host, int port, String name) throws IOException{
        Socket connection = new Socket(host,port);
        ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
        worker = new NetworkClient(name,connection,out,in,this);
        worker.start();
    }

    /**
     * called super.init() for javafx application launch
     * sends parameters into connect to start worker thread
     */
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

    /**
     * displays initial scene
     * @param s the Stage this application will use
     */
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

    /*
    ALL OF THE METHODS BELOW HERE ARE STRICTLY FOR STYLING GUI ELEMENTS
    it's ugly spaghetti code
    GL, Friend
     */

    /**
     * creates the border of filled rectangles on login and entry screens
     */
    private void fillRectangles() {
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
    }

    /**
     * styles the labels, button, and textfield for login and entry screens
     */
    private void formatWelcomeScreen() {
        welcome.setTextAlignment(TextAlignment.CENTER);
        welcome.setTextFill(Color.BLACK);
        welcome.setAlignment(CENTER);
        welcome.setFont(Font.font("Verdana",24));

        centerBoxes.getChildren().addAll(welcome,enter);
        centerBoxes.setAlignment(CENTER);
        centerBoxes.setStyle("-fx-background-color: #FFFFFF");

        enter.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent event) {
                handleEnter();
            }
        });
        enter.setTextFill(Color.BLACK);
        enter.setFont(Font.font("Verdana",12));
    }

    /**
     * the method that's called when enter is pressed;
     * decides action based on whether user is viewing login or entry screen
     */
    private void handleEnter() {
        if(worker.loginFailed) {
            worker.retryLogin(newName.getText());
            try {
                sleep(500);
            } catch(InterruptedException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
            reinit();
            updateView("INITIAL");
        }
        else {
            initBoard();
        }
    }

    /**
     * creates and builds the scene I keep calling entry screen
     */
    private void initial() {
        fillRectangles();
        formatWelcomeScreen();
        wrapperInit.setTop(topBoxes);
        wrapperInit.setBottom(bottomBoxes);
        wrapperInit.setLeft(leftBoxes);
        wrapperInit.setCenter(centerBoxes);
        wrapperInit.setRight(rightBoxes);
        scenes.put("INITIAL",new Scene(wrapperInit));
    }

    /**
     * called when the username passed in from parameters is taken
     * modifies entry screen to become login screen
     * allows user to try a new name
     */
    private void relog() {
        welcome.setText("Username taken\nTry another name.\n\n\n");
        welcome.setFont(Font.font("Verdana",14));
        centerBoxes.getChildren().add(1,newName);
    }

    /**
     * called when the user finally finds a name that can be used
     * re-creates entry screen
     */
    public void reinit() {
        if(worker.loginFailed) {
            newName.clear();
        }
        else {
            welcome.setText("Welcome to Place");
            welcome.setFont(Font.font("Verdana", 24));
            centerBoxes.getChildren().remove(1);
        }
    }

    /**
     * builds and displays the main screen, where the user interacts with the PlaceBoard representation
     * it's a GridPane of rectangles
     */
    private void place() {
        boardGrid.setPrefRows(board.DIM);
        boardGrid.setPrefColumns(board.DIM);
        boardGrid.setAlignment(CENTER);
        boardGrid.setTileAlignment(CENTER);
        rectangles = fillPlace();
        boardGrid.getChildren().addAll(rectangles);

        mostRecent.setFont(Font.font("Verdana",14));
        mostRecent.setAlignment(CENTER_LEFT);
        mostRecent.setTextAlignment(TextAlignment.CENTER);
        mostRecent.setWrapText(true);

        currentTile.getChildren().addAll(currentLoc,currentOwner,currentTime,currentColor);
        currentTile.setAlignment(CENTER_RIGHT);


        placeData.getChildren().addAll(mostRecent,currentTile);
        placeData.setFillHeight(true);
        placeData.setAlignment(CENTER);

        createColorBar();
        colors.setAlignment(CENTER);
        colors.setFillHeight(true);

        placeBox.getChildren().addAll(placeData,boardGrid,colors);

        scenes.put("PLACE",new Scene(placeBox));
    }

    /**
     * create the rectangles for the GridPane mentioned above
     * @return a list of rectangle objects that matches the current board at the time of login
     */
    private ArrayList<Rectangle> fillPlace() {
        ArrayList<Rectangle> rects = new ArrayList<>();
        for(int i = 0; i < Math.pow(board.DIM,2); i++) {
            double tileDim = Math.sqrt(250000/(Math.pow(board.DIM,2)));
            PlaceTile t = board.getTile(i/board.DIM,i%board.DIM);
            PlaceColor pc = t.getColor();
            Color c = Color.rgb(pc.getRed(),pc.getGreen(),pc.getBlue());
            rects.add(new Rectangle(tileDim,tileDim,c));
        }
        for(Rectangle r : rects) {
            r.setStroke(Color.BLACK);
            r.setStrokeWidth(1.0);
            r.setOnMouseEntered(event -> {
                int row = rectangles.indexOf(r)/board.DIM;
                int col = rectangles.indexOf(r)%board.DIM;
                tileRow = "" + row;
                tileCol = "" + col;
                tileOwner = board.getTile(row,col).getOwner();
                tileTime = new Date(board.getTile(row,col).getTime()).toString();
                tileColor = board.getTile(row,col).getColor().name();
                updateView("PLACE");
            });
            r.setOnMouseClicked(event -> {
                Long time = System.currentTimeMillis();
                PlaceTile tile = new PlaceTile(rects.indexOf(r)/board.DIM,rects.indexOf(r)%board.DIM,username,placecol,time);
                writeTo(worker.getOutStream(),new PlaceRequest(CHANGE_TILE,tile));
            });
        }
        return rects;
    }

    /**
     * creates the bar of color choices at the bottom of screen
     * the current color is the background of the screen (it's kind of annoying but I thought it would
     * be a cool idea so I'm not changing it now)
     */
    private void createColorBar() {
        for(int i = 0; i < COLORS.length; i++) {
            ToggleButton tb = new ToggleButton(COLORS[i].toString());
            Color c = Color.rgb(COLORS[i].getRed(),COLORS[i].getGreen(),COLORS[i].getBlue());
            tb.setBackground(new Background(new BackgroundFill(c,CornerRadii.EMPTY,Insets.EMPTY)));
            tb.setToggleGroup(colorToggles);
            tb.setPrefHeight(50.0);
            tb.setUserData(COLORS[i]);
            tb.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
            colors.getChildren().add(tb);
        }
        colorToggles.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
            public void changed(ObservableValue<? extends Toggle> ov, Toggle toggle, Toggle new_toggle) {
                if(new_toggle == null) {
                    placecol = WHITE;
                }
                else {
                    placecol = (PlaceColor)new_toggle.getUserData();
                }
                updateView("PLACE");
            }
        });
    }
}
