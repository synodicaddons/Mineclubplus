package dk.mineclub.plus.core.listener;

import dk.mineclub.plus.core.MineClubPlusAddon;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import net.labymod.api.event.client.network.server.SubServerSwitchEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps the cached snapshot honest across connections.
 *
 * <p>Joining MineClub or hopping to another sub-server invalidates the cache so the next time the
 * window opens it fetches once; it does not fetch here, because the player may never open the
 * window at all.
 */
public class ServerListener {

  private final MineClubPlusAddon addon;

  public ServerListener(@NotNull MineClubPlusAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onServerJoin(ServerJoinEvent event) {
    this.addon.api().snapshot().invalidate();
  }

  @Subscribe
  public void onSubServerSwitch(SubServerSwitchEvent event) {
    this.addon.api().snapshot().invalidate();
  }

  @Subscribe
  public void onServerDisconnect(ServerDisconnectEvent event) {
    this.addon.api().snapshot().invalidate();
  }
}
