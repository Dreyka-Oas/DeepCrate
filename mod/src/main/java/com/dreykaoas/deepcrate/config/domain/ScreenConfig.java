package com.dreykaoas.deepcrate.config.domain;

/** What the crate screen draws. Read on the machine that draws it, never on a server. */
public final class ScreenConfig {
    private ScreenConfig() {}

    /** Past four digits a count runs out of its cell, so it is shortened and the tooltip carries the truth. */
    public static int abbreviateAbove = 999;
}
