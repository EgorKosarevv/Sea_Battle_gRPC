package server;

import java.util.List;

public class Ship {
    private List<Coordinate> coordinates;
    private int size;

    public Ship(List<Coordinate> coordinates, int size) {
        this.coordinates = coordinates;
        this.size = size;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public int getSize() {
        return size;
    }
    // Метод для проверки ориентации корабля (горизонтальная или вертикальная)
    public boolean checkOrientation() {
        if (coordinates.size() < 2) {
            return true;
        }

        boolean isVertical = coordinates.get(0).getX() == coordinates.get(1).getX();
        boolean isHorizontal = coordinates.get(0).getY() == coordinates.get(1).getY();

        for (int i = 1; i < coordinates.size(); i++) {
            if (isVertical && coordinates.get(i).getX() != coordinates.get(0).getX()) {
                return false;
            }
            if (isHorizontal && coordinates.get(i).getY() != coordinates.get(0).getY()) {
                return false;
            }
        }
        return true;
    }
}
