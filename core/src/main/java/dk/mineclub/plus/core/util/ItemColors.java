package dk.mineclub.plus.core.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Stable swatch colour per item, used for the square chips in the transporter grid.
 *
 * <p>Known MineClub items get a hand-picked colour so the grid reads like the in-game item;
 * anything else derives a deterministic hue from its id, which keeps new server items looking
 * intentional without a texture pack.
 */
public final class ItemColors {

  private static final Map<String, Integer> KNOWN = new HashMap<>();
  private static final int ALPHA = 0xFF000000;

  static {
    KNOWN.put("diamond", 0x4AEDD9);
    KNOWN.put("netherite_ingot", 0x6B5A53);
    KNOWN.put("netherite_scrap", 0x7A6258);
    KNOWN.put("gold_ingot", 0xF2C744);
    KNOWN.put("iron_ingot", 0xD8D8D8);
    KNOWN.put("emerald", 0x3FCF8E);
    KNOWN.put("lapis_lazuli", 0x3A62C4);
    KNOWN.put("redstone", 0xE04B4B);
    KNOWN.put("coal", 0x2B2B2B);
    KNOWN.put("copper_ingot", 0xC87A4B);
    KNOWN.put("quartz", 0xE8E2D8);
    KNOWN.put("stone", 0x8C8C8C);
    KNOWN.put("cobblestone", 0x7E7E7E);
    KNOWN.put("obsidian", 0x2A2140);
    KNOWN.put("oak_log", 0x9C7A4A);
    KNOWN.put("nether_brick", 0x503038);
    KNOWN.put("prismarine", 0x76B7A8);
    KNOWN.put("shulker_box", 0xA269A2);
    KNOWN.put("ender_pearl", 0x1C6F63);
    KNOWN.put("elytra", 0xC9C3D6);
    KNOWN.put("golden_apple", 0xF5C542);
    KNOWN.put("cooked_beef", 0xA6553C);
    KNOWN.put("cactus", 0x4C8F3C);
  }

  private ItemColors() {
  }

  public static int of(String itemId) {
    if (itemId == null || itemId.isEmpty()) {
      return ALPHA | 0x8A9199;
    }

    Integer known = KNOWN.get(itemId.toLowerCase(Locale.ROOT));
    if (known != null) {
      return ALPHA | known;
    }

    return ALPHA | hueFrom(itemId);
  }

  private static final float SATURATION = 0.42F;
  private static final float BRIGHTNESS = 0.78F;

  /**
   * Maps the id onto a fixed saturation/brightness ring so generated colours stay in the same
   * visual family as the hand-picked ones.
   */
  private static int hueFrom(String itemId) {
    int degrees = Math.abs(itemId.hashCode() % 360);
    float sector = degrees / 60.0F;
    float chroma = BRIGHTNESS * SATURATION;
    float second = chroma * (1.0F - Math.abs(sector % 2.0F - 1.0F));
    float match = BRIGHTNESS - chroma;

    float red;
    float green;
    float blue;
    switch ((int) sector) {
      case 0 -> {
        red = chroma;
        green = second;
        blue = 0.0F;
      }
      case 1 -> {
        red = second;
        green = chroma;
        blue = 0.0F;
      }
      case 2 -> {
        red = 0.0F;
        green = chroma;
        blue = second;
      }
      case 3 -> {
        red = 0.0F;
        green = second;
        blue = chroma;
      }
      case 4 -> {
        red = second;
        green = 0.0F;
        blue = chroma;
      }
      default -> {
        red = chroma;
        green = 0.0F;
        blue = second;
      }
    }

    return channel(red + match) << 16 | channel(green + match) << 8 | channel(blue + match);
  }

  private static int channel(float value) {
    return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
  }
}
