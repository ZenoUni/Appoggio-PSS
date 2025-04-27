package com.pacman;

public enum Direction {
    UP, DOWN, LEFT, RIGHT;

    public static Direction randomDirection() {
        Direction[] directions = values();
        return directions[(int) (Math.random() * directions.length)];
    }
}
