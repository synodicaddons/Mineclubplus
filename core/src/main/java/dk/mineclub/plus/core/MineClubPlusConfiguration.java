package dk.mineclub.plus.core;

import dk.mineclub.plus.core.config.AvatarStyle;
import dk.mineclub.plus.core.config.Language;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.activity.Activity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.KeybindWidget.KeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.dropdown.DropdownWidget.DropdownSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.annotation.SettingSection;
import net.labymod.api.client.gui.screen.widget.widgets.activity.settings.ActivitySettingWidget.ActivitySetting;
import net.labymod.api.util.MethodOrder;

@ConfigName("settings")
public class MineClubPlusConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  /**
   * Opens the dashboard straight from the addon settings, for players who would rather click than
   * remember the hotkey.
   */
  @MethodOrder(after = "enabled")
  @ActivitySetting
  public Activity openDashboard() {
    return new MineClubPlusActivity(MineClubPlusAddon.get());
  }

  @SettingSection("menu")
  @KeyBindSetting
  private final ConfigProperty<Key> openMenuKey = new ConfigProperty<>(Key.V);

  /**
   * The panel behind the window.
   *
   * <p>Turned off, the interface sits directly on the game rather than on a dimmed plate. Some
   * players would rather keep an eye on what happens behind it.
   */
  @SwitchSetting
  private final ConfigProperty<Boolean> windowBackground = new ConfigProperty<>(true);

  @DropdownSetting
  private final ConfigProperty<AvatarStyle> avatarStyle = new ConfigProperty<>(AvatarStyle.HEAD);

  /**
   * The language of the window. English by default whatever the client runs in, so every player
   * sees the same thing the first time; Danish is a deliberate choice.
   */
  @DropdownSetting
  private final ConfigProperty<Language> language = new ConfigProperty<>(Language.ENGLISH);

  @SettingSection("data")
  @SwitchSetting
  private final ConfigProperty<Boolean> refreshWhileOpen = new ConfigProperty<>(false);

  @SliderSetting(min = 15, max = 300, steps = 5)
  private final ConfigProperty<Integer> refreshIntervalSeconds = new ConfigProperty<>(60);

  @SettingSection("transporter")
  @SwitchSetting
  private final ConfigProperty<Boolean> confirmTransporterActions = new ConfigProperty<>(true);

  @SliderSetting(min = 8, max = 2304, steps = 8)
  private final ConfigProperty<Integer> quickAmount = new ConfigProperty<>(64);

  /**
   * The key that takes one specific item out without opening the window.
   *
   * <p>Unbound by default: a shortcut that moves items out of the transporter should be one the
   * player chose, not one that lands on a key already used for something else.
   */
  @SettingSection("quickTake")
  @KeyBindSetting
  private final ConfigProperty<Key> takeKey = new ConfigProperty<>(Key.NONE);

  /**
   * The material the shortcut takes. Easiest set from the button on the item itself in the
   * transporter, but it can be typed here: the same name the command uses, for example
   * {@code DIAMOND_BLOCK}.
   */
  @TextFieldSetting(maxLength = 64)
  private final ConfigProperty<String> takeKeyItem = new ConfigProperty<>("");

  @SliderSetting(min = 1, max = 2304, steps = 1)
  private final ConfigProperty<Integer> takeKeyAmount = new ConfigProperty<>(64);

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<Key> openMenuKey() {
    return this.openMenuKey;
  }

  public ConfigProperty<Boolean> windowBackground() {
    return this.windowBackground;
  }

  public ConfigProperty<AvatarStyle> avatarStyle() {
    return this.avatarStyle;
  }

  public ConfigProperty<Language> language() {
    return this.language;
  }

  public ConfigProperty<Boolean> refreshWhileOpen() {
    return this.refreshWhileOpen;
  }

  public ConfigProperty<Integer> refreshIntervalSeconds() {
    return this.refreshIntervalSeconds;
  }

  public ConfigProperty<Boolean> confirmTransporterActions() {
    return this.confirmTransporterActions;
  }

  public ConfigProperty<Integer> quickAmount() {
    return this.quickAmount;
  }

  public ConfigProperty<Key> takeKey() {
    return this.takeKey;
  }

  public ConfigProperty<String> takeKeyItem() {
    return this.takeKeyItem;
  }

  public ConfigProperty<Integer> takeKeyAmount() {
    return this.takeKeyAmount;
  }
}
