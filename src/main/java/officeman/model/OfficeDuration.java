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

public class OfficeDuration {

    /** 開始時間 */
    public OfficeTime start;

    /** 終了時間 */
    public OfficeTime end;

    /**
     * Hide
     */
    private OfficeDuration() {
    }

    /**
     * 開始時間を設定。
     * 
     * @param hour
     * @param minute
     * @return
     */
    public static OfficeDuration start(int hour, int minute) {
        OfficeDuration duration = new OfficeDuration();
        duration.start = OfficeTime.of(hour, minute);
        duration.end = OfficeTime.of(hour, minute);

        return duration;
    }

    /**
     * 終了時間を設定。
     * 
     * @param hour
     * @param minute
     * @return
     */
    public OfficeDuration end(int hour, int minute) {
        end = OfficeTime.of(hour, minute);

        return this;
    }

    /**
     * "10時30分 ～ 15時45分"形式で期間を取得。
     * 
     * @return
     */
    public String japanese() {
        return start.japanese() + " ～ " + end.japanese();
    }
}