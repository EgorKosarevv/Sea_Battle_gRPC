package server;

import io.grpc.Status;
import seabattleservice.SeaBattle;
import seabattleservice.SeaBattle.PlayerInfo;
import seabattleservice.SeaBattle.BoardInfo;
import seabattleservice.SeaBattle.Response;

import seabattleservice.SeaBattleServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SeaBattleServer extends SeaBattleServiceGrpc.SeaBattleServiceImplBase {

    private Game game;
    private Player player1;
    private Player player2;
    private final List<StreamObserver<SeaBattle.BoardUpdate>> boardUpdateObservers = new ArrayList<>();


    @Override
    public void registerPlayer(seabattleservice.SeaBattle.PlayerInfo request,
                               io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.Response> responseObserver) {

        String playerName = request.getName();
        boolean success;
        String message;
        if (player1 == null) {
            player1 = new Player(playerName);
            player1.getName();
            player1.getBoard();
            success = true;
            message = "Client registered successfully: " + playerName;
            System.out.println("Игрок 1 зарегистрирован: " + player1.getName());
        } else if (player2 == null) {
            player2 = new Player(playerName);
            success = true;
            message = "Client registered successfully: " + playerName;
            System.out.println("Игрок 2 зарегистрирован: " + player2.getName());
        } else {
            success = false;
            message = "There are already 2 clients in the game: ";
            System.out.println("Все игроки уже зарегистрированы.");
        }
        Response response = Response.newBuilder().setSuccess(success).setMessage(message).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPlayer1(com.google.protobuf.Empty request,
                           io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.PlayerInfo> responseObserver) {
        try {
            if (player1 != null) {
                PlayerInfo playerInfo = PlayerInfo.newBuilder()
                        .setName(player1.getName())
                        .build();
                responseObserver.onNext(playerInfo);
            } else {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Игрок 1 не зарегистрирован.").asRuntimeException());
            }
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Внутренняя ошибка сервера: " + e.getMessage()).asRuntimeException());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getPlayer2(com.google.protobuf.Empty request,
                           io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.PlayerInfo> responseObserver) {
        try {
            if (player2 != null) {
                PlayerInfo playerInfo = PlayerInfo.newBuilder()
                        .setName(player2.getName())
                        .build();
                responseObserver.onNext(playerInfo);
            } else {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Игрок 2 не зарегистрирован.").asRuntimeException());
            }
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Внутренняя ошибка сервера: " + e.getMessage()).asRuntimeException());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void startGame(com.google.protobuf.Empty request,
                          io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.Response> responseObserver) {
        boolean success = true;
        String message = "";

        if (player1 == null || player2 == null) {
//            success = false;
//            message = "Need to register two players first.";
        } else {
            game = new Game(player1, player2);
            message = "Game started.";
            System.out.println("Игра началась!");
        }

        Response response = Response.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void makeMove(seabattleservice.SeaBattle.MoveRequest request,
                         io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.Response> responseObserver) {
        int x = request.getX();
        int y = request.getY();


        if (game.isGameOver()) {
            responseObserver.onError(new Exception("Игра завершена."));
            return;
        }

        System.out.println("Сервер: первый игрок - " + game.getPlayer1());
        System.out.println("Сервер: второй игрок - " + game.getPlayer2());
        System.out.println("Сервер: текущий игрок до хода - " + game.getCurrentPlayer());


        game.makeMove(x, y);

        Response response = Response.newBuilder()
                .setSuccess(true)
                .setMessage("Ход выполнен.")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        sendBoardUpdates();
    }

    @Override
    public void getOpponentBoardForPlayer(seabattleservice.SeaBattle.PlayerRequest request,
                                          io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.BoardInfo> responseObserver) {
        try {
            // Получаем игрока, отправившего запрос
            seabattleservice.SeaBattle.PlayerInfo requestingPlayerInfo = request.getPlayer();
            String requestingPlayerName = requestingPlayerInfo.getName();

            System.out.println("Вызов getOpponentBoardForPlayer для игрока: " + requestingPlayerName);

            // Определяем доску противника
            Board opponentBoard;
            if (requestingPlayerName.equals(game.getPlayer1().getName())) {
//                System.out.println("Возвращаем доску игрока 2 (" + game.getPlayer2().getName() + ")");
                opponentBoard = game.getPlayer2().getBoard(); // Для игрока 1 возвращаем доску игрока 2
            } else if (requestingPlayerName.equals(game.getPlayer2().getName())) {
//                System.out.println("Возвращаем доску игрока 1 (" + game.getPlayer1().getName() + ")");
                opponentBoard = game.getPlayer1().getBoard(); // Для игрока 2 возвращаем доску игрока 1
            } else {
//                System.out.println("Ошибка: запрашивающий игрок не найден в текущей игре");
                throw new IllegalArgumentException("Запрашивающий игрок не найден в текущей игре");
            }



            // Преобразуем доску в BoardInfo
            seabattleservice.SeaBattle.BoardInfo boardInfo = convertBoardToBoardInfo(opponentBoard);


            responseObserver.onNext(boardInfo);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Ошибка при получении доски противника: " + e.getMessage())
                    .asRuntimeException());
        }
    }



    @Override
    public void placeShip(seabattleservice.SeaBattle.PlaceShipRequest request,
                          io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.Response> responseObserver) {
        try {
            // Извлечение координат из запроса
            List<Coordinate> coordinates = request.getCoordinatesList().stream()
                    .map(coord -> new Coordinate(coord.getX(), coord.getY()))
                    .toList();

            int size = request.getSize();


            String playerName = request.getPlayer().getName();

            Player player;
            if (game == null) {
                throw new IllegalStateException("Game object is not initialized.");
            }

            if (playerName.equals(game.getPlayer1().getName())) {
                player = game.getPlayer1();
            } else if (playerName.equals(game.getPlayer2().getName())) {
                player = game.getPlayer2();
            } else {
                throw new IllegalArgumentException("Игрок не найден в текущей игре.");
            }

            // размещения корабля
            boolean success = player.placeShip(coordinates, size, player);

            // Формирование ответа
            seabattleservice.SeaBattle.Response response = seabattleservice.SeaBattle.Response.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ? "Корабль успешно размещен." : "Не удалось разместить корабль.")
                    .build();


            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Формирование и отправка ответа с ошибкой
            seabattleservice.SeaBattle.Response response = seabattleservice.SeaBattle.Response.newBuilder()
                    .setSuccess(false)
                    .setMessage("Ошибка: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {

            seabattleservice.SeaBattle.Response response = seabattleservice.SeaBattle.Response.newBuilder()
                    .setSuccess(false)
                    .setMessage("Произошла непредвиденная ошибка: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }


    @Override
    public void isGameOver(com.google.protobuf.Empty request,
                           io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.BoolResponse> responseObserver) {

        boolean gameOver = game.isGameOver();

        seabattleservice.SeaBattle.BoolResponse response = seabattleservice.SeaBattle.BoolResponse.newBuilder()
                .setValue(gameOver)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void getWinner(com.google.protobuf.Empty request,
                          io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.PlayerInfo> responseObserver) {

        if (game.isGameOver()) {
            Player winner = game.getCurrentPlayer();

            seabattleservice.SeaBattle.PlayerInfo winnerInfo = seabattleservice.SeaBattle.PlayerInfo.newBuilder()
                    .setName(winner.getName())
                    .build();

            responseObserver.onNext(winnerInfo);
        } else {
            responseObserver.onError(new Exception("Игра не завершена, победитель не определен."));
        }

        responseObserver.onCompleted();
    }


    @Override
    public void isFirstPlayer(seabattleservice.SeaBattle.PlayerInfo request,
                              io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.BoolResponse> responseObserver) {

        if (game == null) {
            seabattleservice.SeaBattle.BoolResponse response = seabattleservice.SeaBattle.BoolResponse.newBuilder()
                    .setValue(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        String playerName = request.getName();

        boolean isFirstPlayer = false;

        if (game.getPlayer1() != null && game.getPlayer1().getName().equals(playerName)) {
            isFirstPlayer = true;
        }


        seabattleservice.SeaBattle.BoolResponse response = seabattleservice.SeaBattle.BoolResponse.newBuilder()
                .setValue(isFirstPlayer)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void getPlayerBoard(seabattleservice.SeaBattle.PlayerInfo request,
                               io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.BoardInfo> responseObserver) {


        String playerName = request.getName();

        System.out.println("Запрос на получение доски для игрока: " + playerName);

        if (player1 == null || player2 == null) {
            System.out.println("Игроки не инициализированы. player1: " + player1 + ", player2: " + player2);
        }

        // Сравниваем игрока с player1 и player2
        if (player1 != null && player1.getName().equals(playerName)) {
            System.out.println("Возвращаем доску для первого игрока: " + player1.getName());
            seabattleservice.SeaBattle.BoardInfo boardInfo = convertBoardToBoardInfo(player1.getBoard());
            responseObserver.onNext(boardInfo);
        } else if (player2 != null && player2.getName().equals(playerName)) {
            System.out.println("Возвращаем доску для второго игрока: " + player2.getName());
            seabattleservice.SeaBattle.BoardInfo boardInfo = convertBoardToBoardInfo(player2.getBoard());
            responseObserver.onNext(boardInfo);
        } else {
            System.out.println("Ошибка: Игрок не найден.");
            responseObserver.onError(new Exception("Игрок не найден"));
        }


        responseObserver.onCompleted();

    }




    @Override
    public void isCellOccupied(seabattleservice.SeaBattle.CellRequest request,
                               io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.BoolResponse> responseObserver) {

        // Получаем информацию о клетке и игроке из запроса
        int row = request.getRow();
        int col = request.getCol();
        String playerName = request.getPlayer().getName();

        System.out.println("Проверка клетки: (" + row + ", " + col + ") для игрока: " + playerName);

        // Сравниваем имя игрока с player1 и player2, чтобы получить его доску
        Player player = null;
        if (player1 != null && player1.getName().equals(playerName)) {
            player = player1;
        } else if (player2 != null && player2.getName().equals(playerName)) {
            player = player2;
        }

        if (player == null) {
            System.out.println("Ошибка: Игрок с таким именем не найден.");
            responseObserver.onError(new Exception("Игрок с таким именем не найден"));
            return;
        }

        // Получаем доску игрока
        Board playerBoard = player.getBoard();

        // Получаем состояние клетки на доске
        Cell cell = playerBoard.getCell(row, col);
        boolean isOccupied = cell.getState() != CellState.EMPTY;  // Сравниваем состояние клетки с EMPTY
        System.out.println("Клетка (" + row + ", " + col + ") для игрока " + playerName + " занята: " + isOccupied);

        // Создаем ответ и отправляем его через responseObserver
        seabattleservice.SeaBattle.BoolResponse response = seabattleservice.SeaBattle.BoolResponse.newBuilder()
                .setValue(isOccupied)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void subscribeToBoardUpdates(com.google.protobuf.Empty request,
                                        StreamObserver<SeaBattle.BoardUpdate> responseObserver) {
        boardUpdateObservers.add(responseObserver);
        System.out.println("New observer subscribed: " + responseObserver);
    }


    private void sendBoardUpdates() {
        SeaBattle.BoardUpdate boardUpdate;
        SeaBattle.BoardUpdate boardUpdate1;
        boardUpdate = SeaBattle.BoardUpdate.newBuilder()
                .setPlayer1Board(convertBoardToBoardInfo(player1.getBoard()))
                .build();
        System.out.println("Sending update to observer 0, observers size: " + boardUpdateObservers.size());
        sendUpdateToObserver(0, boardUpdate);
        boardUpdate1 = SeaBattle.BoardUpdate.newBuilder()
                .setPlayer2Board(convertBoardToBoardInfo(player2.getBoard()))
                .build();
        System.out.println("Sending update to observer 0, observers size: " + boardUpdateObservers.size());
        sendUpdateToObserver(1, boardUpdate1);
    }

    private void sendUpdateToObserver(int index, SeaBattle.BoardUpdate boardUpdate) {
        if (index >= 0 && index < boardUpdateObservers.size()) {
            StreamObserver<SeaBattle.BoardUpdate> observer = boardUpdateObservers.get(index);
            observer.onNext(boardUpdate);
        } else {
            System.err.println("Observer index out of bounds: " + index + ", observers size: " + boardUpdateObservers.size());
        }
    }

    @Override
    public void getCurrentPlayer(com.google.protobuf.Empty request,
                                 io.grpc.stub.StreamObserver<seabattleservice.SeaBattle.PlayerInfo> responseObserver) {
        try {
            Player currentPlayer = game.getCurrentPlayer();

            // Строим объект PlayerInfo на основе текущего игрока
            seabattleservice.SeaBattle.PlayerInfo playerInfo = seabattleservice.SeaBattle.PlayerInfo.newBuilder()
                    .setName(currentPlayer.getName())
                    .build();


            responseObserver.onNext(playerInfo);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    private seabattleservice.SeaBattle.BoardInfo convertBoardToBoardInfo(Board board) {
        seabattleservice.SeaBattle.BoardInfo.Builder boardInfoBuilder = seabattleservice.SeaBattle.BoardInfo.newBuilder();


        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                // Получаем объект Cell через getCell(row, col)
                Cell cell = board.getCell(row, col);

                // Получаем состояние ячейки через cell.getState()
                CellState cellState = cell.getState();

                // Преобразуем CellState в CellStateInfo
                seabattleservice.SeaBattle.CellStateInfo cellStateInfo = convertCellState(cellState);

                // Создаем объект CellInfo
                seabattleservice.SeaBattle.CellInfo cellInfo = seabattleservice.SeaBattle.CellInfo.newBuilder()
                        .setRow(row)
                        .setCol(col)
                        .setState(cellStateInfo)
                        .build();

                // Добавляем ячейку в BoardInfo
                boardInfoBuilder.addCells(cellInfo);
            }
        }

        return boardInfoBuilder.build();
    }



    private seabattleservice.SeaBattle.CellStateInfo convertCellState(CellState state) {
        switch (state) {
            case EMPTY:
                return seabattleservice.SeaBattle.CellStateInfo.EMPTY;
            case SHIP:
                return seabattleservice.SeaBattle.CellStateInfo.SHIP;
            case HIT:
                return seabattleservice.SeaBattle.CellStateInfo.HIT;
            case MISS:
                return seabattleservice.SeaBattle.CellStateInfo.MISS;
            default:
                throw new IllegalArgumentException("Неизвестное состояние ячейки: " + state);
        }
    }



    public static void main(String[] args) throws Exception {
        SeaBattleServer seabattleServer = new SeaBattleServer();
        Server server = ServerBuilder.forPort(8080).addService(seabattleServer).build();

        server.start();
        System.out.println("Server started...");
        server.awaitTermination();
    }
}
