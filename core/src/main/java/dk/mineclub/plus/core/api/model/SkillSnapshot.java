package dk.mineclub.plus.core.api.model;

import org.jetbrains.annotations.NotNull;

/**
 * One mcMMO skill. Only rendered when the API actually returns skills, so servers without mcMMO
 * simply do not show the section.
 */
public class SkillSnapshot {

  private String id;
  private String displayName;
  private int level;
  private long experience;
  private long experienceToNextLevel;

  public SkillSnapshot() {
  }

  public SkillSnapshot(
      @NotNull String id,
      @NotNull String displayName,
      int level,
      long experience,
      long experienceToNextLevel
  ) {
    this.id = id;
    this.displayName = displayName;
    this.level = level;
    this.experience = experience;
    this.experienceToNextLevel = experienceToNextLevel;
  }

  public @NotNull String id() {
    return this.id == null ? "" : this.id;
  }

  public @NotNull String displayName() {
    return this.displayName == null ? this.id() : this.displayName;
  }

  public int level() {
    return this.level;
  }

  public long experience() {
    return this.experience;
  }

  public long experienceToNextLevel() {
    return this.experienceToNextLevel;
  }

  public float progress() {
    if (this.experienceToNextLevel <= 0L) {
      return 0.0F;
    }

    return Math.min(1.0F, (float) this.experience / (float) this.experienceToNextLevel);
  }
}
