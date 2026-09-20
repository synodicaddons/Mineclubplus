package dk.mineclub.plus.core.util;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * Number and time formatting for the UI. MineClub is a Danish server, so grouping and decimal
 * separators follow da-DK ("1.248.532", "0,02" and "+6,2 %").
 */
public final class Formats {

  private static final Locale LOCALE = Locale.forLanguageTag("da-DK");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
      .withLocale(LOCALE)
      .withZone(ZoneId.systemDefault());

  private static final ThreadLocal<NumberFormat> WHOLE = ThreadLocal.withInitial(() -> {
    NumberFormat format = NumberFormat.getIntegerInstance(LOCALE);
    format.setGroupingUsed(true);
    return format;
  });

  /**
   * Amounts with however many decimals they carry.
   *
   * <p>Coins are not always whole -- a shop trade can come to 0.02 -- and a rounded amount would
   * read as 0, as if nothing had happened. Whole numbers get no decimals, so 2,550 stays 2,550.
   */
  private static final ThreadLocal<NumberFormat> MONEY = ThreadLocal.withInitial(() -> {
    NumberFormat format = NumberFormat.getNumberInstance(LOCALE);
    format.setGroupingUsed(true);
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(4);
    return format;
  });

  private static final ThreadLocal<NumberFormat> DECIMAL = ThreadLocal.withInitial(() -> {
    NumberFormat format = NumberFormat.getNumberInstance(LOCALE);
    format.setMinimumFractionDigits(1);
    format.setMaximumFractionDigits(1);
    return format;
  });

  /**
   * Danish short scale. {@code DECIMAL} keeps one fraction digit, so anything from this value up
   * would print as 1000,0 and belongs in the next unit.
   */
  private static final String[] COMPACT_UNITS = {" k", " mio.", " mia."};
  private static final double ROUNDS_UP_AT = 999.95D;

  private Formats() {
  }

  public static @NotNull String number(double value) {
    return MONEY.get().format(value);
  }

  public static @NotNull String number(long value) {
    return WHOLE.get().format(value);
  }

  /**
   * A short form for boxes that cannot fit a grouped number.
   *
   * <p>Below ten thousand the exact number still fits, so it is kept; above that the value is
   * shortened to one decimal in the Danish scale -- k, mio., mia. -- which keeps three significant
   * digits and never runs wider than seven characters.
   */
  public static @NotNull String compact(double value) {
    double magnitude = Math.abs(value);
    if (magnitude < 10_000.0D) {
      return number(value);
    }

    // Step up while the rounded value would reach four digits, so 999.999 reads as 1,0 mio.
    // rather than 1000,0 k.
    double scaled = value / 1_000.0D;
    int unit = 0;
    while (unit < COMPACT_UNITS.length - 1 && Math.abs(scaled) >= ROUNDS_UP_AT) {
      scaled /= 1_000.0D;
      unit++;
    }

    return DECIMAL.get().format(scaled) + COMPACT_UNITS[unit];
  }

  public static @NotNull String signed(double value) {
    return (value >= 0.0D ? "+" : "-") + number(Math.abs(value));
  }

  public static @NotNull String percent(double value) {
    return (value >= 0.0D ? "+" : "-") + DECIMAL.get().format(Math.abs(value)) + " %";
  }

  public static @NotNull String decimal(double value) {
    return DECIMAL.get().format(value);
  }

  public static @NotNull String time(long epochMillis) {
    return TIME.format(Instant.ofEpochMilli(epochMillis));
  }

  /**
   * @param minutes total play time in minutes
   * @return a compact "612 t 30 m" style label
   */
  public static @NotNull String playtime(long minutes) {
    long days = minutes / 1440L;
    long hours = minutes % 1440L / 60L;
    long remainder = minutes % 60L;

    // Hours roll into days: "61 h" says little where "2 d 13 h" says it at a glance.
    if (days > 0L) {
      return number(days) + " d " + hours + " t " + remainder + " m";
    }

    if (hours > 0L) {
      return number(hours) + " t " + remainder + " m";
    }

    return remainder + " m";
  }

}
