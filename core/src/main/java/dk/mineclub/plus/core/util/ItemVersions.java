package dk.mineclub.plus.core.util;

import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.world.item.VanillaItem;
import net.labymod.api.client.world.item.VanillaItems;
import net.labymod.api.loader.MinecraftVersion;
import net.labymod.api.loader.MinecraftVersions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Which Minecraft version an item needs.
 *
 * <p>The addon runs on every version LabyMod ships, down to 1.8.9, while the server it talks to
 * runs a recent one. An older client therefore gets told about copper, deepslate and amethyst it
 * has no texture for, and the transporter would show a coloured chip with no explanation.
 *
 * <p>LabyMod already knows the answer: every {@link VanillaItem} carries the versions it exists
 * in, so the oldest of those is the version the player would have to join on.
 */
public final class ItemVersions {

  private ItemVersions() {
  }

  /**
   * @param itemId the material id, in either case
   * @return the oldest version that has this item, or {@code null} when the running version
   *     already has it -- or when LabyMod does not know the id at all, in which case no version
   *     can honestly be named
   */
  public static @Nullable MinecraftVersion requiredBy(@NotNull String itemId) {
    // An id Minecraft would refuse must never reach ResourceLocation: it throws, and a throw
    // here takes the window down with it.
    ItemIds.ItemId id = ItemIds.parse(itemId);
    if (id == null) {
      return null;
    }

    VanillaItem item = VanillaItems.findItem(ResourceLocation.create(id.namespace(), id.path()));
    if (item == null || item.isAvailable()) {
      return null;
    }

    MinecraftVersion oldest = null;
    for (MinecraftVersion version : item.getVersions().values()) {
      if (oldest == null || version.isLowerThan(oldest)) {
        oldest = version;
      }
    }

    return oldest;
  }

  /** @return the version the client is running, as it is written in the launcher */
  public static @NotNull String current() {
    MinecraftVersion version = MinecraftVersions.current();
    return version == null ? "?" : version.getFormattedVersion();
  }
}
