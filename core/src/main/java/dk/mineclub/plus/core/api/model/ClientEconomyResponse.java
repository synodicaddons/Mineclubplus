package dk.mineclub.plus.core.api.model;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The response from {@code GET /v2/client/economy}: the balance, what went in and out, and the
 * most recent transactions.
 */
public class ClientEconomyResponse {

  private boolean success;
  private double balance;
  private Change change;
  private List<Entry> data;

  public boolean success() {
    return this.success;
  }

  public double balance() {
    return this.balance;
  }

  public @Nullable Change change() {
    return this.change;
  }

  public @NotNull List<Entry> data() {
    return this.data == null ? Collections.emptyList() : this.data;
  }

  public static class Change {

    private Delta today;
    private Delta week;
    private Delta month;

    public @Nullable Delta today() {
      return this.today;
    }

    public @Nullable Delta week() {
      return this.week;
    }

    public @Nullable Delta month() {
      return this.month;
    }
  }

  public static class Delta {

    private double net;
    private double earned;
    private double spent;

    public double net() {
      return this.net;
    }

    public double earned() {
      return this.earned;
    }

    public double spent() {
      return this.spent;
    }
  }

  /** One end of a transfer. */
  public static class Actor {

    private String type;
    private String username;
    private String name;

    public @Nullable String type() {
      return this.type;
    }

    /**
     * @return the player's name, or the server's when the other party was not a player
     */
    public @Nullable String displayName() {
      return this.username != null && !this.username.isEmpty() ? this.username : this.name;
    }
  }

  /**
   * The details behind a transaction.
   *
   * <p>The fields come from different types -- a shop trade carries an item and an amount, rent
   * carries a region -- so only the ones matching the type are populated.
   */
  public static class Info {

    private String item;
    private long stock;
    private String shop;
    private String cause;
    private String regionName;

    public @Nullable String item() {
      return this.item;
    }

    /** Antal styk i handlen. */
    public long stock() {
      return this.stock;
    }

    public @Nullable String shop() {
      return this.shop;
    }

    public @Nullable String cause() {
      return this.cause;
    }

    public @Nullable String regionName() {
      return this.regionName;
    }
  }

  public static class Entry {

    private String id;
    private String at;
    private String type;
    private double amount;
    private boolean incoming;
    private Actor from;
    private Actor to;
    private Info info;

    /** Where the coins came from: a player, a server, or the system itself. */
    public @Nullable Actor from() {
      return this.from;
    }

    public @Nullable Actor to() {
      return this.to;
    }

    /** What the log knows about the transaction. Empty for most types. */
    public @Nullable Info info() {
      return this.info;
    }

    public @Nullable String id() {
      return this.id;
    }

    /**
     * @return the timestamp as an ISO string in UTC
     */
    public @Nullable String at() {
      return this.at;
    }

    public @Nullable String type() {
      return this.type;
    }

    /**
     * @return the amount unsigned; {@link #incoming()} states which way it went
     */
    public double amount() {
      return this.amount;
    }

    public boolean incoming() {
      return this.incoming;
    }
  }
}
