package dk.mineclub.plus.core.api;

import dk.mineclub.plus.core.MineClubPlusAddon;
import dk.mineclub.plus.core.api.model.ClientCatalogResponse;
import dk.mineclub.plus.core.api.model.ClientShopResponse;
import dk.mineclub.plus.core.api.model.ClientEconomyResponse;
import dk.mineclub.plus.core.api.model.ClientLogsResponse;
import dk.mineclub.plus.core.api.model.ClientMcmmoResponse;
import dk.mineclub.plus.core.api.model.ClientOverviewResponse;
import dk.mineclub.plus.core.api.model.ClientTransporterResponse;
import dk.mineclub.plus.core.api.model.EconomySnapshot;
import dk.mineclub.plus.core.api.model.ItemCategory;
import dk.mineclub.plus.core.api.model.LogEntry;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.api.model.PlayerProfile;
import dk.mineclub.plus.core.api.model.PlayerResponse;
import dk.mineclub.plus.core.api.model.ServerInfoResponse;
import dk.mineclub.plus.core.api.model.ServerStatus;
import dk.mineclub.plus.core.api.model.SkillSnapshot;
import dk.mineclub.plus.core.api.model.Transaction;
import dk.mineclub.plus.core.api.model.TransporterItem;
import dk.mineclub.plus.core.api.model.TransporterSnapshot;
import dk.mineclub.plus.core.cache.CachedResource;
import dk.mineclub.plus.core.util.Translations;
import dk.mineclub.plus.core.util.Formats;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.labymod.api.Laby;
import net.labymod.api.client.session.Session;
import net.labymod.api.util.io.web.request.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The addon's only entry point to MineClub data.
 *
 * <p>The public profile comes from {@code GET /v2/player/{uuid}} and needs no token. Player owned
 * data sits behind {@code /v2/client/...}, where {@link MineClubAuth} supplies the token. No uuid
 * is ever sent on those routes: the server resolves the player from the token.
 *
 * <p>Nothing polls in the background. The window requests on open and on an explicit refresh, and
 * at most one round of requests runs per {@link #TIME_TO_LIVE}. Sections whose request failed fall
 * back to sample numbers, which {@link #hasSampleData()} reports.
 */
public final class MineClubApi {

  /** Public profile endpoint. {@code %s} is the player's dashed UUID. */
  public static final String PLAYER_PATH = "/v2/player/%s";

  /** Network-wide numbers. No token, no identity. */
  public static final String INFO_PATH = "/v2/info/minecraft";

  /** Everything below is player owned and requires the token from {@link MineClubAuth}. */
  public static final String OVERVIEW_PATH = "/v2/client/overview";
  public static final String ECONOMY_PATH = "/v2/client/economy?antal=25";
  public static final String MCMMO_PATH = "/v2/client/mcmmo";

  /** The player's own log, deep enough to page back through. */
  public static final String LOGS_PATH = "/v2/client/logs?antal=60";

  /** Machine prices and server recipes. Identical for every player. */
  public static final String CATALOG_PATH = "/v2/client/catalog";

  /** Admin shop stock and prices. */
  public static final String SHOP_PATH = "/v2/client/shop";

  /**
   * The whole transporter in one call. Sorting and filtering run on the response, so switching
   * category costs no request.
   */
  public static final String TRANSPORTER_PATH = "/v2/client/transporter?antal=2000";

  /**
   * The API host. A constant rather than a setting: the endpoint is part of the addon, not
   * something a player configures.
   */
  private static final String BASE_URL = "https://api.mineclub.dk";

  private static final long TIME_TO_LIVE = TimeUnit.SECONDS.toMillis(30L);

  /** The catalog comes from server configuration and does not change while it is on screen. */
  private static final long CATALOG_TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(10L);

  /** Shop prices live on signs, which an admin can edit without a restart. */
  private static final long SHOP_TIME_TO_LIVE = TimeUnit.MINUTES.toMillis(5L);

  /**
   * Transaction types left out of the list.
   *
   * <p>{@code add} and {@code subtract} are the raw counter entries to a transaction that is
   * already listed: a shop trade produces both a {@code chestshop} row and a {@code subtract} row
   * for the same amount. Only the row that names what happened is kept. Balances and daily totals
   * are computed server side and still count both.
   */
  private static final Set<String> HIDDEN_TYPES = Set.of("add", "subtract");

  /** The fragment that precedes the item id in a serialized stack. */
  private static final String ITEM_ID_MARKER = "\"id\":\"";

  private final MineClubPlusAddon addon;
  private final MineClubAuth auth;
  private final CachedResource<MineClubSnapshot> snapshot;
  private final CachedResource<ClientCatalogResponse> catalog;
  private final CachedResource<ClientShopResponse> shop;

  private volatile boolean profileLive;

  /**
   * The last reason the sample numbers were used, so the same one is written once rather than on
   * every refresh while the window is open.
   */
  private volatile String lastFallbackReason;

  private volatile boolean economyLive;
  private volatile boolean transporterLive;
  private volatile boolean skillsLive;

  public MineClubApi(@NotNull MineClubPlusAddon addon) {
    this.addon = addon;
    this.auth = new MineClubAuth(addon);
    this.snapshot = new CachedResource<>(this::load, TIME_TO_LIVE);
    this.catalog = new CachedResource<>(this::loadCatalog, CATALOG_TIME_TO_LIVE);
    this.shop = new CachedResource<>(this::loadShop, SHOP_TIME_TO_LIVE);
  }

  public static @NotNull String baseUrl() {
    return BASE_URL;
  }

  public @NotNull CachedResource<MineClubSnapshot> snapshot() {
    return this.snapshot;
  }

  public @Nullable MineClubSnapshot data() {
    return this.snapshot.get();
  }

  public @NotNull MineClubAuth auth() {
    return this.auth;
  }

  /** Machine prices and recipes, fetched the first time a page asks for them. */
  public @NotNull CachedResource<ClientCatalogResponse> catalog() {
    return this.catalog;
  }

  /** Admin shop stock, fetched the first time the shop page opens. */
  public @NotNull CachedResource<ClientShopResponse> shop() {
    return this.shop;
  }

  /**
   * @return true when the profile came from the API rather than from the sample data
   */
  public boolean isProfileLive() {
    return this.profileLive;
  }

  /**
   * @return true while any section still renders sample numbers, which happens when the player
   * could not be verified or a request failed
   */
  public boolean hasSampleData() {
    return !this.economyLive || !this.transporterLive || !this.skillsLive;
  }

  /**
   * Writes why a section fell back to sample numbers, once per reason.
   *
   * <p>The window refreshes on a timer while it is open, so the same line would otherwise be
   * written every half minute.
   */
  private void fallback(@NotNull String reason) {
    if (reason.equals(this.lastFallbackReason)) {
      return;
    }

    this.lastFallbackReason = reason;
    this.addon.logger().warn("MineClub+ viser eksempeltal: " + reason);
  }

  public void refresh(boolean force) {
    this.snapshot.refresh(force);
  }

  /**
   * Loads the catalog. Behind the same token as the rest, so it fails rather than showing
   * invented numbers when the player cannot be verified.
   */
  private void loadCatalog(@NotNull Consumer<ClientCatalogResponse> success,
      @NotNull Consumer<Throwable> failure) {
    this.auth.token(token -> {
      if (token == null) {
        failure.accept(new IllegalStateException("no client token"));
        return;
      }

      this.get(ClientCatalogResponse.class, CATALOG_PATH, token, response -> {
        if (response.isPresent()) {
          success.accept(response.get());
          return;
        }

        failure.accept(new IllegalStateException("HTTP " + response.getStatusCode()));
      });
    });
  }

  /** Loads the shop. Behind the same token as the rest. */
  private void loadShop(@NotNull Consumer<ClientShopResponse> success,
      @NotNull Consumer<Throwable> failure) {
    this.auth.token(token -> {
      if (token == null) {
        failure.accept(new IllegalStateException("no client token"));
        return;
      }

      this.get(ClientShopResponse.class, SHOP_PATH, token, response -> {
        if (response.isPresent()) {
          success.accept(response.get());
          return;
        }

        failure.accept(new IllegalStateException("HTTP " + response.getStatusCode()));
      });
    });
  }

  private void load(@NotNull Consumer<MineClubSnapshot> success,
      @NotNull Consumer<Throwable> failure) {
    Session session = Laby.labyAPI().minecraft().sessionAccessor().getSession();
    if (session == null) {
      this.profileLive = false;
      this.economyLive = false;
      this.transporterLive = false;
      this.skillsLive = false;
      success.accept(DemoData.snapshot());
      return;
    }

    String name = session.getUsername();
    if (!session.hasUniqueId()) {
      this.request(name, data -> this.withInfo(data, success));
      return;
    }

    // The uuid route only reads MineClub's own records; the username route falls back to Mojang
    // and refreshes them. Trying the cheap one first keeps the common case to one lookup.
    this.request(session.getUniqueId().toString(), data -> {
      if (data != null) {
        this.withInfo(data, success);
        return;
      }

      this.request(name, second -> this.withInfo(second, success));
    });
  }

  /**
   * @param identifier the player's uuid or name; the endpoint accepts either
   * @param consumer   receives the payload, or {@code null} when the lookup found nothing
   */
  private void request(String identifier, Consumer<PlayerResponse.Data> consumer) {
    Requests.get(
            PlayerResponse.class,
            BASE_URL + String.format(Locale.ROOT, PLAYER_PATH, identifier),
            this.addon.userAgent(),
            null
        )
        .execute(response -> {
          if (response.isPresent() && response.get().data() != null) {
            consumer.accept(response.get().data());
            return;
          }

          this.addon.logger().warn("MineClub profile lookup for " + identifier + " failed: "
              + (response.hasException()
                  ? response.exception().getMessage()
                  : "HTTP " + response.getStatusCode()));
          consumer.accept(null);
        });
  }

  /** Follows the profile with the network numbers, then asks for the player's own data. */
  private void withInfo(
      @Nullable PlayerResponse.Data data,
      @NotNull Consumer<MineClubSnapshot> success
  ) {
    Requests.get(ServerInfoResponse.class, BASE_URL + INFO_PATH, this.addon.userAgent(), null)
        .execute(response -> this.withPlayerData(
            data,
            response.isPresent() ? response.get() : null,
            success
        ));
  }

  /**
   * Requests the player's own data. The five calls go out together and are joined once the last
   * one returns, so a refresh costs one round trip rather than five in sequence.
   */
  private void withPlayerData(
      @Nullable PlayerResponse.Data data,
      @Nullable ServerInfoResponse info,
      @NotNull Consumer<MineClubSnapshot> success
  ) {
    this.auth.token(token -> {
      if (token == null) {
        this.fallback("ingen klient-token, saa dine egne tal er eksempler");
        this.publish(data, info, null, null, null, null, success);
        return;
      }

      Pending pending = new Pending(data, info, success);

      this.get(ClientOverviewResponse.class, OVERVIEW_PATH, token, pending::overview);
      this.get(ClientTransporterResponse.class, TRANSPORTER_PATH, token, pending::transporter);
      this.get(ClientEconomyResponse.class, ECONOMY_PATH, token, pending::economy);
      this.get(ClientMcmmoResponse.class, MCMMO_PATH, token, pending::skills);
      this.get(ClientLogsResponse.class, LOGS_PATH, token, pending::logs);
    });
  }

  private <T> void get(Class<T> type, String path, String token, Consumer<Response<T>> consumer) {
    Requests.get(type, BASE_URL + path, this.addon.userAgent(), token)
        .execute(response -> {
          // Drop a token the server has stopped accepting, or every later call fails with it
          // until the client restarts.
          if (response.getStatusCode() == 401 || response.getStatusCode() == 403) {
            this.auth.invalidate();
          }

          if (!response.isPresent()) {
            this.addon.logger().warn("MineClub " + path + " failed: "
                + (response.hasException()
                    ? response.exception().getMessage()
                    : "HTTP " + response.getStatusCode()));
          }

          consumer.accept(response);
        });
  }

  /**
   * Joins the five responses. Each lands on its own thread, so the counter decides which one is
   * last and therefore publishes.
   */
  private final class Pending {

    private final PlayerResponse.Data data;
    private final ServerInfoResponse info;
    private final Consumer<MineClubSnapshot> success;
    private final AtomicInteger missing = new AtomicInteger(5);

    private volatile ClientOverviewResponse overview;
    private volatile ClientTransporterResponse transporter;
    private volatile ClientEconomyResponse economy;
    private volatile ClientMcmmoResponse mcmmo;
    private volatile ClientLogsResponse logs;

    private Pending(
        @Nullable PlayerResponse.Data data,
        @Nullable ServerInfoResponse info,
        @NotNull Consumer<MineClubSnapshot> success
    ) {
      this.data = data;
      this.info = info;
      this.success = success;
    }

    private void overview(Response<ClientOverviewResponse> response) {
      this.overview = response.isPresent() ? response.get() : null;
      this.done();
    }

    private void transporter(Response<ClientTransporterResponse> response) {
      this.transporter = response.isPresent() ? response.get() : null;
      this.done();
    }

    private void economy(Response<ClientEconomyResponse> response) {
      this.economy = response.isPresent() ? response.get() : null;
      this.done();
    }

    private void skills(Response<ClientMcmmoResponse> response) {
      this.mcmmo = response.isPresent() ? response.get() : null;
      this.done();
    }

    private void logs(Response<ClientLogsResponse> response) {
      this.logs = response.isPresent() ? response.get() : null;
      this.done();
    }

    private void done() {
      if (this.missing.decrementAndGet() > 0) {
        return;
      }

      MineClubApi.this.publish(
          this.data,
          this.info,
          new Economy(this.overview, this.economy),
          this.transporter,
          this.mcmmo,
          this.logs,
          this.success
      );
    }
  }

  /** The two responses that together make up the economy. Either may be missing on its own. */
  private record Economy(
      @Nullable ClientOverviewResponse overview,
      @Nullable ClientEconomyResponse economy
  ) {

  }

  /**
   * Publishes the snapshot: live numbers where the request came back, sample numbers for the rest.
   */
  private void publish(
      @Nullable PlayerResponse.Data data,
      @Nullable ServerInfoResponse info,
      @Nullable Economy economy,
      @Nullable ClientTransporterResponse transporter,
      @Nullable ClientMcmmoResponse mcmmo,
      @Nullable ClientLogsResponse logs,
      @NotNull Consumer<MineClubSnapshot> success
  ) {
    MineClubSnapshot sample = DemoData.snapshot();

    EconomySnapshot economySnapshot = toEconomy(economy, transporter);
    this.economyLive = economySnapshot != null;

    // A request that came back without the section the page needs used to be indistinguishable
    // from one that was never sent: both ended as sample numbers, neither said anything.
    if (economySnapshot == null && economy != null) {
      this.fallback(economy.overview() == null
          ? "oversigten kom ikke igennem, saa oekonomien er eksempler"
          : "oversigten kom uden coins, saa oekonomien er eksempler");
    } else if (economySnapshot != null) {
      this.lastFallbackReason = null;
    }

    TransporterSnapshot transporterSnapshot = toTransporter(transporter);
    this.transporterLive = transporterSnapshot != null;

    List<SkillSnapshot> skills = toSkills(mcmmo);
    this.skillsLive = skills != null;

    this.profileLive = data != null;

    ClientOverviewResponse overview = economy == null ? null : economy.overview();

    success.accept(new MineClubSnapshot(
        data == null ? sample.profile() : toProfile(data, overview),
        economySnapshot == null ? sample.economy() : economySnapshot,
        transporterSnapshot == null ? sample.transporter() : transporterSnapshot,
        data == null ? sample.server() : toServer(data, info),
        skills == null ? sample.skills() : skills,
        toLogs(logs)
    ));
  }

  /**
   * @return null when the overview did not come through, leaving no balance to show
   */
  private static @Nullable EconomySnapshot toEconomy(
      @Nullable Economy economy,
      @Nullable ClientTransporterResponse transporter
  ) {
    if (economy == null || economy.overview() == null) {
      return null;
    }

    ClientOverviewResponse.Coins coins = economy.overview().coins();
    ClientOverviewResponse.NetWorth netWorth = economy.overview().netWorth();
    if (coins == null) {
      return null;
    }

    // Net worth is part of the overview; the item list is only summed when it is missing.
    double worth = netWorth != null
        ? netWorth.total()
        : coins.balance() + (transporter != null && transporter.summary() != null
            ? transporter.summary().totalValue()
            : 0.0D);

    return new EconomySnapshot(
        coins.balance(),
        coins.today(),
        worth,
        netWorth == null ? coins.today() : netWorth.today(),
        coins.earnedToday(),
        coins.spentToday(),
        toTransactions(economy.economy())
    );
  }

  private static List<Transaction> toTransactions(@Nullable ClientEconomyResponse economy) {
    List<Transaction> transactions = new ArrayList<>();
    if (economy == null) {
      return transactions;
    }

    for (ClientEconomyResponse.Entry entry : economy.data()) {
      String type = entry.type() == null ? "" : entry.type();
      if (HIDDEN_TYPES.contains(type)) {
        continue;
      }

      transactions.add(new Transaction(
          millis(entry.at()),
          describe(entry, type),
          type,
          entry.incoming() ? entry.amount() : -entry.amount()
      ));
    }

    return transactions;
  }

  /**
   * @return null when the transporter did not come through
   */
  private static @Nullable TransporterSnapshot toTransporter(
      @Nullable ClientTransporterResponse response
  ) {
    if (response == null) {
      return null;
    }

    List<TransporterItem> items = new ArrayList<>();
    for (ClientTransporterResponse.Item item : response.data()) {
      String key = item.key() == null ? "" : item.key();
      if (key.isEmpty()) {
        continue;
      }

      items.add(new TransporterItem(
          key,
          item.name() == null ? key : item.name(),
          ItemCategory.of(item.category()),
          item.amount(),
          item.unitPrice(),
          item.change() == null ? 0.0D : item.change()
      ));
    }

    return new TransporterSnapshot(items);
  }

  /**
   * @return null when mcMMO did not come through, or when the plugin has written nothing yet
   */
  private static @Nullable List<SkillSnapshot> toSkills(@Nullable ClientMcmmoResponse response) {
    if (response == null || response.data().isEmpty()) {
      return null;
    }

    List<SkillSnapshot> skills = new ArrayList<>();
    for (ClientMcmmoResponse.Skill skill : response.data()) {
      String id = skill.skill() == null ? "" : skill.skill();
      if (id.isEmpty()) {
        continue;
      }

      skills.add(new SkillSnapshot(
          id.toLowerCase(Locale.ROOT),
          skill.displayName() == null ? id : skill.displayName(),
          skill.level(),
          skill.xp(),
          skill.xpToNext()
      ));
    }

    return skills;
  }

  /**
   * @return the log rows, or an empty list when the call did not come through. Sample rows would
   *     claim events that never happened.
   */
  private static List<LogEntry> toLogs(@Nullable ClientLogsResponse response) {
    List<LogEntry> entries = new ArrayList<>();
    if (response == null) {
      return entries;
    }

    for (ClientLogsResponse.Row row : response.data()) {
      ClientLogsResponse.Player player = row.player();

      entries.add(new LogEntry(
          millis(row.at()),
          row.kind() == null ? "other" : row.kind(),
          row.action() == null ? "" : row.action(),
          row.item(),
          row.itemName(),
          row.amount(),
          row.coins(),
          player == null ? null : player.uuid(),
          player == null ? null : player.username(),
          player == null ? null : player.type()
      ));
    }

    return entries;
  }

  /**
   * @param overview carries the play time, which is derived from the join log rather than stored
   *     on the profile -- the same source the staff panel reads
   */
  private static PlayerProfile toProfile(
      PlayerResponse.Data data,
      @Nullable ClientOverviewResponse overview
  ) {
    String name = data.username() == null ? "?" : data.username();
    // Kept as the server spells it: the group name decides both the colour and which
    // translation is looked up, and the display upper cases it.
    String rank = data.role() == null ? "" : data.role();

    ClientOverviewResponse.Playtime playtime = overview == null ? null : overview.playtime();
    long minutes = playtime == null ? 0L : playtime.seconds() / 60L;

    // The profile knows when the account was created; the log knows when the player first
    // joined. Whichever exists first wins.
    String since = playtime == null ? null : year(playtime.firstSeen());
    if (since == null || since.isEmpty()) {
      since = year(data.firstSeen());
    }

    return new PlayerProfile(
        parseUuid(data.uuid()),
        name,
        rank,
        since,
        minutes,
        data.online()
    );
  }

  private static ServerStatus toServer(
      PlayerResponse.Data data,
      @Nullable ServerInfoResponse info
  ) {
    String current = data.currentServer() != null ? data.currentServer() : data.primaryServer();

    return new ServerStatus(
        "mc.mineclub.dk",
        current == null ? "" : current,
        data.online() ? "Online" : "Offline",
        info == null ? 0 : info.onlinePlayers()
    );
  }

  /**
   * Describes a transaction in one line.
   *
   * <p>The type alone says little: "Chestshop" names neither the goods nor the direction. The log
   * carries both, so the line states what was traded, how much of it, and with whom.
   *
   * <p>Direction comes from who received the coins rather than from the shop's own {@code
   * transactionType}, which is written from the shop's point of view.
   */
  private static String describe(ClientEconomyResponse.Entry entry, String type) {
    ClientEconomyResponse.Info info = entry.info();

    String item = info == null ? null : info.item();
    if (item != null && !item.isEmpty()) {
      String amount = Formats.number(Math.max(1L, info.stock()));
      String name = itemName(item);
      if (name != null) {
        return Translations.get(
            entry.incoming() ? "mineclubplus.economy.sold" : "mineclubplus.economy.bought",
            amount,
            name
        );
      }
    }

    String other = counterparty(entry);
    if (other != null) {
      return Translations.get(
          entry.incoming() ? "mineclubplus.economy.from" : "mineclubplus.economy.to",
          other
      );
    }

    if (info != null && info.regionName() != null && !info.regionName().isEmpty()) {
      return VanillaCatalog.displayName(type) + " - " + info.regionName();
    }

    return label(type);
  }

  /**
   * The name of what was traded.
   *
   * <p>The log stores the serialized stack rather than a material name, for example {@code
   * [{"DataVersion":4903,"id":"minecraft:clay_ball","count":64,...}]}. Without this the raw text
   * would end up in the list.
   *
   * @return the readable name, or null when the text holds no item id
   */
  private static @Nullable String itemName(String raw) {
    String id = raw;

    int marker = raw.indexOf(ITEM_ID_MARKER);
    if (marker >= 0) {
      int start = marker + ITEM_ID_MARKER.length();
      int end = raw.indexOf('"', start);
      id = end < 0 ? raw.substring(start) : raw.substring(start, end);
    } else if (raw.startsWith("[") || raw.startsWith("{")) {
      // A stack with no id we could find; the type reads better than the raw text.
      return null;
    }

    // The namespace means nothing to a player, and a space can stand where an underscore did.
    int colon = id.indexOf(':');
    if (colon >= 0) {
      id = id.substring(colon + 1);
    }

    // Lower case first: a name arriving as DIAMOND_BLOCK would otherwise stay in capitals.
    id = id.trim().replace(' ', '_').toLowerCase(Locale.ROOT);
    return id.isEmpty() ? null : VanillaCatalog.displayName(id);
  }

  /**
   * @return the name of the other party, or null when that is the server itself -- "from System"
   *     says less than the type alone
   */
  private static @Nullable String counterparty(ClientEconomyResponse.Entry entry) {
    ClientEconomyResponse.Actor actor = entry.incoming() ? entry.from() : entry.to();
    if (actor == null || !"player".equalsIgnoreCase(actor.type())) {
      return null;
    }

    String name = actor.displayName();
    return name == null || name.isEmpty() ? null : name;
  }

  /**
   * @return the transaction type in readable form, for example "Chestshop" for {@code chestshop}.
   *     The same conversion the item names get, so both lists read alike.
   */
  private static String label(String type) {
    return type.isEmpty()
        ? Translations.get("mineclubplus.economy.movement")
        : VanillaCatalog.displayName(type);
  }

  private static long millis(@Nullable String isoDate) {
    if (isoDate == null || isoDate.isEmpty()) {
      return 0L;
    }

    try {
      return Instant.parse(isoDate).toEpochMilli();
    } catch (DateTimeParseException exception) {
      return 0L;
    }
  }

  private static @Nullable UUID parseUuid(@Nullable String raw) {
    if (raw == null || raw.isEmpty()) {
      return null;
    }

    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  /**
   * @param isoDate an ISO timestamp from the API
   * @return just the year, which is what "member since" shows
   */
  private static String year(@Nullable String isoDate) {
    if (isoDate == null || isoDate.length() < 4) {
      return "";
    }

    return isoDate.substring(0, 4);
  }

}
