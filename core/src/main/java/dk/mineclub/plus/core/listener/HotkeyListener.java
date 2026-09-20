package dk.mineclub.plus.core.listener;

import dk.mineclub.plus.core.MineClubPlusAddon;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;
import net.labymod.api.event.client.input.KeyEvent.State;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the dashboard on the configured key (V by default).
 *
 * <p>The key is read from the configuration on every press rather than cached, so rebinding takes
 * effect immediately. Nothing happens while a screen is open, so typing V in chat or in a sign
 * never opens the window.
 *
 * <p>The key also works off MineClub. The window then states that there is nothing to fetch and
 * offers to connect, which is more use than a key that does nothing.
 */
public class HotkeyListener {

  private final MineClubPlusAddon addon;

  public HotkeyListener(@NotNull MineClubPlusAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent event) {
    if (event.state() != State.PRESS) {
      return;
    }

    if (!this.addon.configuration().enabled().get()) {
      return;
    }

    Key configured = this.addon.configuration().openMenuKey().get();
    if (configured == null || configured == Key.NONE || !configured.equals(event.key())) {
      return;
    }

    if (!this.addon.labyAPI().minecraft().isIngame()) {
      return;
    }

    if (this.addon.labyAPI().minecraft().minecraftWindow().isScreenOpened()) {
      return;
    }

    this.addon.openMenu();
  }
}
