package dk.mineclub.plus.core.api.model;

import org.jetbrains.annotations.NotNull;

/**
 * A single entry of the economy log. {@link #amount()} is signed: positive means the player
 * earned coins, negative means they spent them.
 */
public class Transaction {

  private long timestamp;
  private String label;
  private String source;
  private double amount;

  public Transaction() {
  }

  public Transaction(long timestamp, @NotNull String label, @NotNull String source, double amount) {
    this.timestamp = timestamp;
    this.label = label;
    this.source = source;
    this.amount = amount;
  }

  public long timestamp() {
    return this.timestamp;
  }

  public @NotNull String label() {
    return this.label == null ? "" : this.label;
  }

  public @NotNull String source() {
    return this.source == null ? "" : this.source;
  }

  public double amount() {
    return this.amount;
  }

  public boolean income() {
    return this.amount >= 0.0D;
  }
}
