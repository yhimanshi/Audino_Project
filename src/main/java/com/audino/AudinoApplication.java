package com.audino;

import com.audino.util.ConfigurationManager;
import com.audino.util.AppSeeder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.io.IOException;

public class AudinoApplication extends Application {
    private static FileChannel lockChannel;
    private static FileLock appLock;

    @Override
    public void stop() {
        ConfigurationManager.getInstance().closeDataSource();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        ConfigurationManager.getInstance().initialize();

        try {
            com.audino.service.DataService ds = new com.audino.service.DataService();
            if (ds.getAllMedications().isEmpty()) {
                System.out.println("[INIT] Empty database detected. Automatically running AppSeeder...");
                AppSeeder.seed(ConfigurationManager.getInstance());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadFonts();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        VBox root = loader.load();

        Scene scene = new Scene(root, 400, 430);
        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());

        primaryStage.setTitle("Audino: Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void loadFonts() {
        try {
            Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Regular.ttf"), 10);
            Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Bold.ttf"), 10);
            Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Light.ttf"), 10);
            Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Medium.ttf"), 10);
            Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-SemiBold.ttf"), 10);
        } catch (Exception e) {
            System.err.println("Could not load fonts: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Path lockPath = Paths.get(
                    System.getProperty("java.io.tmpdir"),
                    "audino-app.lock"
            );

            lockChannel = FileChannel.open(
                    lockPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );

            appLock = lockChannel.tryLock();

            if (appLock == null) {
                System.err.println(
                        "Audino is already running. Close the existing window first."
                );
                lockChannel.close();
                return;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (appLock != null && appLock.isValid()) {
                        appLock.release();
                    }
                    if (lockChannel != null && lockChannel.isOpen()) {
                        lockChannel.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            launch(args);

        } catch (OverlappingFileLockException e) {
            System.err.println("Audino is already running.");
        } catch (IOException e) {
            System.err.println("Could not acquire Audino application lock.");
            e.printStackTrace();
        }
    }
}