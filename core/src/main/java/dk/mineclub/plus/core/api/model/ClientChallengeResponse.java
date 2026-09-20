package dk.mineclub.plus.core.api.model;

import org.jetbrains.annotations.Nullable;

/**
 * The response from {@code POST /v2/client/auth/challenge}: the {@code serverId} to send to
 * Mojang as proof of who the player is.
 */
public class ClientChallengeResponse {

  private boolean success;
  private String serverId;
  private String username;
  private long expiresIn;

  public boolean success() {
    return this.success;
  }

  public @Nullable String serverId() {
    return this.serverId;
  }

  public @Nullable String username() {
    return this.username;
  }

  /**
   * @return seconds left to answer the challenge
   */
  public long expiresIn() {
    return this.expiresIn;
  }
}
