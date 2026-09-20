package dk.mineclub.plus.core.ui;

import dk.mineclub.plus.core.util.Translations;
import dk.mineclub.plus.core.MineClubPlusAddon;
import net.labymod.api.Textures.SpriteCommon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.navigation.elements.ScreenFactoryNavigationElement;
import org.jetbrains.annotations.NotNull;

/**
 * Puts MineClub+ in LabyMod's own navigation rail, next to Settings and Multiplayer.
 *
 * <p>It is a screen factory rather than a fixed screen so every visit builds a fresh window and
 * picks up the current snapshot instead of reusing a stale one.
 */
public class MineClubPlusNavigationElement extends ScreenFactoryNavigationElement {

  /**
   * Id used both for the registry and as the widget id the theme styles.
   */
  public static final String ID = "mineclubplus";

  private final MineClubPlusAddon addon;

  public MineClubPlusNavigationElement(@NotNull MineClubPlusAddon addon) {
    super(() -> new MineClubPlusActivity(addon));

    this.addon = addon;
  }

  /**
   * Switching the addon off in LabyMod's addon list has to take the rail entry with it -- the
   * store guidelines require the addon to be completely disableable, and an entry that still
   * opens the window would not be.
   */
  @Override
  public boolean isVisible() {
    return this.addon.configuration().enabled().get();
  }

  @Override
  public String getWidgetId() {
    return ID;
  }

  @Override
  public Component getDisplayName() {
    return Component.text(Translations.get("mineclubplus.navigation.name"));
  }

  @Override
  public Icon getIcon() {
    return SpriteCommon.LARGE_DOTS;
  }
}
