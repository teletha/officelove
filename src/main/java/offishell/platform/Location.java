/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.platform;

/**
 * @version 2016/10/05 2:08:44
 */
public class Location {

    /** The absolute this location. */
    final int x;

    /** The absolute this location. */
    final int y;

    /** The absolute base location. */
    private final int baseX;

    /** The absolute base location. */
    private final int baseY;

    /** The acceptable range. */
    private final int maxX;

    /** The acceptable range. */
    private final int maxY;

    /**
     * @param x
     * @param y
     * @param width
     * @param height
     */
    Location(int x, int y) {
        this(x, y, x, y, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * <p>
     * Create location info.
     * </p>
     */
    Location(int x, int y, int baseX, int baseY, int width, int height) {
        this.x = x;
        this.y = y;
        this.baseX = baseX;
        this.baseY = baseY;
        this.maxX = width;
        this.maxY = height;
    }

    /**
     * <p>
     * Create relative range.
     * </p>
     * 
     * @param cursor
     * @return
     */
    public Location relative(Location other) {
        if (other == null) {
            return this;
        }
        return new Location(other.x, other.y, baseX, baseY, maxX, maxY);
    }

    /**
     * <p>
     * Compute x position.
     * </p>
     * 
     * @return
     */
    public int x() {
        return x - baseX;
    }

    /**
     * <p>
     * Compute y position.
     * </p>
     * 
     * @return
     */
    public int y() {
        return y - baseY;
    }

    /**
     * <p>
     * Compute x position.
     * </p>
     * 
     * @return
     */
    public int X() {
        return x;
    }

    /**
     * <p>
     * Compute y position.
     * </p>
     * 
     * @return
     */
    public int Y() {
        return y;
    }

    /**
     * <p>
     * Compute x position.
     * </p>
     * 
     * @return
     */
    public int width() {
        return maxX;
    }

    /**
     * <p>
     * Compute y position.
     * </p>
     * 
     * @return
     */
    public int height() {
        return maxY;
    }

    /**
     * @param locationX
     * @param locationY
     * @return
     */
    public Location slide(int locationX, int locationY) {
        return new Location(x + locationX, y + locationY, baseX, baseY, maxX, maxY);
    }

    /**
     * <p>
     * Slide x-location slighty.
     * </p>
     * 
     * @param locationX
     * @return
     */
    public Location slideX(int locationX) {
        if (locationX == 0) {
            return this;
        }
        return new Location(x + locationX, y, baseX, baseY, maxX, maxY);
    }

    /**
     * <p>
     * Slide y-location slighty.
     * </p>
     * 
     * @param locationY
     * @return
     */
    public Location slideY(int locationY) {
        if (locationY == 0) {
            return this;
        }
        return new Location(x, y + locationY, baseX, baseY, maxX, maxY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Location.class
                .getSimpleName() + "  [  x: " + format(x - baseX) + "\ty: " + format(y - baseY) + "\tX: " + format(x) + "\tY: " + format(y) + "\tbaseX: " + format(baseX) + "\tbaseY: " + format(baseY) + "]";
    }

    /**
     * Format value.
     * 
     * @param value
     * @return
     */
    private String format(int value) {
        String text = String.valueOf(value);
        int remaing = 6 - text.length();

        for (int i = 0; i < remaing; i++) {
            text += " ";
        }
        return text;
    }

    /**
     * @param locationX
     * @param locationY
     * @return
     */
    public static Location of(int locationX, int locationY) {
        return new Location(locationX, locationY);
    }
}
