package client;

import io.grpc.stub.StreamObserver;
import javafx.application.Platform;
import server.*;
import seabattleservice.SeaBattle.PlayerInfo;
import seabattleservice.SeaBattle.*;

import com.google.protobuf.Empty;

import seabattleservice.SeaBattleServiceGrpc;
import seabattleservice.SeaBattleServiceGrpc.SeaBattleServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SeaBattleController {
    @FXML
    private Button placeShipsButton;
    @FXML
    private Button registerButton;
    @FXML
    private TextField playerNameField;
    @FXML
    private Button attackButton;

    private seabattleservice.SeaBattleServiceGrpc.SeaBattleServiceBlockingStub gameStub;
    private Player player;
    private Player currentPlayer;
    private int currentShipSize = 4; // Начальный размер корабля
    private List<Coordinate> currentShipCoordinates = new ArrayList<>();
    private boolean isPlacingShips = false;
    private boolean isAttacking = false;


    private int remainingOneCellShips = 4;
    private int remainingTwoCellShips = 3;
    private int remainingThreeCellShips = 2;
    private int remainingFourCellShips = 1;

    @FXML
    private GridPane player1Board;
    @FXML
    private GridPane player2Board;

    private Button[][] player1BoardButtons = new Button[10][10];
    private Button[][] player2BoardButtons = new Button[10][10];


    private boolean isFirstPlayer;
    private seabattleservice.SeaBattleServiceGrpc.SeaBattleServiceStub asyncStub;

    // Устанавливаем gRPC Stub
    public void setGameStub(SeaBattleServiceGrpc.SeaBattleServiceBlockingStub gameStub, SeaBattleServiceGrpc.SeaBattleServiceStub asyncStub) {
        this.gameStub = gameStub;
        this.asyncStub = asyncStub;
        System.out.println("BlockingStub set in controller: " + (gameStub != null));
        System.out.println("AsyncStub set in controller: " + (asyncStub != null));
    }
    @FXML
    public void initialize() {
        BoardFirst();
        BoardSecond();
        placeShipsButton.setOnAction(event -> startPlacingShips());
        attackButton.setOnAction(event -> startAttacking());
        System.out.println("Calling subscribeToBoardUpdates");
        subscribeToBoardUpdates();

    }
    private void initAsyncStub() {
        if (asyncStub == null) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                    .usePlaintext()
                    .build();
            asyncStub = SeaBattleServiceGrpc.newStub(channel);
            System.out.println("AsyncStub initialized in initAsyncStub: " + (asyncStub != null));
        }
    }

    public void subscribeToBoardUpdates() {
        initAsyncStub();

        if (asyncStub == null) {
            System.err.println("asyncStub is not initialized");
            return;
        }

        System.out.println("asyncStub before subscription: " + (asyncStub != null));

        Empty request = Empty.newBuilder().build();

        StreamObserver<BoardUpdate> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(BoardUpdate value) {
                Platform.runLater(() -> {
                    if (player1Board == null) {
                        System.err.println("player1Board is not initialized");
                        return;
                    }
                    if (player2Board == null) {
                        System.err.println("player2Board is not initialized");
                        return;
                    }

                    updateBoardFromStream(player1Board, value.getPlayer1Board());
                    updateBoardFromStream(player1Board, value.getPlayer2Board());

                });
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Board update stream completed");
            }
        };

        asyncStub.subscribeToBoardUpdates(request, responseObserver);
    }

    @FXML
    private void registerPlayer() {
        String playerName = playerNameField.getText();

        if (playerName.isEmpty()) {
            showError("Ошибка", "Имя игрока не может быть пустым.");
            return;
        }

        this.player = new Player(playerName);
        PlayerInfo playerInfo = PlayerInfo.newBuilder()
                .setName(player.getName())
                .build();

            Response response = gameStub.registerPlayer(playerInfo);

            if (response.getSuccess()) {
                showInfo("Успех", "Игрок успешно зарегистрирован: " + playerName);
                clearInterface();


                BoolResponse isFirstResponse = gameStub.isFirstPlayer(playerInfo);
                isFirstPlayer = isFirstResponse.getValue();


                PlayerInfo player1 = gameStub.getPlayer1(com.google.protobuf.Empty.newBuilder().build());
//                PlayerInfo player2 = gameStub.getPlayer2(com.google.protobuf.Empty.newBuilder().build());

                if (player1 != null ) {
//                if (player1 != null && player2 != null) {
                    startGame();
                } else {
                    showInfo("Информация", "Не все игроки зарегистрированы.");
                }
            } else {
                showError("Ошибка", "Ошибка регистрации игрока: " + response.getMessage());
            }

    }



    private void startGame() {
        try {
            com.google.protobuf.Empty request = com.google.protobuf.Empty.newBuilder().build();
            Response response = gameStub.startGame(request);

            if (response.getSuccess()) {
                System.out.println("Игра началась.");
            } else {
                System.out.println("Ошибка: " + response.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Ошибка при начале игры: " + e.getMessage());
        }
    }


    private void clearInterface() {
        registerButton.setDisable(true);
        playerNameField.setDisable(true);
    }


    private void BoardFirst() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Button cellButton = new Button();
                cellButton.setMinSize(30, 30);
                final int finalRow = row;
                final int finalCol = col;
                cellButton.setOnAction(event -> onPlaceShipClicked(finalRow, finalCol));
                player1BoardButtons[row][col] = cellButton;
                player1Board.add(cellButton, col, row);
            }
        }
    }


    private void BoardSecond() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                final int finalRow = row;
                final int finalCol = col;
                Button cellButton = new Button();
                cellButton.setMinSize(30, 30);
                cellButton.setOnAction(event -> onAttackClicked(finalRow, finalCol));
                player2BoardButtons[row][col] = cellButton;
                player2Board.add(cellButton, col, row);
            }
        }
    }

    private void startPlacingShips() {
        isPlacingShips = true;
        showInfo("Режим расстановки", "Теперь выберите клетки для размещения кораблей.");
        currentShipCoordinates.clear();
        updateShipSize();
    }


    private void onPlaceShipClicked(int row, int col) {
        if (!isPlacingShips) {
            showError("Ошибка", "Сначала нажмите 'Расставить корабли'.");
            return;
        }

        if (gameStub == null) {
            showError("Ошибка", "Вы не подключены к серверу.");
            return;
        }

        if (player == null) {
            showError("Ошибка", "Игрок не инициализирован.");
            return;
        }

        System.out.println("Текущий игрок: " + player.getName());

        Board playerBoard;
        try {
            playerBoard = getPlayerBoard();
        } catch (Exception e) {
            showError("Ошибка", "Не удалось получить доску игрока: " + e.getMessage());
            return;
        }

        // Создаем запрос для проверки занятости клетки
        seabattleservice.SeaBattle.CellRequest request = seabattleservice.SeaBattle.CellRequest.newBuilder()
                .setRow(row)
                .setCol(col)
                .setPlayer(seabattleservice.SeaBattle.PlayerInfo.newBuilder()
                        .setName(player.getName())
                        .build())
                .build();

        // Отправляем запрос на сервер через gRPC
        try {
            seabattleservice.SeaBattle.BoolResponse response = gameStub.isCellOccupied(request);  // Используем gRPC stub
            boolean isOccupied = response.getValue();

            if (isOccupied) {
                System.out.println("Ошибка: клетка (" + row + ", " + col + ") уже занята!");
                showError("Ошибка", "Эта клетка уже занята. Выберите другую.");
                return;
            }
        } catch (Exception e) {
            System.out.println("Ошибка при проверке клетки: " + e.getMessage());
            showError("Ошибка", "Не удалось проверить клетку.");
            return;
        }

        if (currentShipCoordinates.size() >= currentShipSize) {
            showError("Ошибка", "Невозможно добавить больше клеток для текущего корабля.");
            return;
        }

        addShipCoordinateAndUpdateCell(playerBoard, row, col);

        if (currentShipCoordinates.size() == currentShipSize) {
            confirmShipPlacement(playerBoard);
        }
    }

    private Board getPlayerBoard() {
        // Создаем запрос для получения информации о доске игрока
        seabattleservice.SeaBattle.PlayerInfo playerInfo = seabattleservice.SeaBattle.PlayerInfo.newBuilder()
                .setName(player.getName())
                .build();

        // Выполняем запрос на сервер через gRPC
        seabattleservice.SeaBattle.BoardInfo boardInfo;
        try {
            boardInfo = gameStub.getPlayerBoard(playerInfo);
            return convertBoardInfoToBoard(boardInfo);
        } catch (Exception e) {
            System.out.println("Ошибка при получении доски: " + e.getMessage());
            return null;
        }
    }


    private Board convertBoardInfoToBoard(seabattleservice.SeaBattle.BoardInfo boardInfo) {
        Board board = new Board();

        // Преобразуем каждую клетку из BoardInfo в Board
        for (seabattleservice.SeaBattle.CellInfo cellInfo : boardInfo.getCellsList()) {
            int row = cellInfo.getRow();
            int col = cellInfo.getCol();
            CellState state = convertCellState(cellInfo.getState()); // Преобразуем CellStateInfo в CellState
            board.setCell(row, col, state); // Устанавливаем состояние клетки на доске
        }
        return board;
    }



    private CellState convertCellState(seabattleservice.SeaBattle.CellStateInfo stateInfo) {
        switch (stateInfo) {
            case SHIP:
                return CellState.SHIP;
            case HIT:
                return CellState.HIT;
            case MISS:
                return CellState.MISS;
            case EMPTY:
            default:
                return CellState.EMPTY;
        }
    }





    private void addShipCoordinateAndUpdateCell(Board playerBoard, int row, int col) {
        Coordinate coordinate = new Coordinate(row, col);
        currentShipCoordinates.add(coordinate);
        playerBoard.setCell(row, col, CellState.SHIP);

        // Определение массива кнопок для текущего игрока
        Button[][] playerBoardButtons = player1BoardButtons;
        playerBoardButtons[row][col].setStyle("-fx-background-color: gray;");
    }
    private void confirmShipPlacement(Board playerBoard) {
        // Создаем запрос для размещения корабля
        seabattleservice.SeaBattle.PlaceShipRequest request = seabattleservice.SeaBattle.PlaceShipRequest.newBuilder()
                .addAllCoordinates(currentShipCoordinates.stream()
                        .map(coord -> seabattleservice.SeaBattle.CoordinateInfo.newBuilder()
                                .setX(coord.getX())
                                .setY(coord.getY())
                                .build())
                        .collect(Collectors.toList())) // Добавляем все координаты корабля
                .setSize(currentShipSize)
                .setPlayer(seabattleservice.SeaBattle.PlayerInfo.newBuilder()
                        .setName(player.getName())
                        .build())
                .build();

        // Выполняем запрос через gRPC
        try {
            seabattleservice.SeaBattle.Response response = gameStub.placeShip(request); // Отправляем запрос на сервер
            if (response.getSuccess()) {
                updateShipCells(playerBoard, "-fx-background-color: black;");
                showInfo("Успех", "Корабль размещен.");
                updateRemainingShips();
                updateShipSize();
            } else {
                updateShipCells(playerBoard, "-fx-background-color: lightgray;");
                showError("Ошибка", "Невозможно разместить корабль. Попробуйте снова.");
            }
        } catch (Exception e) {
            showError("Ошибка", "Не удалось разместить корабль.");
            e.printStackTrace();
        }


        currentShipCoordinates.clear();
    }


    private void updateShipCells(Board playerBoard, String color) {
        Button[][] boardButtons = player1BoardButtons;

        for (Coordinate coordinate : currentShipCoordinates) {
            playerBoard.setCell(coordinate.getX(), coordinate.getY(), CellState.SHIP);
            boardButtons[coordinate.getX()][coordinate.getY()].setStyle(color);
        }
    }

    private void updateRemainingShips() {
        switch (currentShipSize) {
            case 1:
                remainingOneCellShips--;
                break;
            case 2:
                remainingTwoCellShips--;
                break;
            case 3:
                remainingThreeCellShips--;
                break;
            case 4:
                remainingFourCellShips--;
                break;
        }
    }

    private void updateShipSize() {
        if (remainingFourCellShips > 0) {
            currentShipSize = 4;
        } else if (remainingThreeCellShips > 0) {
            currentShipSize = 3;
        } else if (remainingTwoCellShips > 0) {
            currentShipSize = 2;
        } else if (remainingOneCellShips > 0) {
            currentShipSize = 1;
        } else {
            showInfo("Готово", "Все корабли расставлены!");
        }
    }


    private void startAttacking() {
        isAttacking = true;
        showInfo("Режим атаки", "Теперь выберите клетки для атаки.");
    }

    private void onAttackClicked(int row, int col) {
        if (!isAttacking) {
            showError("Ошибка", "Сначала нажмите 'Сделать ход' для начала атаки.");
            return;
        }

        if (gameStub == null) {
            showError("Ошибка", "Вы не подключены к серверу.");
            return;
        }

        try {
            // Получаем текущего игрока с сервера через gRPC
            PlayerInfo currentPlayerInfo = gameStub.getCurrentPlayer(Empty.newBuilder().build());
            Player currentPlayer = new Player(currentPlayerInfo.getName());

            if (player.equals(currentPlayer)) {
                System.out.println("Клиент: текущий игрок до хода - " + currentPlayer.getName());

                // Отправляем ход на сервер
                MoveRequest moveRequest = MoveRequest.newBuilder()
                        .setX(row)
                        .setY(col)
                        .build();
                Response moveResponse = gameStub.makeMove(moveRequest);

                if (moveResponse.getSuccess()) {
                    System.out.println("Клиент: текущий игрок после хода - " + currentPlayer.getName());

                    // Получаем доску противника
                    BoardInfo opponentBoardInfo = gameStub.getOpponentBoardForPlayer(PlayerRequest.newBuilder()
                            .setPlayer(currentPlayerInfo)
                            .build());
                    Board opponentBoard = convertBoardInfoToBoard(opponentBoardInfo);
                    updateOpponentBoard(opponentBoard, true);
                    updateBoard(new Player(currentPlayerInfo.getName()));


                    if (isGameOver()) {
                        Player winner = getWinner();
                        if (winner != null) {
                            showInfo("Игра завершена", "Победитель: " + winner.getName());
                            lockGameInterface();
                        } else {
                            showError("Ошибка", "Не удалось определить победителя.");
                        }
                    }
                } else {
                    showError("Ошибка", "Не удалось выполнить ход.");
                }
            } else {

                if (isGameOver()) {
                    Player winner = getWinner();
                    if (winner != null) {
                        showInfo("Игра завершена", "Победитель: " + winner.getName());
                        lockGameInterface();
                    } else {
                        showError("Ошибка", "Не удалось определить победителя.");
                    }
                } else {
                    showInfo("Ошибка", "Не ваша очередь, подождите");
                    System.out.println("Не ваша очередь, подождите");
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось выполнить атаку. Попробуйте снова.");
        }
    }

    protected void updateBoardFromStream(GridPane board, BoardInfo boardInfo) {
        try {
            Button[][] boardButtons = player1BoardButtons;
            System.out.println("BoardInfo cells count-: " + boardInfo.getCellsCount());

            if (boardInfo.getCellsCount() != 100) {
                System.out.println("BoardInfo cells count--: " + boardInfo.getCellsCount());
//                System.err.println("BoardInfo does not contain 100 cells: " + boardInfo.getCellsCount());

                return;
            }

            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {
                    int index = row * 10 + col;
                    System.out.println("Updating cell at index: " + index);

                    CellInfo cellInfo = boardInfo.getCells(index);
                    CellState state = CellState.valueOf(cellInfo.getState().name());
                    updateCellDisplay(row, col, state, boardButtons);
                }
            }
        } catch (Exception e) {
               e.printStackTrace();
        }
    }


    protected void updateBoard(Player player) {
        try {
            // Получаем информацию о доске игрока через gRPC
            PlayerInfo playerInfo = PlayerInfo.newBuilder().setName(player.getName()).build();
            BoardInfo boardInfo = gameStub.getPlayerBoard(playerInfo);

            Button[][] boardButtons = player1BoardButtons;


            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {

                    CellInfo cellInfo = boardInfo.getCells(row * 10 + col); // Преобразуем индексы в одномерный массив
                    CellState state = CellState.valueOf(cellInfo.getState().name()); // Получаем состояние клетки
                    updateCellDisplay(row, col, state, boardButtons);  // Обновляем отображение клетки


                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось обновить доску игрока.");
        }
    }





    private void updateOpponentBoard(Board opponentBoard, boolean isPlayer1) {


        Button[][] boardButtons = isPlayer1 ? player2BoardButtons : player1BoardButtons;
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                CellState state = opponentBoard.getCell(row, col).getState();
                if (state == CellState.SHIP) {

                    updateCellDisplay(row, col, CellState.EMPTY, boardButtons);
                } else {

                    updateCellDisplay(row, col, state, boardButtons);
                }
            }
        }

    }


    private void updateCellDisplay(int row, int col, CellState state, Button[][] boardButtons) {
        Button cellButton = boardButtons[row][col];
        switch (state) {
            case HIT:
                cellButton.setStyle("-fx-background-color: red;");
                break;
            case MISS:
                cellButton.setStyle("-fx-background-color: blue;");
                break;
            case SHIP:
                cellButton.setStyle("-fx-background-color: black;");
                break;
        }
    }

    private boolean isGameOver() {
        try {
            BoolResponse response = gameStub.isGameOver(Empty.newBuilder().build());
            return response.getValue();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось проверить завершение игры.");
            return false;
        }
    }

    private Player getWinner() {
        try {
            PlayerInfo winnerInfo = gameStub.getWinner(Empty.newBuilder().build());
            if (winnerInfo != null) {
                return new Player(winnerInfo.getName());
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка", "Не удалось получить победителя.");
            return null;
        }
    }



    private void lockGameInterface() {
        disableBoard(player1BoardButtons);
        disableBoard(player2BoardButtons);
        registerButton.setDisable(true);
        playerNameField.setDisable(true);
        attackButton.setDisable(true);
        placeShipsButton.setDisable(true);
    }

    private void disableBoard(Button[][] boardButtons) {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                boardButtons[row][col].setDisable(true);
            }
        }
    }
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}