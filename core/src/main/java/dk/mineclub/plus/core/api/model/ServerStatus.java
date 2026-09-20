package dk.mineclub.plus.core.api.model;

import org.jetbrains.annotations.NotNull;

/**
 * What the network reports about itself.
 *
 * <p>Only what {@code GET /v2/info/minecraft} and the player endpoint actually return. There is no
 * player cap, no TPS figure and no latency in either, so none are held here -- ping is client side
 * state and is read straight from the connection where it is shown.
 */
public class ServerStatus {

  private String host;
  private String world;
  private String gamemode;
  private int onlinePlayers;

  public ServerStatus() {
  }

  public ServerStatus(
      @NotNull String host,
      @NotNull String world,
      @NotNull String gamemode,
      int onlinePlayers
  ) {
    this.host = host;
    this.world = world;
    this.gamemode = gamemode;
    this.onlinePlayers = onlinePlayers;
  }

  public @NotNull String host() {
    return this.host == null ? "" : this.host;
  }

  public @NotNull String world() {
    return this.world == null ? "" : this.world;
  }

  public @NotNull String gamemode() {
    return this.gamemode == null ? "" : this.gamemode;
  }

  public int onlinePlayers() {
    return this.onlinePlayers;
  }
}
