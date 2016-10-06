/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.macro.lol;

import javafx.beans.property.BooleanProperty;

import offishell.macro.Key;
import offishell.macro.Macro;
import offishell.macro.Window;
import offishell.platform.Location;
import offishell.platform.Native;

/**
 * @version 2016/10/05 17:03:59
 */
public abstract class LoLMacro extends Macro {

    /** The debug mode. */
    private static final boolean debug = true;

    /** The shown skill. */
    private Skill skillForShowRange;

    /**
     * 
     */
    protected LoLMacro() {
        requireTitle("League of Legends (TM) Client");

        when(Key.F12).consume().press().to(e -> {
            debugSkillColor();
        });

        // configure skill range mode
        for (Skill skill : new Skill[] {Skill.AA, Skill.Q, Skill.W, Skill.E, Skill.R}) {
            when(skill.key).withAlt().press().to(e -> {
                skillForShowRange = skillForShowRange == skill ? null : skill;

                log("Show range [" + skillForShowRange + "]");
            });
        }

        for (Skill skill : new Skill[] {Skill.Move, Skill.Q, Skill.W, Skill.E, Skill.R}) {
            when(skill.key).press().to(e -> {
                if (skillForShowRange != null) {
                    if (canCast(skillForShowRange)) {
                        if (skill == Skill.Move) {
                            input(Key.O);
                        }
                        delay(10);
                        input(skillForShowRange.rangeKey);
                    }
                }
            });
        }

        // configure combo
        when(Key.MouseLeft).press().to(e -> {
            BooleanProperty released = when(Key.MouseLeft).release().take(1).toBinary();

            while (!released.get()) {
                combo();
            }
        });

        // debug
        when(Key.Pause).consume().press().to(e -> {
            Window window = window();
            Location mouse = window.mousePosition();

            for (int i = -4; i < 4; i++) {
                Location moved = mouse.slideY(i);
                System.out.println(moved + "   " + window.color(moved));
            }
        });
    }

    /**
     * Declare combo action.
     */
    protected abstract void combo();

    /**
     * <p>
     * Check castable skill.
     * </p>
     * 
     * @param skill
     * @return
     */
    protected final boolean canCast(Skill skill) {
        if (skill.castableColor == 0) {
            return true;
        }
        return window().color(skill.locationX, skill.locationY).code == skill.castableColor;
    }

    /**
     * <p>
     * Cast the specified skill.
     * </p>
     * 
     * @param skill
     */
    protected final void cast(Skill skill) {
        cast(skill, 10);
    }

    /**
     * <p>
     * Cast the specified skill.
     * </p>
     * 
     * @param skill
     */
    protected final void cast(Skill skill, int delay) {
        if (skill == Skill.AM) {
            attackMove(99);
        } else {
            if (canCast(skill)) {
                input(skill.key);

                if (0 < delay) {
                    delay(delay);
                }
            }
        }
    }

    /**
     * <p>
     * Cast the specified skill.
     * </p>
     * 
     * @param skill
     */
    protected final void selfCast(Skill skill) {
        selfCast(skill, 10);
    }

    /**
     * <p>
     * Cast the specified skill.
     * </p>
     * 
     * @param skill
     */
    protected final void selfCast(Skill skill, int delay) {
        if (canCast(skill)) {
            inputParallel(Key.Shift, skill.key);

            if (0 < delay) {
                delay(delay);
            }
        }
    }

    /**
     * <p>
     * Declare skill action.
     * </p>
     * 
     * @param skill
     * @param action
     */
    protected final void when(Skill skill, Runnable action) {
        when(skill.key).consume().press().to(action::run);
    }

    /**
     * Debug command.
     */
    protected final void debugSkillColor() {
        System.out.println("Show skills color");
        for (Skill skill : Skill.values()) {
            Location location = window().locate(skill.locationX, skill.locationY);
            System.out.println(skill + "\t\t" + location + "\t\t" + window().color(skill.locationX, skill.locationY));
        }
    }

    /** The attack move state. */
    private long moveLatest;

    /** The attack move state. */
    private long attackLatest;

    /**
     * <p>
     * Attack move gracefully.
     * </p>
     * 
     * @param attackInterval
     */
    private void attackMove(int attackInterval) {
        int moveInterval = 75;
        long now = System.currentTimeMillis();

        if (moveLatest + moveInterval < now) {
            int attackMotion = computeAttackMotion();

            if (attackLatest + attackMotion < now) {
                input(Skill.AM.key).delay(attackMotion);
                attackLatest = System.currentTimeMillis();
            }
            input(Skill.Move.key);
            moveLatest = System.currentTimeMillis();
        }
    }

    /**
     * <p>
     * Compute the current attack speed.
     * </p>
     * 
     * @return
     */
    private int computeAttackMotion() {
        try {
            int attackSpeed = (int) (Float.valueOf(Native.API.ocr(593, 1091, 38, 15)) * 100);
            return Math.max(50000 / attackSpeed, 125);
        } catch (Throwable e) {
            e.printStackTrace();
            return 500;
        }
    }

    /**
     * Log writer.
     * 
     * @param texts
     */
    private void log(Object... texts) {
        if (debug) {
            StringBuilder builder = new StringBuilder();

            for (Object text : texts) {
                builder.append(text);
            }
            System.out.println(builder);
        }
    }

    /** The skill location constants. */
    private static final int SkillBaseX = 700;

    private static final int SkillGapX = 43;

    private static final int SkillBaseY = 991;

    private static final int SkillColor = 8111079;

    private static final int ItemBaseX = 960;

    private static final int ItemBaseY = 990;

    private static final int ItemGapX = 31;

    private static final int ItemGapY = 30;

    private static final int ItemColor = 4942200;

    /**
     * @version 2016/10/05 13:58:25
     */
    public static enum Skill {
        Move(Key.MouseRight, Key.O, Key.MouseRight, 0, 0, 0),

        AA(Key.MouseRight, Key.P, Key.I, 0, 0, 0),

        AM(Key.MouseRight, Key.P, Key.I, 0, 0, 0),

        Q(Key.Q, Key.Q, Key.H, SkillBaseX, SkillBaseY, 8111079),

        W(Key.W, Key.W, Key.J, SkillBaseX + SkillGapX * 1, SkillBaseY, SkillColor),

        E(Key.E, Key.E, Key.K, SkillBaseX + SkillGapX * 2, SkillBaseY, SkillColor),

        R(Key.Q, Key.R, Key.L, SkillBaseX + SkillGapX * 3, SkillBaseY, SkillColor),

        SS1(Key.D, Key.D, Key.H, SkillBaseX + SkillGapX * 4, SkillBaseY, SkillColor),

        SS2(Key.F, Key.F, Key.H, SkillBaseX + SkillGapX * 5, SkillBaseY, 7583454),

        Item1(Key.N1, Key.N1, Key.H, ItemBaseX + ItemGapX * 0, ItemBaseY, ItemColor),

        Item2(Key.N2, Key.N2, Key.H, ItemBaseX + ItemGapX * 1, ItemBaseY, ItemColor),

        Item3(Key.N3, Key.N3, Key.H, ItemBaseX + ItemGapX * 2, ItemBaseY, ItemColor),

        Item4(Key.N5, Key.N5, Key.H, ItemBaseX + ItemGapX * 0, ItemBaseY + ItemGapY, ItemColor),

        Item5(Key.N6, Key.N6, Key.H, ItemBaseX + ItemGapX * 1, ItemBaseY + ItemGapY, ItemColor),

        Item6(Key.N7, Key.N7, Key.H, ItemBaseX + ItemGapX * 2, ItemBaseY + ItemGapY, ItemColor),

        Trinket(Key.N4, Key.N4, Key.H, 0, 0, 0);

        private final Key mainKey;

        private final Key key;

        private final Key rangeKey;

        private final int locationX;

        private final int locationY;

        private final int castableColor;

        /**
         * <p>
         * Skill infomation.
         * </p>
         */
        private Skill(Key mainKey, Key key, Key rangeKey, int locationX, int locationY, int castableColor) {
            this.mainKey = mainKey;
            this.key = key;
            this.rangeKey = rangeKey;
            this.locationX = locationX;
            this.locationY = locationY;
            this.castableColor = castableColor;
        }
    }
}
