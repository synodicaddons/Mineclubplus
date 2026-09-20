package dk.mineclub.plus.core.api.model;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The response from {@code GET /v2/client/logs}.
 *
 * <p>The server merges the shop, transfer and transporter rows and sorts them, so the client only
 * has to render them.
 */
public class ClientLogsResponse {

  private boolean success;
  private boolean hasMore;
  private List<Row> data;

  public boolean success() {
    return this.success;
  }

  /** True when more rows exist behind the page that was fetched. */
  public boolean hasMore() {
    return this.hasMore;
  }

  public @NotNull List<Row> data() {
    return this.data == null ? Collections.emptyList() : this.data;
  }

  public static class Row {

    private String at;
    private String kind;
    private String action;
    private String item;
    private String itemName;
    private long amount;
    private double coins;
    private Player player;

    /**
     * @return the timestamp as an ISO string in UTC
     */
    public @Nullable String at() {
      return this.at;
    }

    public @Nullable String kind() {
      return this.kind;
    }

    public @Nullable String action() {
      return this.action;
    }

    public @Nullable String item() {
      return this.item;
    }

    public @Nullable String itemName() {
      return this.itemName;
    }

    public long amount() {
      return this.amount;
    }

    public double coins() {
      return this.coins;
    }

    /** The other party in the transaction, when there was one. */
    public @Nullable Player player() {
      return this.player;
    }
  }

  public static class Player {

    private String uuid;
    private String username;
    private String type;

    public @Nullable String uuid() {
      return this.uuid;
    }

    public @Nullable String username() {
      return this.username;
    }

    /**
     * @return {@code player}, {@code service} or {@code system} -- the console and a plot are not
     *     players and have no head to look up
     */
    public @Nullable String type() {
      return this.type;
    }
  }
}
