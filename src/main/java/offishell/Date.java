/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell;

import static java.time.temporal.ChronoField.*;
import static java.time.temporal.ChronoUnit.*;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Objects;

/**
 * @version 2016/06/23 10:17:18
 */
public class Date {

    /** 詰め文字 */
    private static final String padding = "";

    /** 元号 */
    private static final DateTimeFormatter ERA = DateTimeFormatter.ofPattern("G").withChronology(JapaneseChronology.INSTANCE);

    /** 曜日 */
    private static final DateTimeFormatter dow = DateTimeFormatter.ofPattern("E").withChronology(JapaneseChronology.INSTANCE);

    /** The actual date. */
    public final LocalDate date;

    private final JapaneseDate japanese;

    /**
     * @param date
     */
    public Date(LocalDate date) {
        this.date = date;
        this.japanese = JapaneseDate.from(date);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Date) {
            return Objects.equals(date, ((Date) obj).date);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return 年月日曜();
    }

    /**
     * Returns a copy of this date with the specified amount added.
     * <p>
     * This returns a {@code LocalDate}, based on this one, with the amount in terms of the unit
     * added. If it is not possible to add the amount, because the unit is not supported or for some
     * other reason, an exception is thrown.
     * <p>
     * In some cases, adding the amount can cause the resulting date to become invalid. For example,
     * adding one month to 31st January would result in 31st February. In cases like this, the unit
     * is responsible for resolving the date. Typically it will choose the previous valid date,
     * which would be the last valid day of February in this example.
     * <p>
     * If the field is a {@link ChronoUnit} then the addition is implemented here. The supported
     * fields behave as follows:
     * <p>
     * All other {@code ChronoUnit} instances will throw an {@code UnsupportedTemporalTypeException}
     * .
     * <p>
     * If the field is not a {@code ChronoUnit}, then the result of this method is obtained by
     * invoking {@code TemporalUnit.addTo(Temporal, long)} passing {@code this} as the argument. In
     * this case, the unit determines whether and how to perform the addition.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToAdd the amount of the unit to add to the result, may be negative
     * @param unit the unit of the amount to add, not null
     * @return a {@code LocalDate} based on this date with the specified amount added, not null
     * @throws DateTimeException if the addition cannot be made
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Date plus(long amountToAdd, TemporalUnit unit) {
        return new Date(date.plus(amountToAdd, unit));
    }

    /**
     * Returns a copy of this date with the specified amount subtracted.
     * <p>
     * This returns a {@code LocalDate}, based on this one, with the amount in terms of the unit
     * subtracted. If it is not possible to subtract the amount, because the unit is not supported
     * or for some other reason, an exception is thrown.
     * <p>
     * This method is equivalent to {@link #plus(long, TemporalUnit)} with the amount negated. See
     * that method for a full description of how addition, and thus subtraction, works.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param amountToMinus the amount of the unit to subtract from the result, may be negative
     * @param unit the unit of the amount to subtract, not null
     * @return a {@code LocalDate} based on this date with the specified amount subtracted, not null
     * @throws DateTimeException if the subtraction cannot be made
     * @throws UnsupportedTemporalTypeException if the unit is not supported
     * @throws ArithmeticException if numeric overflow occurs
     */
    public Date minus(long amountToMinus, TemporalUnit unit) {
        return new Date(date.minus(amountToMinus, unit));
    }

    /**
     * <p>
     * 西暦の数値を返します。
     * </p>
     * 
     * @return
     */
    public int year() {
        return date.get(ChronoField.YEAR_OF_ERA);
    }

    /**
     * <p>
     * 和暦の数値を返します。
     * </p>
     * 
     * @return
     */
    public int yearJP() {
        return JapaneseDate.from(date).get(ChronoField.YEAR_OF_ERA);
    }

    /**
     * <p>
     * 来年の西暦の数値を返します。
     * </p>
     * 
     * @return
     */
    public int yearNext() {
        return date.plusYears(1).get(ChronoField.YEAR_OF_ERA);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String year(String fill) {
        return year(date, fill, 4, "年");
    }

    /**
     * <p>
     * 西暦年度の数値を返します。
     * </p>
     * 
     * @return
     */
    public int period() {
        return date.minus(3, MONTHS).get(YEAR_OF_ERA);
    }

    /**
     * <p>
     * 整形した西暦年度を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String period(String fill) {
        return year(date.minus(3, MONTHS), fill, 4, "年度");
    }

    /**
     * <p>
     * 月の数値を返します。
     * </p>
     * 
     * @return
     */
    public int month() {
        return date.get(ChronoField.MONTH_OF_YEAR);
    }

    /**
     * <p>
     * 日の数値を返します。
     * </p>
     * 
     * @return
     */
    public int day() {
        return date.get(ChronoField.DAY_OF_MONTH);
    }

    /**
     * <p>
     * 和暦の数値を返します。
     * </p>
     * 
     * @return
     */
    public int 年数() {
        return japanese.get(ChronoField.YEAR_OF_ERA);
    }

    /**
     * <p>
     * 整形した和暦を返します。
     * </p>
     * 
     * @return
     */
    public String 年() {
        return 年(padding);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 年(String fill) {
        return year(japanese, fill, 2, "年").replaceAll("令和1年", "令和元年");
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @return
     */
    public String 年月() {
        return 年月(padding);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 年月(String fill) {
        return 年(fill) + 月(fill);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @return
     */
    public String 年月日() {
        return 年月日(padding);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 年月日(String fill) {
        return 年月(fill) + 日(fill);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @return
     */
    public String 年月日曜() {
        return 年月日曜(padding);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 年月日曜(String fill) {
        return 年月日(fill) + "（" + dow(japanese) + "）";
    }

    /**
     * <p>
     * 西暦年度の数値を返します。
     * </p>
     * 
     * @return
     */
    public int 年度数() {
        return japanese.minus(3, MONTHS).get(YEAR_OF_ERA);
    }

    /**
     * <p>
     * 整形した西暦年度を返します。
     * </p>
     * 
     * @return
     */
    public String 年度() {
        return 年度(padding);
    }

    /**
     * <p>
     * 整形した西暦年度を返します。
     * </p>
     * 
     * @return
     */
    public String 短縮年度() {
        return 年度(padding).replace("元年", "01年").replace("年度", "").replace("平成", "H").replace("昭和", "S").replace("令和", "R");
    }

    /**
     * <p>
     * 整形した西暦年度を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 年度(String fill) {
        return year(japanese.minus(3, MONTHS), fill, 2, "年度").replace("平成31年", "令和元年").replace("令和1年", "令和元年");
    }

    /**
     * <p>
     * 整形した西暦年度を返します。
     * </p>
     * 
     * @return
     */
    public String 年度月() {
        return 年度月(padding);
    }

    /**
     * <p>
     * 整形した西暦年度を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 年度月(String fill) {
        return 年度(fill) + 月(fill);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @return
     */
    public String 月() {
        return 月(padding);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 月(String fill) {
        return month(japanese, fill, "月");
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @return
     */
    public String 月日() {
        return 月日(padding);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 月日(String fill) {
        return 月(fill) + 日(fill);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @return
     */
    public String 月日曜() {
        return 月日曜(padding);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 月日曜(String fill) {
        return 月日(fill) + "（" + dow(japanese) + "）";
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @return
     */
    public String 日() {
        return 日(padding);
    }

    /**
     * <p>
     * 整形した西暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 日(String fill) {
        return day(japanese, fill, "日");
    }

    /**
     * <p>
     * 整形した和暦を返します。
     * </p>
     * 
     * @param fill
     * @return
     */
    public String 曜日(String fill) {
        return dow(japanese);
    }

    /**
     * <p>
     * 今日と比較して過去かどうか
     * </p>
     * 
     * @return
     */
    public boolean isPast() {
        return date.isBefore(LocalDate.now());
    }

    /**
     * <p>
     * 今日と比較して未来かどうか
     * </p>
     * 
     * @return
     */
    public boolean isFuture() {
        return date.isAfter(LocalDate.now());
    }

    /**
     * <p>
     * Helper method to retrive japanese era.
     * </p>
     * 
     * @param date
     * @return
     */
    private String era(ChronoLocalDate date) {
        if (date instanceof JapaneseDate) {
            return ERA.format(date);
        } else {
            return "";
        }
    }

    /**
     * <p>
     * Helper method to format the specified {@link ChronoLocalDate}.
     * </p>
     * 
     * @param date
     * @param fill
     * @param size
     * @param postfix
     * @return
     */
    private String year(ChronoLocalDate date, String fill, int size, String postfix) {
        return era(date) + Padding.size(size).text(fill).format(date.get(YEAR_OF_ERA)) + postfix;
    }

    /**
     * <p>
     * Helper method to format the specified {@link ChronoLocalDate}.
     * </p>
     * 
     * @param date
     * @param fill
     * @param size
     * @param postfix
     * @return
     */
    private String month(ChronoLocalDate date, String fill, String postfix) {
        return Padding.size(2).text(fill).format(date.get(MONTH_OF_YEAR)) + postfix;
    }

    /**
     * <p>
     * Helper method to format the specified {@link ChronoLocalDate}.
     * </p>
     * 
     * @param date
     * @param fill
     * @param size
     * @param postfix
     * @return
     */
    private String day(ChronoLocalDate date, String fill, String postfix) {
        return Padding.size(2).text(fill).format(date.get(DAY_OF_MONTH)) + postfix;
    }

    /**
     * <p>
     * Helper method to format the specified {@link ChronoLocalDate}.
     * </p>
     * 
     * @param date
     * @return
     */
    private String dow(ChronoLocalDate date) {
        return dow.format(date);
    }

    /**
     * 現在日時を返します。
     * 
     * @return
     */
    public static Date now() {
        return of(LocalDate.now());
    }

    /**
     * @param month
     * @param dayOfMonth
     * @return
     */
    public static Date of(int month, int dayOfMonth) {
        return of(LocalDate.now().getYear(), month, dayOfMonth);
    }

    /**
     * @param year
     * @param month
     * @param dayOfMonth
     * @return
     */
    public static Date of(int year, int month, int dayOfMonth) {
        return of(LocalDate.of(year, month, dayOfMonth));
    }

    /**
     * @param date
     * @return
     */
    public static Date of(LocalDate date) {
        if (date == null) {
            return now();
        }
        return new Date(date);
    }

    /**
     * <p>
     * 現在の西暦数を返す。
     * </p>
     * 
     * @return
     */
    public static int thisYear() {
        return now().year();
    }

    /**
     * <p>
     * 現在の西暦年度数を返す。
     * </p>
     * 
     * @return
     */
    public static int thisYearPeriod() {
        return now().period();
    }
}