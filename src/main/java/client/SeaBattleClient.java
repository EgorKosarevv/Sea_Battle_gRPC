package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import seabattleservice.SeaBattleServiceGrpc;

public class SeaBattleClient extends Application {

    private SeaBattleServiceGrpc.SeaBattleServiceBlockingStub gameStub; // gRPC Stub для общения с сервером
    private SeaBattleServiceGrpc.SeaBattleServiceStub asyncStub; // Асинхронный gRPC Stub для общения с сервером
    private SeaBattleController controller; // Ссылка на контроллер

    @Override
    public void start(Stage primaryStage) {
        try {
            // Создаем канал для подключения к серверу gRPC
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                    .usePlaintext() // Без шифрования для теста
                    .build();
            // Создаем Stub для общения с сервером
            gameStub = SeaBattleServiceGrpc.newBlockingStub(channel);
            asyncStub = SeaBattleServiceGrpc.newStub(channel);
            System.out.println("BlockingStub initialized: " + (gameStub != null));
            System.out.println("AsyncStub initialized: " + (asyncStub != null));

            // Загружаем FXML файл для интерфейса игры
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/game.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            controller.setGameStub(gameStub, asyncStub);


            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Морской бой");
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
