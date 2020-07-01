/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officeman.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class OfficeTime {

    private static final DateTimeFormatter COLON = DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter JAPANESE = DateTimeFormatter.ofPattern("HH'時'mm'分'");

    private final LocalTime time;

    /**
     * @param time
     */
    private OfficeTime(LocalTime time) {
        this.time = Objects.requireNonNull(time);
    }

    /**
     * "10:30"形式で時間を取得します。
     * 
     * @return
     */
    public String colon() {
        return COLON.format(time);
    }

    /**
     * "10時30分"形式で時間を取得します。
     * 
     * @return
     */
    public String japanese() {
        return JAPANESE.format(time);
    }

    /**
     * 時間(0～23)を取得します。
     * 
     * @return
     */
    public int hour() {
        return time.getHour();
    }

    /**
     * 分(0～59)を取得します。
     * 
     * @return
     */
    public int minute() {
        return time.getMinute();
    }

    /**
     * 指定した分だけ戻した時間を取得。
     */
    public OfficeTime minus(int minute) {
        return new OfficeTime(time.minusMinutes(minute));
    }

    /**
     * 指定した分だけ加えた時間を取得。
     */
    public OfficeTime plus(int minute) {
        return new OfficeTime(time.plusMinutes(minute));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return japanese();
    }

    /**
     * 不変の{@link OfficeTime}を作成。
     * 
     * @param hour
     * @param minute
     * @return
     */
    public static OfficeTime of(int hour, int minute) {
        return new OfficeTime(LocalTime.of(hour, minute));
    }
}