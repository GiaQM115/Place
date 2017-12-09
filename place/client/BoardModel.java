package place.client;

import place.PlaceBoard;
import place.PlaceTile;

import java.util.Observable;

public class BoardModel extends Observable{

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

    public void notifier(PlaceTile changed) {
        super.setChanged();
        super.notifyObservers(changed);
    }

}
