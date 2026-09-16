package org.example.notes.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public final class Icons {
    private Icons() {
    }

    public static Node sidebar(String color) {
        StackPane box = new StackPane();
        Rectangle frame = roundRect(18, 14, 3, Color.TRANSPARENT, color, 1.4);
        Line split = line(7, 3, 7, 13, color, 1.4);
        box.getChildren().addAll(frame, split);
        box.setMaxSize(20, 16);
        return box;
    }

    public static Node gallery(String color) {
        StackPane box = new StackPane();
        Rectangle a = roundRect(7, 7, 1.5, Color.TRANSPARENT, color, 1.3);
        Rectangle b = roundRect(7, 7, 1.5, Color.TRANSPARENT, color, 1.3);
        Rectangle c = roundRect(7, 7, 1.5, Color.TRANSPARENT, color, 1.3);
        Rectangle d = roundRect(7, 7, 1.5, Color.TRANSPARENT, color, 1.3);
        a.setTranslateX(-5);
        a.setTranslateY(-5);
        b.setTranslateX(5);
        b.setTranslateY(-5);
        c.setTranslateX(-5);
        c.setTranslateY(5);
        d.setTranslateX(5);
        d.setTranslateY(5);
        box.getChildren().addAll(a, b, c, d);
        box.setMaxSize(18, 18);
        return box;
    }

    public static Node list(String color) {
        StackPane box = new StackPane();
        box.getChildren().addAll(
                line(-7, -5, 7, -5, color, 1.5),
                line(-7, 0, 7, 0, color, 1.5),
                line(-7, 5, 7, 5, color, 1.5)
        );
        box.setMaxSize(18, 14);
        return box;
    }

    public static Node pencil(String color) {
        SVGPath path = new SVGPath();
        path.setContent("M3 14.5 L12.5 5 L15.5 8 L6 17.5 L3 18 Z M11.2 6.3 L14.2 9.3");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web(color));
        path.setStrokeWidth(1.5);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        return wrap(path, 20, 20);
    }

    public static Node trash(String color) {
        SVGPath path = new SVGPath();
        path.setContent("M4 6 h12 M7 6 v-2 h6 v2 M6 6 v11 h8 V6");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web(color));
        path.setStrokeWidth(1.5);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        return wrap(path, 20, 20);
    }

    public static Node pin(String color) {
        SVGPath path = new SVGPath();
        path.setContent("M9 2.5 c3 0 5 2.2 5 4.6 c0 2.6-3.2 5.4-5 7.4 c-1.8-2-5-4.8-5-7.4 c0-2.4 2-4.6 5-4.6 z M9 14.5 v4");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web(color));
        path.setStrokeWidth(1.45);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        return wrap(path, 18, 20);
    }

    public static Node checklist(String color) {
        StackPane box = new StackPane();
        Rectangle r = roundRect(16, 16, 3, Color.TRANSPARENT, color, 1.5);
        Line a = line(-3, 1, -0.5, 4, color, 1.6);
        Line b = line(-0.5, 4, 5, -3, color, 1.6);
        box.getChildren().addAll(r, a, b);
        box.setMaxSize(18, 18);
        return box;
    }

    public static Node table(String color) {
        SVGPath path = new SVGPath();
        path.setContent("M3 4 h14 v12 h-14 z M3 8 h14 M3 12 h14 M8 4 v12");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web(color));
        path.setStrokeWidth(1.4);
        return wrap(path, 20, 20);
    }

    public static Node formatAa(String color) {
        Text t = new Text("Aa");
        t.setFill(Color.web(color));
        t.setFont(Font.font("Noto Sans", FontWeight.BOLD, 13));
        return t;
    }

    public static Node search(String color) {
        StackPane box = new StackPane();
        Circle c = new Circle(5.2, Color.TRANSPARENT);
        c.setStroke(Color.web(color));
        c.setStrokeWidth(1.5);
        c.setTranslateX(-2);
        c.setTranslateY(-2);
        Line handle = line(3, 3, 7, 7, color, 1.6);
        box.getChildren().addAll(c, handle);
        box.setMaxSize(16, 16);
        return box;
    }

    public static Node folder() {
        SVGPath path = new SVGPath();
        path.setContent("M2 6.5 a1.5 1.5 0 0 1 1.5-1.5 h4.2 l1.3 1.6 h7.5 a1.5 1.5 0 0 1 1.5 1.5 v7 a1.5 1.5 0 0 1-1.5 1.5 H3.5 a1.5 1.5 0 0 1-1.5-1.5 z");
        path.setFill(Color.web("#FFCC00"));
        path.setStroke(Color.web("#E6B800"));
        path.setStrokeWidth(0.6);
        return wrap(path, 18, 16);
    }

    public static Node notesPad() {
        StackPane box = new StackPane();
        Rectangle page = roundRect(14, 16, 2.2, Color.web("#FFD60A"), Color.web("#E6B800"), 0.6);
        Line l1 = line(-4, -1, 4, -1, "#C7A000", 1.1);
        Line l2 = line(-4, 2.5, 4, 2.5, "#C7A000", 1.1);
        Line l3 = line(-4, 6, 2, 6, "#C7A000", 1.1);
        box.getChildren().addAll(page, l1, l2, l3);
        box.setMaxSize(16, 18);
        return box;
    }

    public static Node allNotes() {
        StackPane box = new StackPane();
        Rectangle back = roundRect(13, 15, 2, Color.web("#FFE566"), Color.web("#E6B800"), 0.6);
        back.setTranslateX(2);
        back.setTranslateY(-2);
        Rectangle front = roundRect(13, 15, 2, Color.web("#FFD60A"), Color.web("#E6B800"), 0.6);
        front.setTranslateX(-1);
        front.setTranslateY(1);
        box.getChildren().addAll(back, front);
        box.setMaxSize(16, 18);
        return box;
    }

    public static Node plus(String color) {
        StackPane box = new StackPane();
        box.getChildren().addAll(
                line(-6, 0, 6, 0, color, 1.7),
                line(0, -6, 0, 6, color, 1.7)
        );
        box.setMinSize(14, 14);
        box.setMaxSize(14, 14);
        return box;
    }

    public static Node recover(String color) {
        Arc arc = new Arc(0, 0, 6, 6, 40, 260);
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(Color.web(color));
        arc.setStrokeWidth(1.5);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        return wrap(arc, 16, 16);
    }

    public static Node tag(String color) {
        SVGPath path = new SVGPath();
        path.setContent("M3 10 L10 3 h6 v6 L9 16 z M14 6.5 a1 1 0 1 1 0 0.1");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web(color));
        path.setStrokeWidth(1.4);
        return wrap(path, 18, 16);
    }

    public static Node emptyState(String color) {
        return notesPad();
    }

    private static Rectangle roundRect(double w, double h, double arc, Color fill, String stroke, double sw) {
        return roundRect(w, h, arc, fill, Color.web(stroke), sw);
    }

    private static Rectangle roundRect(double w, double h, double arc, Color fill, Color stroke, double sw) {
        Rectangle r = new Rectangle(w, h, fill);
        r.setArcWidth(arc * 2);
        r.setArcHeight(arc * 2);
        r.setStroke(stroke);
        r.setStrokeWidth(sw);
        return r;
    }

    private static Line line(double x1, double y1, double x2, double y2, String color, double w) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.web(color));
        line.setStrokeWidth(w);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }

    private static Node wrap(Node node, double w, double h) {
        StackPane pane = new StackPane(node);
        pane.setAlignment(Pos.CENTER);
        pane.setMinSize(w, h);
        pane.setPrefSize(w, h);
        pane.setMaxSize(w, h);
        if (node instanceof Region region) {
            region.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }
        return pane;
    }
}
