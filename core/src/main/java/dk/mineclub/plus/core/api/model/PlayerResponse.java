package dk.mineclub.plus.core.api.model;

import org.jetbrains.annotations.Nullable;

/**
 * Maps {@code GET /v2/player/{uuid}} of the MineClub API one to one.
 *
 * <p>Field names match the JSON exactly so the mapping stays obvious when the endpoint changes.
 * Nothing here is assumed to be present: the endpoint blanks out fields the player has hidden on
 * their public profile.
 */
public class PlayerResponse {

  private boolean success;
  private String message;
  private Data data;

  public boolean success() {
    return this.success;
  }

  public @Nullable String message() {
    return this.message;
  }

  public @Nullable Data data() {
    return this.data;
  }

  public static class Data {

    private String username;
    private String uuid;
    private boolean online;
    private String points;
    private String firstSeen;
    private String lastSeen;
    private String prefix;
    private String role;
    private int vipdays;
    private String currentServer;
    private String primaryServer;

    public @Nullable String username() {
      return this.username;
    }

    public @Nullable String uuid() {
      return this.uuid;
    }

    public boolean online() {
      return this.online;
    }

    /**
     * @return the point balance as a decimal string, or {@code null} when the player hides it
     */
    public @Nullable String points() {
      return this.points;
    }

    public @Nullable String firstSeen() {
      return this.firstSeen;
    }

    public @Nullable String lastSeen() {
      return this.lastSeen;
    }

    /**
     * @return the LuckPerms prefix, which carries the colours the server shows in chat
     */
    public @Nullable String prefix() {
      return this.prefix;
    }

    public @Nullable String role() {
      return this.role;
    }

    public int vipDays() {
      return this.vipdays;
    }

    public @Nullable String currentServer() {
      return this.currentServer;
    }

    public @Nullable String primaryServer() {
      return this.primaryServer;
    }
  }
}
