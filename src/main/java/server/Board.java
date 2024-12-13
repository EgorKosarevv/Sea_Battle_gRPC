package server;

public class Board {
    private Cell[][] cells;

    public Board() {
        cells = new Cell[10][10];
        initializeBoard();
    }


    private void initializeBoard() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                cells[i][j] = new Cell(CellState.EMPTY);
            }
        }
    }


    public Cell getCell(int x, int y) {
        return cells[x][y];
    }


    public void setCell(int x, int y, CellState state) {
        cells[x][y].setState(state);
    }


    public boolean isCellOccupied1(int x, int y) {
        return cells[x][y].getState() != CellState.EMPTY;
    }

}
