package org.distmatrix.actor;

public class IndexedPoint {
    public Point point;
    public int index;

    IndexedPoint(Point point, int index) {
        if (index < 0) throw new IllegalArgumentException("negative index: " + index);
        this.point = point;
        this.index = index;
    }

    public static IndexedPoint of(Point point, int index) {
        return new IndexedPoint(point, index);
    }

    public static IndexedPoint of(Point point, Long index) {
        if (index > Integer.MAX_VALUE) throw new IllegalArgumentException("illegal index: " + index);
        return IndexedPoint.of(point, index.intValue());
    }

    @Override
    public String toString() {
        return "{" + index +
                "," + point +
                '}';
    }
}
