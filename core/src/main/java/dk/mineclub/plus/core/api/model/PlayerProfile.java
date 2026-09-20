package dk.mineclub.plus.core.api.model;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The player as MineClub knows them. Deserialized straight from the API, so every field is
 * nullable until the response has been validated.
 */
public class PlayerProfile {

  private String uuid;
  private String name;
  private String rank;
  private String memberSince;
  private long playtimeMinutes;
  private boolean online;

  public PlayerProfile() {
  }

  public PlayerProfile(
      @Nullable UUID uuid,
      @NotNull String name,
      @NotNull String rank,
      @NotNull String memberSince,
      long playtimeMinutes,
      boolean online
  ) {
    this.uuid = uuid == null ? null : uuid.toString();
    this.name = name;
    this.rank = rank;
    this.memberSince = memberSince;
    this.playtimeMinutes = playtimeMinutes;
    this.online = online;
  }

  public @Nullable UUID uniqueId() {
    if (this.uuid == null || this.uuid.isEmpty()) {
      return null;
    }

    try {
      return UUID.fromString(this.uuid);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  public @NotNull String name() {
    return this.name == null ? "?" : this.name;
  }

  public @NotNull String rank() {
    return this.rank == null ? "" : this.rank;
  }

  public @NotNull String memberSince() {
    return this.memberSince == null ? "" : this.memberSince;
  }

  public long playtimeMinutes() {
    return this.playtimeMinutes;
  }

  public boolean online() {
    return this.online;
  }
}
