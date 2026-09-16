package org.example.notes;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.notes.persist.JsonRepository;
import org.example.notes.service.NotesService;
import org.example.notes.ui.NotesWorkspace;

import java.time.Clock;
import java.util.Objects;

public class NotesApp extends Application {
    @Override
    public void start(Stage stage) {
        Clock clock = Clock.systemDefaultZone();
        NotesService service = new NotesService(new JsonRepository(JsonRepository.defaultPath()), clock);
        service.load();

        NotesWorkspace workspace = new NotesWorkspace(stage, service, clock);
        Scene scene = new Scene(workspace, 1180, 760);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/notes.css")).toExternalForm());

        var icon = getClass().getResource("/icons/app-icon.jpg");
        if (icon != null) {
            stage.getIcons().add(new Image(icon.toExternalForm()));
        }
        stage.setTitle("Notes");
        stage.setMinWidth(880);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
