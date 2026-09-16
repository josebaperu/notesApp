package org.example.notes.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.function.BiConsumer;

public class FormatPopup {
    private final Popup popup = new Popup();
    private BiConsumer<String, String> onCommand = (c, v) -> {
    };

    public FormatPopup() {
        VBox root = new VBox(2);
        root.getStyleClass().add("format-popup");
        root.setPadding(new Insets(6));
        root.getChildren().addAll(
                item("Title", "title", null, "title-style"),
                item("Heading", "heading", null, "heading-style"),
                item("Subheading", "subheading", null, "subheading-style"),
                item("Body", "body", null, null),
                item("Monostyled", "mono", null, null),
                new Separator(),
                commandRow(),
                new Separator(),
                listRow(),
                new Separator(),
                highlightRow()
        );
        popup.getContent().add(root);
        popup.setAutoHide(true);
    }

    public void setOnCommand(BiConsumer<String, String> onCommand) {
        this.onCommand = onCommand;
    }

    public void show(javafx.scene.Node owner) {
        var bounds = owner.localToScreen(owner.getBoundsInLocal());
        popup.show(owner, bounds.getMinX(), bounds.getMaxY() + 6);
    }

    public void hide() {
        popup.hide();
    }

    private Button item(String label, String command, String value, String extraClass) {
        Button button = new Button(label);
        button.getStyleClass().add("format-item");
        if (extraClass != null) {
            button.getStyleClass().add(extraClass);
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> fire(command, value));
        return button;
    }

    private HBox commandRow() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
                tiny("B", "bold", null),
                tiny("I", "italic", null),
                tiny("U", "underline", null),
                tiny("S", "strikeThrough", null)
        );
        return row;
    }

    private HBox listRow() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);
        row.getChildren().addAll(
                tiny("•", "insertUnorderedList", null),
                tiny("1.", "insertOrderedList", null),
                tiny("☑", "checklist", null),
                tiny("⊞", "table", null)
        );
        return row;
    }

    private HBox highlightRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 8, 4, 8));
        Label label = new Label("Highlight");
        label.getStyleClass().add("folder-count");
        row.getChildren().add(label);
        row.getChildren().addAll(
                swatch("#E9D5FF"),
                swatch("#FECDD3"),
                swatch("#FED7AA"),
                swatch("#BBF7D0"),
                swatch("#BFDBFE")
        );
        return row;
    }

    private Button tiny(String text, String command, String value) {
        Button button = new Button(text);
        button.getStyleClass().add("format-item");
        button.setOnAction(e -> fire(command, value));
        return button;
    }

    private javafx.scene.layout.Region swatch(String color) {
        javafx.scene.layout.Region region = new javafx.scene.layout.Region();
        region.getStyleClass().add("swatch");
        region.setStyle("-fx-background-color: " + color + ";");
        region.setOnMouseClicked(e -> fire("hiliteColor", color));
        return region;
    }

    private void fire(String command, String value) {
        onCommand.accept(command, value);
        popup.hide();
    }
}
