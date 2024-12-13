package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class Player {
    private String name;

    private Board board;
    private List<Ship> ships;

    public Player(String name) {
        this.name = name;

        this.board = new Board();
        this.ships = new ArrayList<>();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public List<Ship> getShips() {
        return ships;
    }

    public void addShip(Ship ship) {
        ships.add(ship);
    }



    public boolean placeShip(List<Coordinate> coordinates, int size, Player player) {

        if (coordinates.size() != size) {
            System.out.println("Ошибка: Размер корабля не совпадает с количеством координат.");
            return false;
        }


        Ship ship = new Ship(coordinates, size);

        // Проверка ориентации корабля
        if (!ship.checkOrientation()) {
            System.out.println("Ошибка: Ориентация корабля некорректна.");
            return false;  // Если ориентация некорректна, возвращаем false
        }

        // Проверка на близость к другим кораблям на поле текущего игрока
        for (Coordinate coordinate : coordinates) {
            // Проверяем соседние клетки вокруг каждой клетки корабля
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int newX = coordinate.getX() + dx;
                    int newY = coordinate.getY() + dy;

                    // Игнорируем саму клетку корабля
                    if (dx == 0 && dy == 0) continue;

                    // Проверяем, чтобы соседние клетки были в пределах поля и не были заняты
                    if (newX >= 0 && newX < 10 && newY >= 0 && newY < 10) {
                        if (this.getBoard().isCellOccupied1(newX, newY)) {
                            System.out.println("Ошибка: Клетка с координатами (" + newX + ", " + newY + ") занята другим объектом.");
                            return false;
                        }
                    }
                }
            }
        }

        // Проверяем, что все клетки на поле не заняты и находятся в пределах поля
        for (Coordinate coordinate : coordinates) {
            if (coordinate.getX() < 0 || coordinate.getX() >= 10 || coordinate.getY() < 0 || coordinate.getY() >= 10) {
                System.out.println("Ошибка: Корабль выходит за пределы поля на координатах (" + coordinate.getX() + ", " + coordinate.getY() + ").");
                return false;  // Корабль выходит за пределы поля
            }
            if (player.getBoard().isCellOccupied1(coordinate.getX(), coordinate.getY())) {
                System.out.println("Ошибка: Клетка с координатами (" + coordinate.getX() + ", " + coordinate.getY() + ") уже занята.");
                return false;  // Клетка уже занята
            }
        }

        // Проверка, что все клетки корабля соединены между собой
        if (!areCoordinatesConnected(coordinates)) {
            System.out.println("Ошибка: Клетки корабля не соединены.");
            return false;
        }

        // Если все проверки прошли, размещаем корабль
        for (Coordinate coordinate : coordinates) {
            getBoard().setCell(coordinate.getX(), coordinate.getY(), CellState.SHIP);  // Устанавливаем клетку как корабль
        }
        ships.add(ship);
        System.out.println("Добавлен корабль: " + ship);
        System.out.println("Текущее количество кораблей: " + ships.size());
        return true;  // Успешно разместили корабль
    }



    // Метод для проверки, что все клетки корабля соединены
    private boolean areCoordinatesConnected(List<Coordinate> coordinates) {
        Set<Coordinate> visited = new HashSet<>();
        Queue<Coordinate> toVisit = new LinkedList<>();

        // Начинаем с первой клетки
        toVisit.add(coordinates.get(0));
        visited.add(coordinates.get(0));

        while (!toVisit.isEmpty()) {
            Coordinate current = toVisit.poll();

            // Проверяем все соседние клетки
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) continue;  // Игнорируем диагональные клетки

                    int newX = current.getX() + dx;
                    int newY = current.getY() + dy;

                    Coordinate neighbor = new Coordinate(newX, newY);
                    if (coordinates.contains(neighbor) && !visited.contains(neighbor)) {
                        visited.add(neighbor);
                        toVisit.add(neighbor);
                    }
                }
            }
        }

        // Если количество посещенных клеток равно размеру корабля, значит они соединены
        return visited.size() == coordinates.size();
    }





    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Player player = (Player) obj;
        return name != null && name.equals(player.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
