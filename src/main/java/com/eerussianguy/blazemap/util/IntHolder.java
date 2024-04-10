package com.eerussianguy.blazemap.util;

public class IntHolder {
    private int number;

    public IntHolder() {
        this(0);
    }

    public IntHolder(int number) {
        this.number = number;
    }

    public int get() {
        return number;
    }

    public void set(int number) {
        this.number = number;
    }

    public void add(int delta) {
        this.number += delta;
    }

    public void factor(double factor) {
        this.number *= factor;
    }
}
