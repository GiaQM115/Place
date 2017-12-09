package place.client;

import place.PlaceBoard;
import place.PlaceTile;

import java.util.Observable;

public class BoardModel extends Observable{

    private static PlaceTile recent = null;

    private PlaceBoard board;

    public BoardModel(PlaceBoard b) {
        board = b;
    }

    public void update(PlaceTile t) {
        board.setTile(t);
    }

    public PlaceBoard getBoard() {
        return board;
    }

    public static void setRecent(PlaceTile t) {
        recent = t;
    }

    public void notifier() {
        if(recent != null) {
            update(recent);
            super.setChanged();
            super.notifyObservers(recent);
        }
    }

}
