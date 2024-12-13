package server;

import java.util.List;
import java.util.ArrayList;

public class Game {
    private Player player1;
    private Player player2;
    private Player currentPlayer;
    private boolean gameOver;

    public Game(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
        this.gameOver = false;
        this.player1.setBoard(new Board());
        this.player2.setBoard(new Board());
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }


    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void switchTurn() {

        currentPlayer = (currentPlayer == player1) ? player2 : player1;
    }

    public boolean isGameOver() {
        return gameOver;
    }


    public void makeMove(int x, int y) {
        if (gameOver) {
            throw new IllegalStateException("Игра завершена.");
        }

        // Получаем поле противника
        Board opponentBoard = (currentPlayer == player1) ? player2.getBoard() : player1.getBoard();
        System.out.println("Игрок " + currentPlayer.getName() + " делает ход по координатам: (" + x + ", " + y + ")");

        // Проверяем, была ли клетка уже атакована
        CellState state = opponentBoard.getCell(x, y).getState();
        System.out.println("Состояние клетки: " + state);

        if (state == CellState.EMPTY) {
            // Промах
            opponentBoard.setCell(x, y, CellState.MISS);
            System.out.println("Промах. Клетка (" + x + ", " + y + ") теперь помечена как MISS.");

            // Переключаем ход только если промах
            switchTurn();
            System.out.println("Теперь ходит игрок: " + currentPlayer.getName());
        } else if (state == CellState.SHIP) {
            // Попадание
            opponentBoard.setCell(x, y, CellState.HIT);
            System.out.println("Попадание! Клетка (" + x + ", " + y + ") теперь помечена как HIT.");

            // Проверяем, уничтожен ли корабль
            if (isShipSunk(opponentBoard, x, y)) {
                System.out.println("Корабль уничтожен!");
                Ship sunkShip = getShipAtCoordinates(opponentBoard, x, y);

                // Получаем окружающие клетки
                List<Coordinate> surroundingCells = getSurroundingCells(sunkShip, opponentBoard);
                for (Coordinate coord : surroundingCells) {
                    opponentBoard.setCell(coord.getX(), coord.getY(), CellState.MISS);
                    System.out.println("Клетка (" + coord.getX() + ", " + coord.getY() + ") помечена как MISS.");
                }
            }

            // Ход не переключается, игрок продолжает ходить
            System.out.println("Игрок " + currentPlayer.getName() + " продолжает ходить.");
        } else {
            // Если клетка уже была атакована (например, уже HIT или MISS)
            System.out.println("Клетка уже была атакована.");
        }

        // Проверка на победу
        if (checkWin(opponentBoard)) {
            gameOver = true;
            System.out.println(currentPlayer.getName() + " победил!");
        }
    }

    private Ship getShipAtCoordinates(Board board, int x, int y) {
        for (Ship ship : (currentPlayer == player1 ? player2.getShips() : player1.getShips())) {
            for (Coordinate coord : ship.getCoordinates()) {
                if (coord.getX() == x && coord.getY() == y) {
                    return ship;
                }
            }
        }
        throw new IllegalStateException("Корабль не найден для координат (" + x + ", " + y + ").");
    }




    private boolean isShipSunk(Board board, int x, int y) {
        Ship targetShip = null;

        // Находим корабль, которому принадлежит клетка
        for (Ship ship : (currentPlayer == player1 ? player2.getShips() : player1.getShips())) {
            for (Coordinate coordinate : ship.getCoordinates()) {
                if (coordinate.getX() == x && coordinate.getY() == y) {
                    targetShip = ship;
                    break;
                }
            }
            if (targetShip != null) break;
        }


        // Проверяем, все ли клетки корабля повреждены
        System.out.println("Корабль найден: " + targetShip);
        for (Coordinate coord : targetShip.getCoordinates()) {
            CellState cellState = board.getCell(coord.getX(), coord.getY()).getState();
            System.out.println("Проверяем состояние клетки (" + coord.getX() + ", " + coord.getY() + "): " + cellState);
            if (cellState != CellState.HIT) {
                System.out.println("Корабль еще не уничтожен.");
                return false; // Если хотя бы одна клетка не повреждена
            }
        }


        System.out.println("Корабль уничтожен!");



        return true;
    }


    private List<Coordinate> getSurroundingCells(Ship ship, Board board) {
        List<Coordinate> surroundingCells = new ArrayList<>();
        for (Coordinate coord : ship.getCoordinates()) {
            // Проверяем 8 соседних клеток
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = coord.getX() + dx;
                    int ny = coord.getY() + dy;

                    // Пропускаем саму клетку корабля
                    if (dx == 0 && dy == 0) continue;

                    // Убедимся, что координаты в пределах доски
                    if (nx >= 0 && ny >= 0 && nx < 10 && ny < 10) {
                        Cell cell = board.getCell(nx, ny);
                        // Добавляем только пустые клетки, которые не принадлежат кораблю
                        if (cell.getState() != CellState.SHIP && cell.getState() != CellState.HIT) {
                            surroundingCells.add(new Coordinate(nx, ny));
                        }
                    }
                }
            }
        }
        return surroundingCells;
    }




    private boolean checkWin(Board opponentBoard) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                CellState state = opponentBoard.getCell(i, j).getState();
                if (state == CellState.SHIP) {
                    System.out.println("Корабль найден в клетке (" + i + ", " + j + ")");
                    return false;
                }
            }
        }
        System.out.println("Все корабли уничтожены.");
        return true;
    }
}
