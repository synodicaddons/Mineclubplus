package dk.mineclub.plus.core.api.model;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The response from {@code GET /v2/client/mcmmo}.
 *
 * <p>The numbers come from the snapshot the lobby plugin writes, so {@link #stale()} is set when
 * the player has been offline for a while.
 */
public class ClientMcmmoResponse {

  private boolean success;
  private int powerLevel;
  private String collectedAt;
  private boolean stale;
  private List<Skill> data;

  public boolean success() {
    return this.success;
  }

  public int powerLevel() {
    return this.powerLevel;
  }

  public @Nullable String collectedAt() {
    return this.collectedAt;
  }

  public boolean stale() {
    return this.stale;
  }

  public @NotNull List<Skill> data() {
    return this.data == null ? Collections.emptyList() : this.data;
  }

  public static class Skill {

    private String skill;
    private String displayName;
    private int level;
    private long xp;
    private long xpToNext;
    private boolean childSkill;

    public @Nullable String skill() {
      return this.skill;
    }

    public @Nullable String displayName() {
      return this.displayName;
    }

    public int level() {
      return this.level;
    }

    public long xp() {
      return this.xp;
    }

    public long xpToNext() {
      return this.xpToNext;
    }

    /**
     * @return true for child skills, which have no XP of their own
     */
    public boolean childSkill() {
      return this.childSkill;
    }
  }
}
