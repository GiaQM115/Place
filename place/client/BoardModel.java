package place.client;

import place.PlaceBoard;
import place.PlaceTile;

import java.util.Observable;

public class BoardModel extends Observable{

    private PlaceBoard board;

    public BoardModel(PlaceBoard b) {
        board = b;
    }

    public void update(PlaceTile tile) {
        board.setTile(tile);
    }

    public PlaceBoard getBoard() {
        return board;
    }

}
