package gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import server.Server;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class JavaFxServer extends Application implements Server.ServerListener {

    private final Server server = new Server();
    private boolean running = false;

    // ---- Composants UI ----
    private Button  startStopBtn;
    private Label   statusDot;
    private Label   statusText;
    private ListView<String> clientListView;
    private TextArea logArea;

    private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================================
    //  Démarrage JavaFX
    // =========================================================================
    @Override
    public void start(Stage stage) {
        server.setListener(this);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e2e;");
        root.setTop(buildTopBar());
        root.setLeft(buildClientPanel());
        root.setCenter(buildLogPanel());

        stage.setScene(new Scene(root, 900, 560));
        stage.setTitle("Remote Terminal — Serveur");
        stage.setOnCloseRequest(e -> { if (running) server.stop(); });
        stage.show();
    }

    // =========================================================================
    //  BARRE SUPÉRIEURE
    // =========================================================================
    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.setStyle("-fx-background-color: #181825; -fx-border-color: #313244; -fx-border-width: 0 0 1 0;");

        Label title = new Label("Remote Terminal — Serveur");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 15));
        title.setStyle("-fx-text-fill: #cdd6f4;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Indicateur de statut
        statusDot  = new Label("●");
        statusText = new Label("Arrêté");
        statusDot .setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 11;");
        statusText.setStyle("-fx-text-fill: #f38ba8; -fx-font-family: Monospaced; -fx-font-size: 12;");

        // Bouton démarrer / arrêter
        startStopBtn = new Button("  Démarrer  ");
        startStopBtn.setStyle(btnStyle("#a6e3a1"));
        startStopBtn.setOnAction(e -> toggleServer());

        bar.getChildren().addAll(title, spacer, statusDot, statusText, startStopBtn);
        return bar;
    }

    // =========================================================================
    //  PANNEAU GAUCHE — clients connectés
    // =========================================================================
    private VBox buildClientPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(230);
        panel.setStyle("-fx-background-color: #181825; -fx-border-color: #313244; -fx-border-width: 0 1 0 0;");

        Label header = new Label("Clients connectés");
        header.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        header.setStyle("-fx-text-fill: #89b4fa;");

        clientListView = new ListView<>();
        clientListView.setStyle(
                "-fx-background-color: #1e1e2e; -fx-border-color: #313244; " +
                "-fx-font-family: Monospaced; -fx-font-size: 12;");

        Label empty = new Label("Aucun client connecté");
        empty.setStyle("-fx-text-fill: #6c7086; -fx-font-family: Monospaced; -fx-font-size: 11;");
        clientListView.setPlaceholder(empty);
        VBox.setVgrow(clientListView, Priority.ALWAYS);

        panel.getChildren().addAll(header, new Separator(), clientListView);
        return panel;
    }

    // =========================================================================
    //  PANNEAU CENTRAL — journal
    // =========================================================================
    private VBox buildLogPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #1e1e2e;");

        Label header = new Label("Journal des commandes");
        header.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        header.setStyle("-fx-text-fill: #89b4fa;");

        Button clearBtn = new Button("Effacer");
        clearBtn.setStyle(
                "-fx-background-color: #313244; -fx-text-fill: #cdd6f4; " +
                "-fx-font-family: Monospaced; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 3 10;");
        clearBtn.setOnAction(e -> logArea.clear());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox logHeader = new HBox(10, header, spacer, clearBtn);
        logHeader.setAlignment(Pos.CENTER_LEFT);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setFont(Font.font("Monospaced", 12));
        logArea.setStyle(
                "-fx-control-inner-background: #181825; -fx-text-fill: #cdd6f4; " +
                "-fx-highlight-fill: #45475a; -fx-focus-color: transparent;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        panel.getChildren().addAll(logHeader, new Separator(), logArea);
        return panel;
    }

    // =========================================================================
    //  Contrôle du serveur
    // =========================================================================
    private void toggleServer() {
        if (!running) {
            try {
                server.start();
                running = true;
                startStopBtn.setText("  Arrêter  ");
                startStopBtn.setStyle(btnStyle("#f38ba8"));
                statusDot .setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 11;");
                statusText.setStyle("-fx-text-fill: #a6e3a1; -fx-font-family: Monospaced; -fx-font-size: 12;");
                statusText.setText("En cours");
            } catch (IOException e) {
                appendLog("ERREUR", "Impossible de démarrer : " + e.getMessage());
            }
        } else {
            server.stop();
            running = false;
            startStopBtn.setText("  Démarrer  ");
            startStopBtn.setStyle(btnStyle("#a6e3a1"));
            statusDot .setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 11;");
            statusText.setStyle("-fx-text-fill: #f38ba8; -fx-font-family: Monospaced; -fx-font-size: 12;");
            statusText.setText("Arrêté");
            clientListView.getItems().clear();
        }
    }

    // =========================================================================
    //  Server.ServerListener
    // =========================================================================
    @Override
    public void onClientConnected(String clientId, String ip) {
        Platform.runLater(() -> {
            clientListView.getItems().add(clientId);
            appendLog("+", "Nouveau client : " + clientId);
        });
    }

    @Override
    public void onClientDisconnected(String clientId) {
        Platform.runLater(() -> {
            clientListView.getItems().remove(clientId);
            appendLog("-", "Client déconnecté : " + clientId);
        });
    }

    @Override
    public void onCommandReceived(String clientId, String command) {
        Platform.runLater(() -> appendLog("CMD", clientId + "  →  " + command));
    }

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> appendLog("SYS", message));
    }

    // =========================================================================
    //  Helpers
    // =========================================================================
    private void appendLog(String tag, String message) {
        String time = LocalTime.now().format(HH_MM_SS);
        logArea.appendText("[" + time + "] [" + tag + "] " + message + "\n");
    }

    private String btnStyle(String color) {
        return "-fx-background-color: " + color + "; -fx-text-fill: #1e1e2e; " +
               "-fx-font-family: Monospaced; -fx-font-weight: bold; -fx-font-size: 12; " +
               "-fx-cursor: hand; -fx-padding: 6 18;";
    }

    // =========================================================================
    //  Entry point
    // =========================================================================
    public static void main(String[] args) {
        launch(args);
    }
}
