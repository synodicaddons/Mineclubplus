package dk.mineclub.plus.core.ui;

import dk.mineclub.plus.core.util.Translations;
import dk.mineclub.plus.core.MineClubPlusAddon;
import dk.mineclub.plus.core.api.model.ItemCategory;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.api.model.PlayerProfile;
import dk.mineclub.plus.core.ui.page.EconomyPage;
import dk.mineclub.plus.core.ui.page.EconomyPeriod;
import dk.mineclub.plus.core.ui.page.LogsPage;
import dk.mineclub.plus.core.ui.page.MaterialsPage;
import dk.mineclub.plus.core.ui.page.OffNetworkPage;
import dk.mineclub.plus.core.ui.page.OverviewPage;
import dk.mineclub.plus.core.ui.page.PageRenderer;
import dk.mineclub.plus.core.ui.page.PlaceholderPage;
import dk.mineclub.plus.core.ui.page.SettingsPage;
import dk.mineclub.plus.core.ui.page.ShopPage;
import dk.mineclub.plus.core.ui.page.StatisticsPage;
import dk.mineclub.plus.core.ui.page.TransporterPage;
import dk.mineclub.plus.core.ui.widget.Brand;
import dk.mineclub.plus.core.ui.widget.States;
import dk.mineclub.plus.core.ui.widget.Widgets;
import dk.mineclub.plus.core.util.Avatars;
import dk.mineclub.plus.core.util.Ranks;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.labymod.api.Textures.SpriteCommon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.ScreenInstance;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.activities.ConfirmActivity;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.action.Pressable;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The MineClub+ window.
 *
 * <p>The shell is one box whose children pin themselves to its edges -- header, sidebar, content
 * and the dividers between them. Nothing in the shell sizes itself from its children, so the
 * layout is correct on the first frame and cannot drift.
 *
 * <p>Switching page or receiving new data marks the activity dirty; the rebuild happens once on
 * the next tick, so neither a click nor an API response can rebuild widgets mid-frame.
 *
 * <p>Every responsive decision comes from {@link Layout}, which measures the window rather than
 * the screen -- the two are different numbers and the GUI scale moves them apart. The mode it
 * picks is hung on the shell as an id, because a stylesheet media query can only see the screen.
 */
@Link("mineclubplus.lss")
public class MineClubPlusActivity extends SimpleActivity {

  /**
   * The largest amount the shop buys at once: a full inventory.
   *
   * <p>The server refuses more anyway, so the buttons should not be able to ask for it. The same
   * limit lives in {@code AdminShopBuyCommand}.
   */
  public static final int MAX_SHOP_AMOUNT = 36 * 64;


  private final MineClubPlusAddon addon;
  private final Map<MineClubPage, PageRenderer> pages = new EnumMap<>(MineClubPage.class);
  private final PageRenderer offNetworkPage = new OffNetworkPage();
  private final Runnable dataListener = this::markDirty;

  private MineClubPage page = MineClubPage.OVERVIEW;
  private ItemCategory categoryFilter = ItemCategory.ALL;
  private TransporterSort transporterSort = TransporterSort.VALUE;
  private EconomyPeriod economyPeriod = EconomyPeriod.DAY;
  private String selectedItemId;
  private String search = "";
  private boolean searchFocused;
  private String takeInput = "";
  private boolean takeInputFocused;
  private int gridPage;

  /** The page number in the log and in the reference. They share it; only one is on screen
   * at a time. */
  private int listPage;
  private String selectedRecipe;

  /** The selected shop item and its amount. The amount is kept when another item is picked. */
  private String shopItem;
  private int shopAmount = 1;
  private boolean dirty;
  private Layout layout = Layout.current();
  private boolean offNetwork;
  private long lastAutoRefresh;
  private long autoRefreshIntervalMillis;


  public MineClubPlusActivity(@NotNull MineClubPlusAddon addon) {
    this.addon = addon;

    this.pages.put(MineClubPage.OVERVIEW, new OverviewPage());
    this.pages.put(MineClubPage.TRANSPORTER, new TransporterPage());
    this.pages.put(MineClubPage.ECONOMY, new EconomyPage());
    this.pages.put(MineClubPage.STATISTICS, new StatisticsPage());
    this.pages.put(MineClubPage.SETTINGS, new SettingsPage());
    this.pages.put(MineClubPage.MATERIALS, new MaterialsPage());
    this.pages.put(MineClubPage.LOGS, new LogsPage());
    this.pages.put(MineClubPage.SHOP, new ShopPage());
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.dirty = false;

    // Read once per rebuild rather than once per tick: ConfigProperty#get boxes, and the store
    // guidelines rule out allocating every tick. A settings change rebuilds the window anyway.
    this.autoRefreshIntervalMillis = TimeUnit.SECONDS.toMillis(
        this.addon.configuration().refreshIntervalSeconds().get()
    );

    // The backdrop is one extra id on top, so the window is otherwise styled as before.
    DivWidget window = this.addon.configuration().windowBackground().get()
        ? Widgets.panel("mcp-window")
        : Widgets.panel("mcp-window", "mcp-window-clear");
    window.addChild(this.createHeader());
    window.addChild(Widgets.panel("mcp-divider", "mcp-divider-header"));

    if (this.isPreparing()) {
      // Nothing has arrived the first time the window opens. A half empty dashboard filling
      // in field by field looks broken, so it waits for the response; no rebuild has to be
      // scheduled, because a completed request already asks for one.
      DivWidget waiting = Widgets.panel("mcp-blocker");

      if (this.addon.api().snapshot().error() == null) {
        DivWidget panel = Widgets.panel("mcp-offnetwork");
        panel.addChild(Widgets.i18n("mineclubplus.state.preparing", "mcp-offnetwork-title"));
        panel.addChild(Widgets.component(Brand.wordmark(), "mcp-offnetwork-address"));
        waiting.addChild(panel);
      } else {
        // The first request failed. Without this the window would say "loading" forever,
        // with no way on except closing it.
        waiting.addChild(States.failed(() -> this.addon.api().refresh(true)));
      }

      window.addChild(waiting);
    } else if (this.isBlocked()) {
      // Off the network the notice takes the whole window. A sidebar next to it would only offer
      // pages that have nothing to show, so the way on is the connect button and nothing else.
      DivWidget blocker = Widgets.panel("mcp-blocker");
      blocker.addChild(this.offNetworkPage.create(this, null));
      window.addChild(blocker);
    } else {
      window.addChild(this.createSidebar());
      window.addChild(this.compactable(
          Widgets.panel("mcp-divider", "mcp-divider-sidebar"),
          "mcp-divider-sidebar-compact"
      ));
      window.addChild(this.createContent());
    }

    this.document.addChild(window);
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);

    // A resize fires on every dragged pixel and on every GUI scale change, so the rebuild is
    // gated on the measurement actually producing a different tree -- a different mode, or one
    // row or column fewer.
    Layout resized = Layout.of(width, height);
    if (this.layout.sameAs(resized)) {
      this.layout = resized;
      return;
    }

    this.layout = resized;
    this.markDirty();
  }

  @Override
  public void onOpenScreen() {
    super.onOpenScreen();

    this.layout = Layout.current();
    this.offNetwork = this.addon.isOffNetwork();

    this.addon.api().snapshot().addListener(this.dataListener);
    this.addon.api().catalog().addListener(this.dataListener);
    this.addon.api().shop().addListener(this.dataListener);
    this.addon.api().refresh(false);
    this.lastAutoRefresh = System.currentTimeMillis();
  }

  @Override
  public void onCloseScreen() {
    this.addon.api().snapshot().removeListener(this.dataListener);
    this.addon.api().catalog().removeListener(this.dataListener);
    this.addon.api().shop().removeListener(this.dataListener);

    super.onCloseScreen();
  }

  @Override
  public void tick() {
    super.tick();

    this.tickNetwork();
    this.tickAutoRefresh();

    if (this.dirty) {
      this.dirty = false;
      this.reload();
    }
  }

  /**
   * Joining or leaving MineClub swaps the whole content area, and either can happen while the
   * window is open.
   */
  private void tickNetwork() {
    boolean off = this.addon.isOffNetwork();
    if (off == this.offNetwork) {
      return;
    }

    this.offNetwork = off;
    this.markDirty();
  }

  /**
   * Only refreshes while the window is open, and only when the user turned it on. Nothing polls in
   * the background.
   */
  private void tickAutoRefresh() {
    if (this.offNetwork || !this.addon.configuration().refreshWhileOpen().get()) {
      return;
    }

    long now = System.currentTimeMillis();
    if (now - this.lastAutoRefresh < this.autoRefreshIntervalMillis) {
      return;
    }

    this.lastAutoRefresh = now;
    this.addon.api().refresh(true);
  }

  // -------------------------------------------------------------------------------------------
  // Shell
  // -------------------------------------------------------------------------------------------

  private Widget createHeader() {
    DivWidget header = Widgets.panel("mcp-header");

    HorizontalListWidget left = Widgets.row("mcp-header-left");
    left.addEntry(Widgets.component(Brand.wordmark(), "mcp-brand"));
    left.addEntry(Widgets.i18n(this.page.titleKey(), "mcp-breadcrumb"));

    // Sample numbers are only honest while they are marked as such. Without this the window
    // shows an invented balance that reads exactly like a real one.
    if (this.addon.api().hasSampleData()) {
      Widget badge = Widgets.i18n("mineclubplus.state.demo", "mcp-demo-badge");
      badge.setHoverComponent(Component.text(
          Translations.get("mineclubplus.state.demoHint")));
      left.addEntry(badge);
    }

    header.addChild(left);

    // Pinned individually rather than lined up in a list: a list sizes itself to fit-content,
    // which left the buttons short of the edge.
    header.addChild(Widgets.text(
        "v" + this.addon.addonInfo().getVersion(),
        "mcp-version"
    ));
    header.addChild(Widgets.panel("mcp-header-separator"));
    header.addChild(this.iconButton(
        SpriteCommon.REFRESH,
        "mineclubplus.action.refresh",
        () -> this.addon.api().refresh(true)
    ).addId("mcp-refresh"));
    header.addChild(this.iconButton(
        SpriteCommon.X,
        "mineclubplus.action.close",
        this::closeWindow
    ).addId("mcp-close"));

    return header;
  }

  /**
   * A square header button. The label lives in the hover box instead of next to the icon, so the
   * header stays the same width in every language.
   */
  private ButtonWidget iconButton(Icon icon, String translationKey, Pressable pressable) {
    ButtonWidget button = ButtonWidget.icon(icon, pressable);
    button.addId("mcp-header-button");
    button.setHoverComponent(Component.text(Translations.get(translationKey)));
    return button;
  }

  private Widget createSidebar() {
    DivWidget sidebar = this.compactable(Widgets.panel("mcp-sidebar"), "mcp-sidebar-compact");

    VerticalListWidget<Widget> top = Widgets.column("mcp-sidebar-top");
    top.addChild(Widgets.i18n("mineclubplus.sidebar.navigation", "mcp-sidebar-label"));

    for (MineClubPage entry : MineClubPage.mainEntries()) {
      top.addChild(this.createNavEntry(entry));
    }
    sidebar.addChild(top);

    VerticalListWidget<Widget> bottom = Widgets.column("mcp-sidebar-bottom");
    bottom.addChild(this.createNavEntry(MineClubPage.SETTINGS));
    bottom.addChild(this.createPlayerCard());
    sidebar.addChild(bottom);

    return sidebar;
  }

  private Widget createNavEntry(MineClubPage entry) {
    boolean active = entry == this.page;

    DivWidget row = Widgets.panel("mcp-nav");
    Widgets.clickable(row, () -> this.openPage(entry));
    row.setActive(active);
    row.setHoverComponent(Component.text(Translations.get(entry.titleKey())));
    if (active) {
      row.addId("mcp-nav-active");
    }

    row.addChild(Widgets.icon(entry.icon(), "mcp-nav-icon"));
    row.addChild(Widgets.i18n(entry.titleKey(), "mcp-nav-label"));

    return row;
  }

  private Widget createPlayerCard() {
    MineClubSnapshot data = this.addon.api().data();
    PlayerProfile profile = data == null ? null : data.profile();

    DivWidget card = Widgets.panel("mcp-player-card");
    Widgets.clickable(card, () -> this.openPage(MineClubPage.SETTINGS));

    card.addChild(Widgets.icon(
        Avatars.of(
            profile == null ? null : profile.uniqueId(),
            profile == null ? null : profile.name(),
            this.addon.configuration().avatarStyle().get()
        ),
        "mcp-player-avatar"
    ));
    card.addChild(Widgets.text(profile == null ? "..." : profile.name(), "mcp-player-name"));
    card.addChild(Widgets.text(
        Ranks.displayName(profile == null ? null : profile.rank()),
        "mcp-player-rank",
        Ranks.styleId(profile == null ? null : profile.rank())
    ));

    return card;
  }

  /**
   * @return true while the very first response is still on its way, so there is nothing to draw
   */
  private boolean isPreparing() {
    return !this.offNetwork && !this.addon.api().snapshot().hasValue();
  }

  /**
   * @return true when the window shows the off-network notice instead of the dashboard. The
   * settings are exempt: they are worth reaching even when there is nothing to fetch.
   */
  private boolean isBlocked() {
    return this.offNetwork && this.page != MineClubPage.SETTINGS;
  }

  private Widget createContent() {
    DivWidget content = this.compactable(Widgets.panel("mcp-content"), "mcp-content-compact");
    if (this.layout.isStacked(this.page)) {
      content.addId("mcp-stacked");
    } else if (this.layout.isShort()) {
      // Only meaningful while the pages are still pinned -- a stacked page scrolls, so it has no
      // fixed offsets to tighten, and the two sets of rules would otherwise overlap.
      content.addId("mcp-short");
    }

    if (this.layout.isTight()) {
      content.addId("mcp-tight");
    }

    // Off the network the notice has already taken the window, so anything reaching this point
    // is a page worth drawing -- the settings, or a client that is on MineClub.
    PageRenderer renderer = this.pages.get(this.page);
    if (renderer != null) {
      content.addChild(renderer.create(this, this.addon.api().data()));
    }

    return content;
  }

  /**
   * Tags a shell region with its compact-mode id, so the sidebar and the content area shrink
   * together instead of the stylesheet having to guess.
   */
  private DivWidget compactable(DivWidget widget, String compactId) {
    if (this.layout.isCompact()) {
      widget.addId(compactId);
    }

    return widget;
  }

  // -------------------------------------------------------------------------------------------
  // State shared with the pages
  // -------------------------------------------------------------------------------------------

  public @NotNull MineClubPlusAddon addon() {
    return this.addon;
  }

  public @NotNull MineClubPage page() {
    return this.page;
  }

  /**
   * The window's own measurements. Pages ask this for their row and column counts rather than
   * re-deriving the window from the screen.
   */
  public @NotNull Layout layout() {
    return this.layout;
  }

  /**
   * @return true when the window cannot hold a side rail, so pages become one scrolling column
   */
  public boolean isStacked() {
    return this.layout.isStacked(this.page);
  }

  public void openPage(@NotNull MineClubPage target) {
    if (this.page == target) {
      return;
    }

    this.page = target;

    // A new page starts at the top; otherwise you land on page seven of a list you just
    // opened.
    this.listPage = 0;

    // Search and filter belong to the page they were typed on. The shop and the transporter
    // share both fields, and without this the shop would open filtered by something searched
    // for somewhere else.
    this.gridPage = 0;
    this.search = "";
    this.categoryFilter = ItemCategory.ALL;
    this.searchFocused = false;
    this.takeInputFocused = false;
    this.markDirty();
  }

  public @NotNull ItemCategory categoryFilter() {
    return this.categoryFilter;
  }

  public void setCategoryFilter(@NotNull ItemCategory filter) {
    if (this.categoryFilter == filter) {
      return;
    }

    this.categoryFilter = filter;
    this.gridPage = 0;
    this.markDirty();
  }

  public int gridPage() {
    return this.gridPage;
  }

  public @Nullable String selectedRecipe() {
    return this.selectedRecipe;
  }

  /** Selects the recipe whose pattern is drawn. Clicking the same one again closes it. */
  public void selectRecipe(@NotNull String key) {
    this.selectedRecipe = key.equals(this.selectedRecipe) ? null : key;
    this.markDirty();
  }

  /** @return the material selected in the shop, or {@code null} when nothing is selected */
  public @Nullable String shopItem() {
    return this.shopItem;
  }

  /** Selects an item in the shop. Clicking the same one again deselects it. */
  public void selectShopItem(@NotNull String material) {
    this.shopItem = material.equals(this.shopItem) ? null : material;
    this.markDirty();
  }

  public int shopAmount() {
    return this.shopAmount;
  }

  /** Sets the shop amount, clamped to what the server will hand over. */
  public void setShopAmount(int amount) {
    int wanted = Math.max(1, Math.min(MAX_SHOP_AMOUNT, amount));
    if (this.shopAmount == wanted) {
      return;
    }

    this.shopAmount = wanted;
    this.markDirty();
  }

  public int listPage() {
    return this.listPage;
  }

  public void setListPage(int page) {
    int wanted = Math.max(0, page);
    if (this.listPage == wanted) {
      return;
    }

    this.listPage = wanted;
    this.markDirty();
  }

  public void setGridPage(int page) {
    if (this.gridPage == page) {
      return;
    }

    this.gridPage = page;
    this.searchFocused = false;
    this.takeInputFocused = false;
    this.markDirty();
  }

  public @NotNull TransporterSort transporterSort() {
    return this.transporterSort;
  }

  public void cycleTransporterSort() {
    this.transporterSort = this.transporterSort.next();
    this.gridPage = 0;
    this.markDirty();
  }

  public @NotNull EconomyPeriod economyPeriod() {
    return this.economyPeriod;
  }

  public void setEconomyPeriod(@NotNull EconomyPeriod period) {
    if (this.economyPeriod == period) {
      return;
    }

    this.economyPeriod = period;
    this.markDirty();
  }

  public @Nullable String selectedItemId() {
    return this.selectedItemId;
  }

  public void selectItem(@Nullable String itemId) {
    this.selectedItemId = itemId;
    this.markDirty();
  }

  public @NotNull String search() {
    return this.search;
  }

  public void setSearch(@Nullable String search) {
    String normalized = search == null ? "" : search.trim();
    if (this.search.equals(normalized)) {
      return;
    }

    this.search = normalized;
    this.gridPage = 0;

    // Typing is what drives this, so the rebuilt field has to take the caret back -- and only
    // one field can hold it.
    this.searchFocused = true;
    this.takeInputFocused = false;
    this.markDirty();
  }

  /**
   * @return true when the rebuild was caused by typing, so the search field should regain focus
   */
  public boolean isSearchFocused() {
    return this.searchFocused;
  }

  /**
   * @return what the player typed in the take-this-many field, kept across rebuilds
   */
  public @NotNull String takeInput() {
    return this.takeInput;
  }

  public void setTakeInput(@Nullable String amount) {
    String normalized = amount == null ? "" : amount.trim();
    if (this.takeInput.equals(normalized)) {
      return;
    }

    this.takeInput = normalized;

    // Same as the search field: typing is what drives the rebuild, so the caret has to come back.
    this.takeInputFocused = true;
    this.searchFocused = false;
    this.markDirty();
  }

  public boolean isTakeInputFocused() {
    return this.takeInputFocused;
  }

  /**
   * Requests a rebuild on the next tick. Called from click handlers and from API callbacks, both
   * of which can arrive while the current frame is still rendering.
   */
  public void markDirty() {
    this.dirty = true;
  }

  /**
   * Runs an action that changes something on the server.
   *
   * <p>With confirmations on, LabyMod's confirm screen asks first and returns here; either way the
   * window closes afterwards so the player sees what the server answers in chat.
   *
   * @param question the confirmation prompt
   * @param action   sends the command
   */
  public void runServerAction(@NotNull Component question, @NotNull Runnable action) {
    if (!this.addon.configuration().confirmTransporterActions().get()) {
      action.run();
      this.closeWindow();
      return;
    }

    ConfirmActivity.confirm(question, confirmed -> {
      if (!Boolean.TRUE.equals(confirmed)) {
        return;
      }

      action.run();

      // The confirm screen returns to this window first, so close on the tick after that.
      this.labyAPI.minecraft().executeNextTick(this::closeWindow);
    });
  }

  /**
   * Buys in the shop.
   *
   * <p>Like {@link #runServerAction}, but the window stays open: players rarely buy a single
   * item, and reopening the menu for each one makes the shop tedious to use.
   *
   * <p>The snapshot is refreshed afterwards, because the balance on screen has just changed.
   */
  public void runShopPurchase(@NotNull Component question, @NotNull Runnable action) {
    if (!this.addon.configuration().confirmTransporterActions().get()) {
      this.purchase(action);
      return;
    }

    ConfirmActivity.confirm(question, confirmed -> {
      if (!Boolean.TRUE.equals(confirmed)) {
        return;
      }

      this.purchase(action);
    });
  }

  private void purchase(Runnable action) {
    action.run();
    this.addon.api().refresh(true);
  }

  public void closeWindow() {
    this.labyAPI.minecraft().minecraftWindow().displayScreen((ScreenInstance) null);
  }
}
