package com.boopugstudios.dynamicjobseconomy.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Simple currency formatter to ensure consistent two-decimal formatting
 * with a dot decimal separator and no grouping, independent of default Locale.
 */
public final class EconomyFormat {
    // Backwards-compatible basic number formatter (US-style, 2 decimals, no grouping)
    private static final DecimalFormatSymbols LEGACY_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);
    private static final ThreadLocal<DecimalFormat> TWO_DEC = ThreadLocal.withInitial(() -> {
        DecimalFormat df = new DecimalFormat("0.00", LEGACY_SYMBOLS);
        df.setGroupingUsed(false);
        return df;
    });

    // Config-driven state (volatile for safe publication)
    private static volatile String SYMBOL = "$";
    private static volatile boolean PREFIX = true; // true = prefix, false = suffix
    private static volatile boolean SPACE = false; // space between symbol and number
    private static volatile int DECIMALS = 2;
    private static volatile boolean GROUPING = true;
    private static volatile char GROUPING_SEP = ',';
    private static volatile char DECIMAL_SEP = '.';

    // Abbreviation settings
    private static volatile boolean ABBR_ENABLED = false;
    private static volatile int ABBR_DECIMALS = 2;

    // Thread-local number formatter based on current config
    private static ThreadLocal<DecimalFormat> NUMBER_FMT = ThreadLocal.withInitial(EconomyFormat::createNumberFormat);

    private EconomyFormat() {}

    public static String format(double amount) {
        // Legacy number-only formatting (no currency symbol)
        return TWO_DEC.get().format(amount);
    }

    /**
     * Returns a number formatted according to config (grouping, decimals, separators) without currency symbol.
     */
    public static String number(double amount) {
        return NUMBER_FMT.get().format(amount);
    }

    /**
     * Returns a money string with currency symbol applied according to config.
     */
    public static String money(double amount) {
        String num = number(amount);
        if (PREFIX) {
            return SYMBOL + (SPACE ? " " : "") + num;
        } else {
            return num + (SPACE ? " " : "") + SYMBOL;
        }
    }

    /**
     * Returns an abbreviated money string (e.g., $1.2K, $3.4M) when enabled. Falls back to money() if disabled.
     */
    public static String moneyAbbrev(double amount) {
        if (!ABBR_ENABLED) return money(amount);
        double abs = Math.abs(amount);
        String suffix = "";
        double base = amount;
        if (abs >= 1_000_000_000d) { // B
            suffix = "B";
            base = amount / 1_000_000_000d;
        } else if (abs >= 1_000_000d) { // M
            suffix = "M";
            base = amount / 1_000_000d;
        } else if (abs >= 1_000d) { // K
            suffix = "K";
            base = amount / 1_000d;
        }
        // Build a compact formatter for abbreviation decimals (no grouping)
        DecimalFormatSymbols syms = new DecimalFormatSymbols(Locale.US);
        syms.setDecimalSeparator(DECIMAL_SEP);
        StringBuilder pat = new StringBuilder("0");
        if (ABBR_DECIMALS > 0) {
            pat.append('.');
            for (int i = 0; i < ABBR_DECIMALS; i++) pat.append('0');
        }
        DecimalFormat df = new DecimalFormat(pat.toString(), syms);
        String num = df.format(base);
        String out = num + suffix;
        if (PREFIX) {
            return SYMBOL + (SPACE ? " " : "") + out;
        } else {
            return out + (SPACE ? " " : "") + SYMBOL;
        }
    }

    /**
     * Reload formatting settings from the given Bukkit FileConfiguration.
     * Expected keys under economy.format.*
     */
    public static void reloadFromConfig(FileConfiguration cfg) {
        if (cfg == null) return;
        try {
            String sym = cfg.getString("economy.format.symbol", SYMBOL);
            String position = cfg.getString("economy.format.position", PREFIX ? "prefix" : "suffix");
            boolean space = cfg.getBoolean("economy.format.space", SPACE);
            int decimals = cfg.getInt("economy.format.decimals", DECIMALS);
            boolean grouping = cfg.getBoolean("economy.format.use_grouping", GROUPING);
            String gsepStr = cfg.getString("economy.format.grouping_separator", String.valueOf(GROUPING_SEP));
            String dsepStr = cfg.getString("economy.format.decimal_separator", String.valueOf(DECIMAL_SEP));

            boolean abbr = cfg.getBoolean("economy.format.abbreviate.enabled", ABBR_ENABLED);
            int abbrDec = cfg.getInt("economy.format.abbreviate.decimals", ABBR_DECIMALS);

            // Apply
            SYMBOL = (sym != null) ? sym : SYMBOL;
            String pos = position != null ? position.toLowerCase(Locale.ROOT) : "prefix";
            PREFIX = !pos.equals("suffix");
            SPACE = space;
            DECIMALS = Math.max(0, decimals);
            GROUPING = grouping;
            GROUPING_SEP = (gsepStr != null && !gsepStr.isEmpty()) ? gsepStr.charAt(0) : GROUPING_SEP;
            DECIMAL_SEP = (dsepStr != null && !dsepStr.isEmpty()) ? dsepStr.charAt(0) : DECIMAL_SEP;

            ABBR_ENABLED = abbr;
            ABBR_DECIMALS = Math.max(0, abbrDec);

            // Rebuild thread-local formatter to pick up new settings
            NUMBER_FMT = ThreadLocal.withInitial(EconomyFormat::createNumberFormat);
        } catch (Throwable ignored) {
            // Keep previous settings on error
        }
    }

    private static DecimalFormat createNumberFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(GROUPING_SEP);
        symbols.setDecimalSeparator(DECIMAL_SEP);
        StringBuilder pattern = new StringBuilder(GROUPING ? "#,##0" : "0");
        if (DECIMALS > 0) {
            pattern.append('.');
            for (int i = 0; i < DECIMALS; i++) pattern.append('0');
        }
        DecimalFormat df = new DecimalFormat(pattern.toString(), symbols);
        df.setGroupingUsed(GROUPING);
        return df;
    }
}
