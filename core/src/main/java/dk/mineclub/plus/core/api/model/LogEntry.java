package dk.mineclub.plus.core.api.model;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One row in the player's own log.
 *
 * <p>A trade, a transfer or a transporter movement, written from the player's side, so "sold"
 * always means the player sold, whoever stood on the other side of the counter.
 */
public class LogEntry {

  private long timestamp;
  private String kind;
  private String action;
  private String item;
  private String itemName;
  private long amount;
  private double coins;
  private String playerUuid;
  private String playerName;
  private String playerType;

  public LogEntry() {
  }

  public LogEntry(
      long timestamp,
      @NotNull String kind,
      @NotNull String action,
      @Nullable String item,
      @Nullable String itemName,
      long amount,
      double coins,
      @Nullable String playerUuid,
      @Nullable String playerName,
      @Nullable String playerType
  ) {
    this.timestamp = timestamp;
    this.kind = kind;
    this.action = action;
    this.item = item;
    this.itemName = itemName;
    this.amount = amount;
    this.coins = coins;
    this.playerUuid = playerUuid;
    this.playerName = playerName;
    this.playerType = playerType;
  }

  public long timestamp() {
    return this.timestamp;
  }

  /**
   * @return {@code shop}, {@code transfer}, {@code transporter} eller {@code other}
   */
  public @NotNull String kind() {
    return this.kind == null ? "other" : this.kind;
  }

  /**
   * @return {@code bought}, {@code sold}, {@code sent}, {@code received}, {@code add} eller
   *     {@code remove}
   */
  public @NotNull String action() {
    return this.action == null ? "" : this.action;
  }

  /**
   * @return the material, so the row can render the right block, or null when the row is only
   *     about coins
   */
  public @Nullable String item() {
    return this.item;
  }

  public @NotNull String itemName() {
    return this.itemName == null ? "" : this.itemName;
  }

  public long amount() {
    return this.amount;
  }

  /** Coins in (positive) or out (negative). 0 when no money was involved. */
  public double coins() {
    return this.coins;
  }

  /**
   * @return the other party's uuid, so the row can render their head, or null when there was
   *     none -- an admin shop purchase, or something the player stored themselves
   */
  public @Nullable String playerUuid() {
    return this.playerUuid;
  }

  public @NotNull String playerName() {
    return this.playerName == null ? "" : this.playerName;
  }

  /**
   * @return true when the other party is a real player. The console and a plot have no head, and
   *     a lookup by their name would return a stranger's skin
   */
  public boolean isPlayer() {
    return "player".equalsIgnoreCase(this.playerType);
  }

  /**
   * @return the key of the text describing the row, e.g. {@code mineclubplus.logs.action.sold}
   */
  public @NotNull String actionKey() {
    return "mineclubplus.logs.action." + this.action().toLowerCase(Locale.ROOT);
  }
}
