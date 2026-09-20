package dk.mineclub.plus.core.ui.widget;

import dk.mineclub.plus.core.util.ItemColors;
import dk.mineclub.plus.core.util.ItemVersions;
import dk.mineclub.plus.core.util.Translations;
import java.util.Locale;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.loader.MinecraftVersion;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.minecraft.ItemStackWidget;
import net.labymod.api.client.world.item.Item;
import net.labymod.api.client.world.item.ItemStack;
import net.labymod.api.client.world.item.ItemStackFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders a transporter entry as the actual Minecraft item.
 *
 * <p>The server sends Bukkit material names, which map onto vanilla item ids one to one once they
 * are lower cased, so every vanilla block and item is covered. Blocks come out as Minecraft draws
 * them in an inventory -- the isometric three quarter view -- and items as their flat sprite.
 *
 * <p>Anything the running Minecraft version does not know -- a modded item, or one added in a
 * later version than the client -- falls back to the coloured chip, so a single unknown id never
 * leaves a hole in the grid.
 *
 * <p>The widget box must be exactly 16 by 16: the theme renderer draws the item at that size
 * around the widget centre and does not scale it to a smaller box.
 */
public final class ItemIcons {

  private static final String VANILLA = "minecraft";

  /** Drawn over the colour chip when a later version is what holds the item. */
  private static final Icon LOCK = Icon.texture(
      ResourceLocation.create("mineclubplus", "textures/lock.png")
  );

  private ItemIcons() {
  }

  /**
   * @param itemId the material id, in either case
   * @param ids    stylesheet ids for the resulting widget
   */
  public static @NotNull Widget of(@NotNull String itemId, String... ids) {
    ItemStack stack = resolve(itemId);
    if (stack == null) {
      return missing(itemId, ids);
    }

    ItemStackWidget widget = new ItemStackWidget(stack);
    widget.decorate().set(false);
    for (String id : ids) {
      widget.addId(id);
    }

    return widget;
  }

  /**
   * The name Minecraft itself gives the item, in the player's language.
   *
   * @param itemId   the material id
   * @param fallback used when this version has no such item, so a server item the client does not
   *                 know still gets a label
   */
  public static @NotNull Component nameOf(@NotNull String itemId, @NotNull String fallback) {
    ItemStack stack = resolve(itemId);
    if (stack == null) {
      return Component.text(fallback);
    }

    Component name = stack.getDisplayName();
    return name == null ? Component.text(fallback) : name;
  }

  /**
   * The stand-in for an item the running version does not have.
   *
   * <p>The addon runs down to 1.8.9 while the server runs a recent version, so an old client is
   * told about blocks it has no texture for. A coloured chip on its own leaves the player
   * guessing; when LabyMod knows the item from a later version, the chip carries a padlock and
   * the tooltip names the version that has it.
   */
  private static @NotNull Widget missing(String itemId, String... ids) {
    DivWidget swatch = Widgets.swatch(ItemColors.of(itemId), ids);

    MinecraftVersion required = ItemVersions.requiredBy(itemId);
    if (required == null) {
      return swatch;
    }

    swatch.addId("mcp-locked");
    swatch.addChild(Widgets.icon(LOCK, "mcp-lock"));
    swatch.setHoverComponent(Component.text(Translations.get(
        "mineclubplus.state.locked",
        ItemVersions.current(),
        required.getFormattedVersion()
    )));

    return swatch;
  }

  /**
   * @return the vanilla stack, or {@code null} when this version has no such item
   */
  private static @Nullable ItemStack resolve(String itemId) {
    String path = itemId.toLowerCase(Locale.ROOT).trim();
    if (path.isEmpty()) {
      return null;
    }

    // A namespaced id from the server wins over the vanilla default.
    String namespace = VANILLA;
    int colon = path.indexOf(':');
    if (colon > 0) {
      namespace = path.substring(0, colon);
      path = path.substring(colon + 1);
    }

    try {
      ItemStackFactory factory = Laby.references().itemStackFactory();
      ItemStack stack = factory.create(namespace, path);
      if (stack == null) {
        return null;
      }

      // A factory may hand back a stack whose item resolves later, so only a positive "this is
      // air" counts as unknown; anything undecided is left to the renderer.
      Item item = stack.getAsItem();
      return item != null && item.isAir() ? null : stack;
    } catch (RuntimeException exception) {
      // An unknown id throws on some versions rather than returning air.
      return null;
    }
  }
}
