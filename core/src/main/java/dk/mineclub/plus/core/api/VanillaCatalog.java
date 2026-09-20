package dk.mineclub.plus.core.api;

import dk.mineclub.plus.core.api.model.ItemCategory;
import dk.mineclub.plus.core.api.model.TransporterItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.labymod.api.client.world.item.VanillaItem;
import net.labymod.api.client.world.item.VanillaItems;
import org.jetbrains.annotations.NotNull;

/**
 * Every vanilla block and item the running Minecraft version has, turned into transporter entries.
 *
 * <p>This is what backs the sample transporter: rather than a hand written handful, the grid holds
 * the whole catalogue, so the icons, the search and the category chips are exercised against the
 * real thing. Amounts and prices are derived from the id, so they are stable between refreshes
 * instead of jumping every thirty seconds.
 */
public final class VanillaCatalog {

  /**
   * Checked in this order, because an id matches several of them -- {@code iron_pickaxe} is a tool
   * before it is anything to do with mining.
   */
  private static final String[][] CATEGORY_HINTS = {
      {"tools", "sword", "pickaxe", "_axe", "shovel", "_hoe", "helmet", "chestplate", "leggings",
          "boots", "bow", "shield", "trident", "elytra", "fishing_rod", "shears", "flint_and_steel",
          "bucket", "compass", "clock", "spyglass", "brush", "mace"},
      {"machinery", "piston", "hopper", "dispenser", "dropper", "observer", "comparator",
          "repeater", "rail", "furnace", "smoker", "blast_", "crafter", "chest", "barrel",
          "shulker", "anvil", "beacon", "conduit", "lectern", "loom", "grindstone", "stonecutter",
          "cauldron", "brewing", "enchanting", "dispenser", "lever", "button", "pressure_plate",
          "redstone_torch", "tripwire", "target", "note_block", "jukebox"},
      {"mobs", "spawn_egg", "_head", "_skull", "bone", "rotten_flesh", "string", "spider_eye",
          "leather", "feather", "beef", "porkchop", "mutton", "chicken", "rabbit", "_cod",
          "salmon", "shell", "scute", "ink_sac", "ender_pearl", "blaze_rod", "slime", "phantom",
          "wither", "dragon", "ghast", "gunpowder", "prismarine", "sponge", "honeycomb"},
      {"farming", "seeds", "wheat", "carrot", "potato", "beetroot", "melon", "pumpkin", "sugar",
          "cactus", "bamboo", "kelp", "berries", "apple", "bread", "cake", "cookie", "honey",
          "_egg", "milk", "mushroom", "flower", "tulip", "orchid", "dandelion", "poppy", "sapling",
          "wart", "cocoa", "hay", "compost", "dirt", "farmland", "moss", "vine", "grass"},
      {"wood", "_log", "_wood", "planks", "leaves", "_stem", "hyphae", "stick", "fence",
          "trapdoor", "_door", "sign", "boat", "bookshelf", "ladder", "scaffolding"},
      {"mines", "_ore", "ingot", "raw_", "deepslate", "stone", "granite", "andesite", "diorite",
          "cobble", "gravel", "sand", "coal", "diamond", "emerald", "lapis", "redstone", "quartz",
          "netherite", "gold", "iron", "copper", "amethyst", "obsidian", "nugget", "scrap",
          "tuff", "calcite", "basalt", "blackstone", "terracotta", "concrete", "brick"},
  };

  private VanillaCatalog() {
  }

  /**
   * @return one entry per item available in this version, sorted by name
   */
  public static @NotNull List<TransporterItem> items() {
    List<VanillaItem> available = VanillaItems.findItems(VanillaItem::isAvailable);

    List<TransporterItem> items = new ArrayList<>(available.size());
    for (VanillaItem item : available) {
      String id = item.identifier().getPath();

      // Three independent mixes of the id: drawing amount and price from the same number made
      // them rise and fall together, so sorting by value came out sorted by amount as well.
      items.add(new TransporterItem(
          id,
          displayName(id),
          categoryOf(id),
          1L + mix(id, 0x9E3779B1) % 2304L,
          1.0D + mix(id, 0x85EBCA77) % 500L,
          (mix(id, 0xC2B2AE3D) % 401 - 200) / 10.0D
      ));
    }

    items.sort((left, right) -> left.displayName().compareToIgnoreCase(right.displayName()));
    return items;
  }

  /**
   * A stable, well spread number for an id. The multiply-and-xor is the finaliser from MurmurHash3,
   * which decorrelates the low bits that {@link String#hashCode()} leaves in step.
   */
  private static int mix(String id, int salt) {
    int hash = id.hashCode() ^ salt;
    hash ^= hash >>> 16;
    hash *= 0x7FEB352D;
    hash ^= hash >>> 15;
    hash *= 0x846CA68B;
    hash ^= hash >>> 16;

    return hash & 0x7FFFFFFF;
  }

  /**
   * @param id a vanilla item path, for example {@code netherite_ingot}
   * @return the same id as a readable label, {@code Netherite ingot}
   */
  public static @NotNull String displayName(@NotNull String id) {
    String spaced = id.replace('_', ' ');
    if (spaced.isEmpty()) {
      return id;
    }

    return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
  }

  public static @NotNull ItemCategory categoryOf(@NotNull String id) {
    String path = id.toLowerCase(Locale.ROOT);

    for (String[] hints : CATEGORY_HINTS) {
      for (int index = 1; index < hints.length; index++) {
        if (path.contains(hints[index])) {
          return ItemCategory.of(hints[0]);
        }
      }
    }

    return ItemCategory.OTHERS;
  }
}
