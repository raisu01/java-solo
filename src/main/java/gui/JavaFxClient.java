package gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import common.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class JavaFxClient extends Application {

    // -------------------------------------------------------------------------
    //  Réseau
    // -------------------------------------------------------------------------
    private Socket socket;
    private PrintWriter writer;
    private InputStreamReader reader;

    // -------------------------------------------------------------------------
    //  Composants UI — terminal
    // -------------------------------------------------------------------------
    private Stage primaryStage;
    private TextArea consoleArea;
    private TextField inputField;

    // -------------------------------------------------------------------------
    //  Historique des commandes (flèches ↑ ↓)
    // -------------------------------------------------------------------------
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    // =========================================================================
    //  Démarrage JavaFX
    // =========================================================================
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Remote Terminal");
        stage.setOnCloseRequest(e -> disconnect());
        showConnectScene();
        stage.show();
    }

    // =========================================================================
    //  ÉCRAN 1 — CONNEXION
    // =========================================================================
    private void showConnectScene() {
        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50, 60, 50, 60));
        root.setStyle("-fx-background-color: #1e1e2e;");

        // Titre
        Label title = new Label("Remote Terminal");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #cdd6f4;");

        Label subtitle = new Label("Connexion au serveur distant");
        subtitle.setFont(Font.font("Monospaced", 12));
        subtitle.setStyle("-fx-text-fill: #6c7086;");

        // Champ IP
        Label ipLabel = fieldLabel("Adresse IP du serveur");
        TextField ipField = styledInput("localhost", 320);

        // Champ Port
        Label portLabel = fieldLabel("Port");
        TextField portField = styledInput(String.valueOf(Protocol.DEFAULT_PORT), 320);

        // Message de statut
        Label statusLabel = new Label(" ");
        statusLabel.setStyle("-fx-text-fill: #f38ba8; -fx-font-family: Monospaced; -fx-font-size: 11;");
        statusLabel.setMinHeight(16);

        // Bouton Connexion
        Button connectBtn = new Button("  Connexion  ");
        styleConnectButton(connectBtn);

        connectBtn.setOnAction(e -> attemptConnect(
                ipField.getText().trim(),
                portField.getText().trim(),
                statusLabel, connectBtn));

        // Entrée ↵ sur les champs
        ipField.setOnAction(e -> connectBtn.fire());
        portField.setOnAction(e -> connectBtn.fire());

        root.getChildren().addAll(
                title, subtitle,
                new Separator(),
                ipLabel, ipField,
                portLabel, portField,
                connectBtn, statusLabel);

        primaryStage.setScene(new Scene(root, 440, 370));
    }

    private void attemptConnect(String ip, String portStr, Label status, Button btn) {
        if (ip.isEmpty()) { setError(status, "Veuillez saisir une adresse IP."); return; }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            setError(status, "Port invalide.");
            return;
        }

        status.setStyle("-fx-text-fill: #a6e3a1; -fx-font-family: Monospaced; -fx-font-size: 11;");
        status.setText("Connexion en cours...");
        btn.setDisable(true);

        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Protocol.ENCODING), true);
                reader = new InputStreamReader(socket.getInputStream(), Protocol.ENCODING);
                Platform.runLater(() -> showTerminalScene(ip, port));
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    setError(status, "Impossible de se connecter : " + ex.getMessage());
                    btn.setDisable(false);
                });
            }
        }).start();
    }

    // =========================================================================
    //  ÉCRAN 2 — TERMINAL
    // =========================================================================
    private void showTerminalScene(String ip, int port) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e2e;");

        root.setTop(buildTopBar(ip, port));
        root.setCenter(buildConsole());
        root.setBottom(buildInputBar());

        startReceiving();
        appendConsole("--- Connecté à " + ip + ":" + port + " ---\n");
        inputField.requestFocus();

        primaryStage.setScene(new Scene(root, 860, 540));
        primaryStage.setTitle("Remote Terminal — " + ip);
    }

    // ---- Barre supérieure ----
    private HBox buildTopBar(String ip, int port) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 14, 8, 14));
        bar.setStyle("-fx-background-color: #181825; -fx-border-color: #313244; -fx-border-width: 0 0 1 0;");

        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 10;");

        Label info = new Label("Connecté à " + ip + ":" + port);
        info.setStyle("-fx-text-fill: #cdd6f4; -fx-font-family: Monospaced; -fx-font-size: 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button disconnectBtn = new Button("Déconnecter");
        disconnectBtn.setStyle(
                "-fx-background-color: #313244; -fx-text-fill: #f38ba8; " +
                "-fx-font-family: Monospaced; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 4 12;");
        disconnectBtn.setOnAction(e -> { disconnect(); showConnectScene(); });

        bar.getChildren().addAll(dot, info, spacer, disconnectBtn);
        return bar;
    }

    // ---- Zone console ----
    private ScrollPane buildConsole() {
        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setWrapText(true);
        consoleArea.setFont(Font.font("Monospaced", 13));
        consoleArea.setStyle(
                "-fx-control-inner-background: #1e1e2e; -fx-text-fill: #cdd6f4; " +
                "-fx-highlight-fill: #45475a; -fx-focus-color: transparent;");

        ScrollPane scroll = new ScrollPane(consoleArea);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background: #1e1e2e; -fx-background-color: #1e1e2e;");
        return scroll;
    }

    // ---- Barre de saisie ----
    private HBox buildInputBar() {
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(8, 14, 8, 14));
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color: #181825; -fx-border-color: #313244; -fx-border-width: 1 0 0 0;");

        Label prompt = new Label("›");
        prompt.setStyle("-fx-text-fill: #89b4fa; -fx-font-family: Monospaced; -fx-font-weight: bold; -fx-font-size: 16;");

        inputField = new TextField();
        inputField.setPromptText("Tapez une commande...");
        inputField.setStyle(
                "-fx-background-color: #313244; -fx-text-fill: #cdd6f4; " +
                "-fx-font-family: Monospaced; -fx-font-size: 13; -fx-border-color: transparent;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("Envoyer");
        sendBtn.setStyle(
                "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; " +
                "-fx-font-family: Monospaced; -fx-font-weight: bold; -fx-font-size: 12; " +
                "-fx-cursor: hand; -fx-padding: 6 16;");

        sendBtn.setOnAction(e -> sendCommand());
        inputField.setOnAction(e -> sendCommand());
        bindHistoryNavigation();

        bar.getChildren().addAll(prompt, inputField, sendBtn);
        return bar;
    }

    // ---- Navigation historique ↑ ↓ ----
    private void bindHistoryNavigation() {
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP) {
                if (history.isEmpty()) return;
                if (historyIndex == -1) historyIndex = history.size() - 1;
                else if (historyIndex > 0) historyIndex--;
                inputField.setText(history.get(historyIndex));
                inputField.end();
            } else if (e.getCode() == KeyCode.DOWN) {
                if (historyIndex == -1) return;
                historyIndex++;
                if (historyIndex >= history.size()) { historyIndex = -1; inputField.clear(); }
                else { inputField.setText(history.get(historyIndex)); inputField.end(); }
            }
        });
    }

    // =========================================================================
    //  Réseau
    // =========================================================================
    private void sendCommand() {
        String cmd = inputField.getText().trim();
        if (writer == null || cmd.isEmpty()) return;

        history.add(cmd);
        historyIndex = -1;

        appendConsole("\n› " + cmd + "\n");
        writer.println(cmd);
        inputField.clear();

        if ("EXIT".equalsIgnoreCase(cmd)) { disconnect(); showConnectScene(); }
    }

    private void startReceiving() {
        new Thread(() -> {
            try {
                int c;
                while (reader != null && (c = reader.read()) != -1) {
                    final char ch = (char) c;
                    Platform.runLater(() -> appendConsole(String.valueOf(ch)));
                }
            } catch (IOException e) {
                Platform.runLater(() -> appendConsole("\n[Déconnecté du serveur]\n"));
            }
        }).start();
    }

    private void disconnect() {
        try {
            if (writer != null) { writer.println("EXIT"); writer.close(); writer = null; }
            if (reader != null) { reader.close(); reader = null; }
            if (socket != null && !socket.isClosed()) { socket.close(); socket = null; }
        } catch (IOException e) { /* ignore */ }
    }

    // =========================================================================
    //  Helpers UI
    // =========================================================================
    private void appendConsole(String text) {
        if (consoleArea != null) consoleArea.appendText(text);
    }

    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #a6adc8; -fx-font-family: Monospaced; -fx-font-size: 12;");
        return lbl;
    }

    private TextField styledInput(String defaultValue, double width) {
        TextField tf = new TextField(defaultValue);
        tf.setMaxWidth(width);
        tf.setStyle(
                "-fx-background-color: #313244; -fx-text-fill: #cdd6f4; " +
                "-fx-font-family: Monospaced; -fx-font-size: 13; -fx-padding: 6 10;");
        return tf;
    }

    private void styleConnectButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; " +
                "-fx-font-family: Monospaced; -fx-font-weight: bold; -fx-font-size: 13; " +
                "-fx-cursor: hand; -fx-padding: 8 24;");
    }

    private void setError(Label lbl, String msg) {
        lbl.setStyle("-fx-text-fill: #f38ba8; -fx-font-family: Monospaced; -fx-font-size: 11;");
        lbl.setText(msg);
    }

    // =========================================================================
    //  Entry point
    // =========================================================================
    public static void main(String[] args) {
        launch(args);
    }
}
