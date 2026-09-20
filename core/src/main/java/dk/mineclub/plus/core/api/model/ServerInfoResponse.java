package dk.mineclub.plus.core.api.model;

/**
 * Maps {@code GET /v2/info/minecraft}, which needs no token.
 *
 * <p>The endpoint also reports how much capacity the wings have left; that is a hosting concern,
 * so only the player count is read here.
 */
public class ServerInfoResponse {

  private int onlinePlayers;

  public int onlinePlayers() {
    return this.onlinePlayers;
  }
}
