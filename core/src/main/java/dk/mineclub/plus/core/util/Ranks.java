package dk.mineclub.plus.core.util;

import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The name and colour of a rank.
 *
 * <p>The names and colours are the ones the panel uses ({@code --role-*} in its dark theme), so a
 * developer is blue and a supporter yellow in both places. The colour itself lives in the
 * stylesheet; this only maps a rank onto the id the stylesheet knows, so a theme can restyle them
 * without touching code.
 *
 * <p>An unknown rank falls back to the addon's own yellow, which is how every rank looked
 * before, and keeps the name the server gave it.
 */
public final class Ranks {

  /**
   * The rank as LuckPerms names it, against the suffix of the stylesheet id.
   */
  private static final Map<String, String> STYLES = Map.of(
      "admin", "admin",
      "udvikler", "udvikler",
      "mod", "mod",
      "supporter", "supporter",
      "bygger", "bygger",
      "vip", "vip"
  );

  private static final String FALLBACK = "default";

  private Ranks() {
  }

  /**
   * The rank as it is shown, in the language the player picked.
   *
   * <p>The API sends the LuckPerms group name, which is Danish ({@code udvikler}) and lower case.
   * Neither belongs on screen, so a known rank is translated and every rank is upper cased, which
   * is how the chip and the player card have always read.
   *
   * @param rank the rank as the API spells it
   * @return the translated name, or the server's own spelling for a rank the addon does not know
   */
  public static @NotNull String displayName(@Nullable String rank) {
    if (rank == null) {
      return "";
    }

    String trimmed = rank.trim();
    if (trimmed.isEmpty()) {
      return "";
    }

    // Keyed by the group name itself and not by the style id: an unknown rank maps onto the
    // default style, and looking the name up that way would call a Media rank a Player.
    String translated = Translations.find(
        "mineclubplus.rank." + trimmed.toLowerCase(Locale.ROOT)
    );

    return (translated == null ? trimmed : translated).toUpperCase(Locale.ROOT);
  }

  /**
   * @param rank the rank as the API spells it, in any case
   * @return the stylesheet id carrying the rank's text colour and its tinted background
   */
  public static @NotNull String styleId(@Nullable String rank) {
    return "mcp-rank-" + suffix(rank);
  }

  /**
   * @return the rank reduced to one of the known ids, or {@code default} for anything else --
   *     the same id the stylesheet and the language files are keyed by
   */
  private static String suffix(@Nullable String rank) {
    if (rank == null) {
      return FALLBACK;
    }

    String normalized = rank.trim().toLowerCase(Locale.ROOT);
    return STYLES.getOrDefault(normalized, FALLBACK);
  }
}
