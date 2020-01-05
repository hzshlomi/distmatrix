package org.distmatrix.actor;

public class Point {
    public final double x;
    public final double y;

    Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Point of(double x, double y) {
        return new Point(x, y);
    }

    @Override
    public String toString() {
        return "(" + x +
                "," + y +
                ')';
    }
}
