package dk.mineclub.plus.core.api.model;

import org.jetbrains.annotations.Nullable;

/**
 * The response from {@code POST /v2/client/auth/verify}. The token from here is sent as
 * {@code Authorization: Bearer} on every {@code /v2/client/...} call.
 */
public class ClientVerifyResponse {

  private boolean success;
  private String token;
  private String expiresAt;
  private Player player;

  public boolean success() {
    return this.success;
  }

  public @Nullable String token() {
    return this.token;
  }

  public @Nullable String expiresAt() {
    return this.expiresAt;
  }

  public @Nullable Player player() {
    return this.player;
  }

  public static class Player {

    private String uuid;
    private String username;

    public @Nullable String uuid() {
      return this.uuid;
    }

    public @Nullable String username() {
      return this.username;
    }
  }
}
