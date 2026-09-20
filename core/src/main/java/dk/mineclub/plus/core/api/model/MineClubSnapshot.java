package dk.mineclub.plus.core.api.model;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The single payload MineClub+ asks for. Bundling the pages into one document keeps the addon at
 * one request per refresh instead of five.
 */
public class MineClubSnapshot {

  private PlayerProfile profile;
  private EconomySnapshot economy;
  private TransporterSnapshot transporter;
  private ServerStatus server;
  private List<SkillSnapshot> skills;
  private List<LogEntry> logs;

  public MineClubSnapshot() {
  }

  public MineClubSnapshot(
      @Nullable PlayerProfile profile,
      @Nullable EconomySnapshot economy,
      @Nullable TransporterSnapshot transporter,
      @Nullable ServerStatus server,
      @Nullable List<SkillSnapshot> skills,
      @Nullable List<LogEntry> logs
  ) {
    this.profile = profile;
    this.economy = economy;
    this.transporter = transporter;
    this.server = server;
    this.skills = skills;
    this.logs = logs;
  }

  public @Nullable PlayerProfile profile() {
    return this.profile;
  }

  public @Nullable EconomySnapshot economy() {
    return this.economy;
  }

  public @Nullable TransporterSnapshot transporter() {
    return this.transporter;
  }

  public @Nullable ServerStatus server() {
    return this.server;
  }

  public @NotNull List<SkillSnapshot> skills() {
    return this.skills == null ? Collections.emptyList()
        : Collections.unmodifiableList(this.skills);
  }

  /** Spillerens egen log, nyeste foerst. */
  public @NotNull List<LogEntry> logs() {
    return this.logs == null ? Collections.emptyList()
        : Collections.unmodifiableList(this.logs);
  }
}
