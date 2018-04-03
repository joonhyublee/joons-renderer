package org.sunflow.math;

public final class Point2 {

    public float x, y;

    public Point2() {
    }

    public Point2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Point2(Point2 p) {
        x = p.x;
        y = p.y;
    }

    public final Point2 set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public final Point2 set(Point2 p) {
        x = p.x;
        y = p.y;
        return this;
    }

    @Override
    public final String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}