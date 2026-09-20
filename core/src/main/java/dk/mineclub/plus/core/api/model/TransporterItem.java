package dk.mineclub.plus.core.api.model;

import org.jetbrains.annotations.NotNull;

/**
 * One stack type inside the transporter. {@code amount} is the total item count, not the stack
 * count -- the UI derives stacks from it so the server stays the single source of truth.
 */
public class TransporterItem {

  private String id;
  private String displayName;
  private String category;
  private long amount;
  private double unitValue;
  private double change24h;

  public TransporterItem() {
  }

  public TransporterItem(
      @NotNull String id,
      @NotNull String displayName,
      @NotNull ItemCategory category,
      long amount,
      double unitValue,
      double change24h
  ) {
    this.id = id;
    this.displayName = displayName;
    this.category = category.id();
    this.amount = amount;
    this.unitValue = unitValue;
    this.change24h = change24h;
  }

  public @NotNull String id() {
    return this.id == null ? "" : this.id;
  }

  public @NotNull String displayName() {
    return this.displayName == null ? this.id() : this.displayName;
  }

  public @NotNull ItemCategory category() {
    return ItemCategory.of(this.category);
  }

  public long amount() {
    return this.amount;
  }

  public double unitValue() {
    return this.unitValue;
  }

  public double totalValue() {
    return this.unitValue * this.amount;
  }

  public double change24h() {
    return this.change24h;
  }

  /**
   * @return how many full stacks of 64 the amount covers, used for the "x stakke" label.
   */
  public long stacks() {
    return this.amount / 64L;
  }
}
