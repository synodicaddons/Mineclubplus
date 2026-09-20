package dk.mineclub.plus.core.api.model;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * The transporter categories, matching {@code transporter.yml} in the lobby plugin.
 *
 * <p>The ids are the category names the plugin uses, so a value straight from the server maps
 * without a lookup table. Anything the plugin adds later lands in {@link #OTHERS} until it is
 * added here.
 */
public enum ItemCategory {

  ALL("all"),
  MINES("mines"),
  FARMING("farming"),
  WOOD("wood"),
  MOBS("mobs"),
  TOOLS("tools"),
  MACHINERY("machinery"),
  OTHERS("others");

  private final String id;

  ItemCategory(String id) {
    this.id = id;
  }

  public @NotNull String id() {
    return this.id;
  }

  public @NotNull String translationKey() {
    return "mineclubplus.category." + this.id;
  }

  public static @NotNull ItemCategory of(String raw) {
    if (raw == null || raw.isEmpty()) {
      return OTHERS;
    }

    String normalized = raw.toLowerCase(Locale.ROOT);
    for (ItemCategory category : values()) {
      if (category.id.equals(normalized)) {
        return category;
      }
    }

    return OTHERS;
  }
}
