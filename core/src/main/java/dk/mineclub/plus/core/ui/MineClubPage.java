package dk.mineclub.plus.core.ui;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * The entries of the left navigation rail, in display order.
 *
 * <p>Each entry carries a line icon of its own from {@code textures/icons/<id>.png} -- drawn in
 * the Lucide manner, white on transparency so the stylesheet tints it, and authored at twice the
 * display size so the stroke survives any GUI scale.
 */
public enum MineClubPage {

  OVERVIEW("overview"),
  TRANSPORTER("transporter"),
  ECONOMY("economy"),
  SHOP("shop"),
  MATERIALS("materials"),
  LOGS("logs"),
  STATISTICS("statistics"),
  SETTINGS("settings");

  private static final String NAMESPACE = "mineclubplus";

  private final String id;
  private final Icon icon;

  MineClubPage(String id) {
    this.id = id;
    this.icon = Icon.texture(
        ResourceLocation.create(NAMESPACE, "textures/icons/" + id + ".png")
    );
  }

  public @NotNull String id() {
    return this.id;
  }

  public @NotNull Icon icon() {
    return this.icon;
  }

  public @NotNull String titleKey() {
    return "mineclubplus.page." + this.id + ".title";
  }

  public @NotNull String subtitleKey() {
    return "mineclubplus.page." + this.id + ".subtitle";
  }

  /**
   * @return the entries shown above the divider; {@link #SETTINGS} sits in the sidebar footer.
   */
  public static @NotNull MineClubPage[] mainEntries() {
    return new MineClubPage[]{
        OVERVIEW, TRANSPORTER, ECONOMY, SHOP, MATERIALS, LOGS, STATISTICS
    };
  }
}
