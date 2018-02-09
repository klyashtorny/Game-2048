import java.util.*;

public class Model {
    private static final int FIELD_WIDTH = 4;
    private Tile[][] gameTiles = new Tile[FIELD_WIDTH][FIELD_WIDTH];
    int score = 0;
    int maxTile = 2;
    private boolean isSaveNeeded = true;
    private Stack<Tile[][]> previousStates = new Stack<>();
    private Stack<Integer> previousScores = new Stack<>();

    private void saveState(Tile[][] tiles) {
        Tile[][] saveTile = new Tile[FIELD_WIDTH][FIELD_WIDTH];

        for (int i = 0; i < FIELD_WIDTH; i++) {
            for (int k = 0; k < FIELD_WIDTH; k++) {
                saveTile[i][k] = new Tile(tiles[i][k].value);
            }
        }
            previousStates.push(saveTile);
            previousScores.push(score);
            isSaveNeeded = false;

    }
    public void rollback(){
        if (previousScores.isEmpty() || previousStates.isEmpty()) return;
        gameTiles = previousStates.pop();
        score = previousScores.pop();
    }
    public Model() {
        resetGameTiles();
    }

    public Tile[][] getGameTiles() {
        return gameTiles;
    }
    public boolean canMove(){
        if(!getEmptyTiles().isEmpty()) return true;

        for (int i=0; i<gameTiles.length; i++){
            for (int j=1; j<gameTiles.length; j++){
                if(gameTiles[i][j].value == gameTiles[i][j-1].value) return true;
            }
        }
        for (int i=0; i<gameTiles.length; i++){
            for (int j=1; j<gameTiles.length; j++){
                if(gameTiles[j][i].value==gameTiles[j-1][i].value) return true;
            }
        }
        return false;
    }
    void resetGameTiles() {
        for (int i = 0; i < FIELD_WIDTH; i++) {
            for (int k = 0; k < FIELD_WIDTH; k++) {
                gameTiles[i][k] = new Tile();
            }
        }
        addTile();
        addTile();
    }

    private void addTile() {
        List<Tile> emptyTiles = getEmptyTiles();
        if (emptyTiles.size() == 0) return;
        Tile addTile = emptyTiles.get((int) (emptyTiles.size() * Math.random()));
        addTile.value = Math.random() < 0.9 ? 2 : 4;
    }

    private List<Tile> getEmptyTiles() {
        List<Tile> tiles = new ArrayList<>();
        for (Tile[] tile : gameTiles) {
            for (Tile tileOne : tile) {
                if (tileOne.isEmpty()) tiles.add(tileOne);
            }
        }
        return tiles;
    }

    void left() {
        if(isSaveNeeded) saveState(gameTiles);
        boolean needToAdd = false;
        for (Tile[] tileRow : gameTiles) {
            if (compressTiles(tileRow) | mergeTiles(tileRow)) {
                needToAdd = true;
            }
        }
        if (needToAdd) addTile();
        isSaveNeeded = true;
    }
    void right(){
        saveState(gameTiles);
        rotateMatrixRight();
        rotateMatrixRight();
        left();
        rotateMatrixRight();
        rotateMatrixRight();
    }
    void up(){
        saveState(gameTiles);
        rotateMatrixRight();
        rotateMatrixRight();
        rotateMatrixRight();
        left();
        rotateMatrixRight();
    }
    void down(){
        saveState(gameTiles);
        rotateMatrixRight();
        left();
        rotateMatrixRight();
        rotateMatrixRight();
        rotateMatrixRight();

    }
    void randomMove(){
       int n = ((int) (Math.random() * 100)) % 4;
       switch (n){
           case 0: left(); break;
           case 1: right(); break;
           case 2: up(); break;
           case 3: down(); break;

       }

    }
    public boolean hasBoardChanged(){
        Tile[][] previousTile = previousStates.peek();
        for (int i=0; i<previousTile.length; i++) {
            for (int j = 0; j < previousTile[0].length; j++) {
                if (previousTile[i][j].value != gameTiles[i][j].value) return true;
            }
        }
        return false;
    }
    public MoveEfficiency getMoveEfficiency(Move move){
        move.move();
        if(!hasBoardChanged()) return new MoveEfficiency(-1,0,move);
        MoveEfficiency moveEfficiency = new MoveEfficiency(getEmptyTiles().size(), score, move);
        rollback();
        return moveEfficiency;
    }
    public void autoMove(){
        PriorityQueue<MoveEfficiency> queue = new PriorityQueue<>(4, Collections.reverseOrder());
        queue.add(getMoveEfficiency(this::left));
        queue.add(getMoveEfficiency(this::right));
        queue.add(getMoveEfficiency(this::up));
        queue.add(getMoveEfficiency(this::down));
        queue.peek().getMove().move();
    }

    private boolean compressTiles(Tile[] tiles) { //ряд {0, 0, 0, 4} становится рядом {4, 0, 0, 0}
        boolean compress = false;

        ArrayList<Integer> arr = new ArrayList<>();
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i].value != 0) arr.add(tiles[i].value);
            tiles[i].value = 0;
        }
        for (int x = 0; x < arr.size(); x++) {
            tiles[x].value = arr.get(x);
        }
        if (arr.size() == tiles.length || arr.size() == 0) compress = false;
        else compress = true;

        return compress;
//        boolean compressed = false;
//
//            for (int indexTile = 1; indexTile < tiles.length; indexTile++) {
//            if (tiles[indexTile].value == 0) continue;
//            for (int nextTile = indexTile-1; nextTile >= 0; nextTile--) {
//                if (tiles[nextTile].value!=0) break;
//                    tiles[nextTile].value = tiles[nextTile+1].value;
//                    tiles[nextTile+1].value = 0;
//                compressed = true;
//            }
//        }
//        return compressed;
    }

    private boolean mergeTiles(Tile[] tiles) { //ряд {4, 4, 4, 4} превратится в {8, 8, 0, 0}, а {4, 4, 4, 0} в {8, 4, 0, 0}
        boolean merged = false;
        for (int chosenTileIndex = 0; chosenTileIndex < tiles.length - 1; chosenTileIndex++) {
            Tile chosenTile = tiles[chosenTileIndex];
            if (chosenTile.value == 0) continue;
            Tile sideTile = tiles[chosenTileIndex + 1];
            if (sideTile.value == 0) continue;
            if (chosenTile.value == sideTile.value) {
                chosenTile.value *= 2;
                score += chosenTile.value;
                if (chosenTile.value > maxTile) maxTile = chosenTile.value;
                sideTile.value = 0;
                compressTiles(tiles);
                merged = true;
            }
        }
        return merged;
    }
    public void rotateMatrixRight()
    {
    /* W and H are already swapped */
        int w = gameTiles.length;
        int h = gameTiles[0].length;
        Tile[][] ret = new Tile[h][w];
        for (int i = 0; i < h; ++i) {
            for (int j = 0; j < w; ++j) {
                ret[i][j] = gameTiles[w - j - 1][i];
            }
        }
        gameTiles = ret;
    }

}