package dk.mineclub.plus.core.listener;

import dk.mineclub.plus.core.MineClubPlusAddon;
import dk.mineclub.plus.core.service.ServerCommands;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;
import net.labymod.api.event.client.input.KeyEvent.State;
import org.jetbrains.annotations.NotNull;

/**
 * Takes one specific item out of the transporter on a key press, without opening the window.
 *
 * <p>Material and amount come from the settings, and are easiest set from the button on the item
 * itself in the transporter. There is no default key: a shortcut that moves items should be one
 * the player chose.
 *
 * <p>Same guard as the menu key: never while a screen is open, so it cannot fire while typing in
 * chat.
 */
public class QuickTakeListener {

  private final MineClubPlusAddon addon;

  public QuickTakeListener(@NotNull MineClubPlusAddon addon) {
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

    Key configured = this.addon.configuration().takeKey().get();
    if (configured == null || configured == Key.NONE || !configured.equals(event.key())) {
      return;
    }

    String item = this.addon.configuration().takeKeyItem().get();
    if (item == null || item.isBlank()) {
      return;
    }

    if (!this.addon.labyAPI().minecraft().isIngame()) {
      return;
    }

    if (this.addon.labyAPI().minecraft().minecraftWindow().isScreenOpened()) {
      return;
    }

    // The command only exists on MineClub, so it must not be sent to another server.
    if (!this.addon.isOnMineClub()) {
      return;
    }

    int amount = Math.max(1, this.addon.configuration().takeKeyAmount().get());
    ServerCommands.takeOut(item.trim(), amount);
  }
}
