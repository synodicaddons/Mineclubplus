package dk.mineclub.plus.core;

import dk.mineclub.plus.core.api.MineClubApi;
import dk.mineclub.plus.core.command.MineClubPlusCommand;
import dk.mineclub.plus.core.listener.HotkeyListener;
import dk.mineclub.plus.core.util.Translations;
import dk.mineclub.plus.core.listener.QuickTakeListener;
import dk.mineclub.plus.core.listener.ServerListener;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.MineClubPlusNavigationElement;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.network.server.ServerData;
import net.labymod.api.configuration.exception.ConfigurationSaveException;
import net.labymod.api.models.addon.annotation.AddonMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MineClub+ -- a dashboard for the MineClub server: transporter, economy and statistics in one
 * window, opened with a single key.
 */
@AddonMain
public class MineClubPlusAddon extends LabyAddon<MineClubPlusConfiguration> {

  /**
   * Host fragment used to recognise the server. Matching on the fragment keeps sub-domains such as
   * {@code play.mineclub.dk} working.
   */
  private static final String SERVER_HOST_FRAGMENT = "mineclub";

  /** The address shown to a player who is not on the network. */
  public static final String SERVER_ADDRESS = "mc.mineclub.dk";

  private static MineClubPlusAddon instance;

  private MineClubApi api;

  @Override
  protected void load() {
    instance = this;
  }

  /**
   * The running addon.
   *
   * <p>LabyMod builds the configuration without handing it the addon, so the settings button that
   * opens the dashboard has no other way to reach it.
   */
  static @NotNull MineClubPlusAddon get() {
    return instance;
  }

  @Override
  protected void enable() {
    this.registerSettingCategory();

    this.api = new MineClubApi(this);

    // Translations have to be in place before any part of the window is built.
    Translations.load();
    Translations.language(this.configuration().language().get());

    this.registerListener(new HotkeyListener(this));
    this.registerListener(new QuickTakeListener(this));
    this.registerListener(new ServerListener(this));
    this.registerCommand(new MineClubPlusCommand(this));

    // Adds MineClub+ to LabyMod's navigation rail, so the window is reachable without the hotkey.
    this.labyAPI().navigationService().register(
        MineClubPlusNavigationElement.ID,
        new MineClubPlusNavigationElement(this)
    );

    this.logger().info("MineClub+ enabled");
  }

  @Override
  protected Class<MineClubPlusConfiguration> configurationClass() {
    return MineClubPlusConfiguration.class;
  }

  public @NotNull MineClubApi api() {
    return this.api;
  }

  /**
   * Opens the dashboard on the next tick, which is the only safe moment to swap the screen from a
   * key or command handler.
   */
  public void openMenu() {
    this.labyAPI().minecraft().executeNextTick(
        () -> this.labyAPI().minecraft().minecraftWindow()
            .displayScreen(new MineClubPlusActivity(this))
    );
  }

  /**
   * @return the name the addon identifies itself by, in the user agent and towards the API
   */
  public String userAgent() {
    return "MineClubPlus/" + this.addonInfo().getVersion();
  }

  /**
   * @return true when the client is connected to a MineClub address
   */
  public boolean isOnMineClub() {
    ServerData serverData = this.labyAPI().serverController().getCurrentServerData();
    if (serverData == null) {
      return false;
    }

    return containsIgnoreCase(serverData.address().getHost(), SERVER_HOST_FRAGMENT);
  }

  /**
   * Case-insensitive substring test that allocates nothing.
   *
   * <p>{@code toLowerCase().contains(...)} builds a new string on every call, and the open window
   * asks this once a tick to notice the player joining or leaving MineClub. Addon store guideline
   * 1.1 rules out per-tick allocation, so the comparison walks the host in place instead.
   */
  private static boolean containsIgnoreCase(@Nullable String host, @NotNull String fragment) {
    if (host == null) {
      return false;
    }

    int limit = host.length() - fragment.length();
    for (int index = 0; index <= limit; index++) {
      if (host.regionMatches(true, index, fragment, 0, fragment.length())) {
        return true;
      }
    }

    return false;
  }

  /**
   * @return true when the addon is running from {@code :game-runner:client_*} rather than from an
   *     installed jar
   */
  public boolean isDevClient() {
    return this.labyAPI().labyModLoader().isAddonDevelopmentEnvironment();
  }

  /**
   * Whether the window has nothing real to show.
   *
   * <p>Off the network every figure would be sample data, so a released client says so instead of
   * dressing the numbers up as real. The dev client is exempt -- that is the whole point of the
   * sample data, and {@code :game-runner:client_*} never joins MineClub.
   *
   * @return true when a released client is not connected to MineClub
   */
  public boolean isOffNetwork() {
    return !this.isDevClient() && !this.isOnMineClub();
  }

  /**
   * Persists the configuration after an in-window settings change. A failed save must not take the
   * window down with it, so it is logged instead of thrown.
   */
  public void save() {
    try {
      this.saveConfiguration();
    } catch (ConfigurationSaveException exception) {
      this.logger().error("Could not save the MineClub+ configuration", exception);
    }
  }
}
