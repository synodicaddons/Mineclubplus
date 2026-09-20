package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.api.VanillaCatalog;
import dk.mineclub.plus.core.api.model.ClientShopResponse;
import dk.mineclub.plus.core.api.model.EconomySnapshot;
import dk.mineclub.plus.core.api.model.ItemCategory;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.service.ServerCommands;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.widget.Cards;
import dk.mineclub.plus.core.ui.widget.ItemIcons;
import dk.mineclub.plus.core.ui.widget.Pager;
import dk.mineclub.plus.core.ui.widget.States;
import dk.mineclub.plus.core.ui.widget.Widgets;
import dk.mineclub.plus.core.util.Formats;
import dk.mineclub.plus.core.util.Translations;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The shop: what the console sells on the market square, bought from here instead of at the sign.
 *
 * <p>Stock and prices come from those same signs, so no price is invented here. The client only
 * states what and how much; the server recomputes the cost when it takes the coins, which is what
 * keeps a modified client from setting its own price.
 *
 * <p>The page reuses the transporter's layout and stylesheet ids: a paged grid with a detail
 * column beside it. The two pages do the same thing with different stock and should read alike.
 */
public final class ShopPage implements PageRenderer {

  /** How far {@code +} and {@code -} move the amount. */
  private static final int STEP = 1;

  /** One stack, for the button that buys a whole one. */
  private static final int STACK = 64;

  /** Fixed amounts below the stepper, so sixty clicks are never needed. */
  private static final int[] QUICK = {1, 16, 64};

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    // Fetched separately from the rest: prices live on signs and rarely change.
    activity.addon().api().shop().refresh(false);

    ClientShopResponse shop = activity.addon().api().shop().get();
    if (shop == null) {
      return activity.addon().api().shop().error() == null
          ? States.loading()
          : States.failed(() -> activity.addon().api().shop().refresh(true));
    }

    if (!shop.ready()) {
      // The server answered, but the plugin has not read the market square yet.
      return States.message(
          "mineclubplus.shop.empty",
          () -> activity.addon().api().shop().refresh(true),
          false
      );
    }

    List<ClientShopResponse.Item> visible = this.filter(activity, shop);

    // Narrow window: the grid is pinned to the floor and the rest scrolls above it, as on the
    // transporter. Tiles render real blocks, and no scroll can clip those.
    if (activity.isStacked()) {
      DivWidget stacked = Widgets.panel("mcp-transporter-stacked");

      VerticalListWidget<Widget> above = Widgets.column("mcp-page");
      above.addChild(this.createFilters(activity, data));
      above.addChild(this.createDetails(activity, visible));
      stacked.addChild(Cards.scroll(
          above,
          "mcp-transporter-stacked-scroll",
          "mcp-transporter-stacked-scroll-" + activity.layout().stackedGridRows()
      ));

      stacked.addChild(this.createGrid(activity, visible));

      return stacked;
    }

    DivWidget page = Widgets.panel("mcp-transporter");
    page.addChild(this.createHeader(activity, visible));
    page.addChild(this.createFilters(activity, data));
    page.addChild(this.createGrid(activity, visible));
    page.addChild(this.createDetailsColumn(activity, visible));

    return page;
  }

  // -------------------------------------------------------------------------------------------

  private List<ClientShopResponse.Item> filter(
      MineClubPlusActivity activity,
      ClientShopResponse shop
  ) {
    ItemCategory category = activity.categoryFilter();
    String search = activity.search().toLowerCase(Locale.ROOT);

    List<ClientShopResponse.Item> visible = new ArrayList<>();
    for (ClientShopResponse.Item item : shop.items()) {
      String material = item.material();
      if (material == null || material.isEmpty()) {
        continue;
      }

      // The category is derived client side from the material name, the same way the sample
      // transporter derives it before the server has answered.
      if (category != ItemCategory.ALL
          && VanillaCatalog.categoryOf(material.toLowerCase(Locale.ROOT)) != category) {
        continue;
      }

      String name = item.displayName() == null ? material : item.displayName();
      if (!search.isEmpty()
          && !name.toLowerCase(Locale.ROOT).contains(search)
          && !material.toLowerCase(Locale.ROOT).contains(search)) {
        continue;
      }

      visible.add(item);
    }

    return visible;
  }

  private Widget createHeader(
      MineClubPlusActivity activity,
      List<ClientShopResponse.Item> visible
  ) {
    DivWidget header = Widgets.panel("mcp-page-header");

    header.addChild(Widgets.i18n("mineclubplus.page.shop.title", "mcp-page-title"));
    header.addChild(Widgets.i18nArgs(
        "mineclubplus.shop.subtitle",
        new Object[]{visible.size()},
        "mcp-page-subtitle"
    ));

    HorizontalListWidget actions = Widgets.row("mcp-page-actions");

    TextFieldWidget search = new TextFieldWidget();
    search.addId("mcp-search");
    search.setText(activity.search());
    search.placeholder(Component.text(
        Translations.get("mineclubplus.shop.searchPlaceholder")
    ));
    search.updateListener(activity::setSearch);
    search.submitHandler(activity::setSearch);
    if (activity.isSearchFocused()) {
      search.setFocused(true);

      // As on the transporter: setText leaves the caret at index 0, which would type the word
      // backwards.
      search.setCursorAtEnd();
    }
    actions.addEntry(search);

    header.addChild(actions);

    return header;
  }

  /** The category chips, and what the player has to spend. */
  private Widget createFilters(MineClubPlusActivity activity, @Nullable MineClubSnapshot data) {
    DivWidget filters = Widgets.panel("mcp-filters");

    HorizontalListWidget chips = Widgets.row("mcp-chips");
    for (ItemCategory category : ItemCategory.values()) {
      boolean active = activity.categoryFilter() == category;

      ComponentWidget chip = Widgets.i18n(category.translationKey(), "mcp-filter-chip");
      Widgets.clickable(chip, () -> activity.setCategoryFilter(category));
      chip.setActive(active);
      if (active) {
        chip.addId("mcp-filter-chip-active");
      }

      chips.addEntry(chip);
    }
    filters.addChild(chips);

    // The balance sits where the transporter shows its total value: in a shop that is the
    // figure being traded against. It comes from the last snapshot, so it trails a purchase by
    // one refresh.
    EconomySnapshot economy = data == null ? null : data.economy();

    DivWidget balance = Widgets.panel("mcp-filters-total");
    balance.addChild(Widgets.i18n("mineclubplus.shop.balance", "mcp-filters-label"));
    balance.addChild(Widgets.text(
        economy == null ? "-" : Formats.compact(economy.balance()),
        "mcp-filters-value"
    ));
    filters.addChild(balance);

    return filters;
  }

  // -------------------------------------------------------------------------------------------

  private Widget createGrid(
      MineClubPlusActivity activity,
      List<ClientShopResponse.Item> items
  ) {
    int columns = activity.layout().gridColumns();
    int perPage = columns * (activity.isStacked()
        ? activity.layout().stackedGridRows()
        : activity.layout().gridRows());
    int pages = Math.max(1, (items.size() + perPage - 1) / perPage);
    int page = Math.min(Math.max(activity.gridPage(), 0), pages - 1);

    VerticalListWidget<Widget> grid = Widgets.column("mcp-grid");
    if (items.isEmpty()) {
      grid.addChild(Cards.placeholder("mineclubplus.state.noMatches"));
      return this.gridArea(activity, grid, null);
    }

    int from = page * perPage;
    int to = Math.min(from + perPage, items.size());

    HorizontalListWidget row = null;
    for (int index = from; index < to; index++) {
      if ((index - from) % columns == 0) {
        row = Widgets.row("mcp-grid-row");
        grid.addChild(row);
      }

      row.addEntry(this.createTile(activity, items.get(index)));
    }

    // The last row is padded out so the tiles stay aligned with the rows above it.
    int remainder = (to - from) % columns;
    if (remainder != 0) {
      for (int filler = remainder; filler < columns; filler++) {
        row.addEntry(Widgets.panel("mcp-tile-filler"));
      }
    }

    return this.gridArea(
        activity,
        grid,
        Pager.create(page, pages, items.size(), activity::setGridPage)
    );
  }

  /** The grid and its pager, in whatever area the window size leaves them. */
  private Widget gridArea(
      MineClubPlusActivity activity,
      VerticalListWidget<Widget> grid,
      @Nullable Widget pager
  ) {
    VerticalListWidget<Widget> area = activity.isStacked()
        ? Widgets.column("mcp-grid-pinned",
            "mcp-grid-pinned-" + activity.layout().stackedGridRows())
        : Widgets.column("mcp-grid-area");
    area.addChild(grid);
    if (pager != null) {
      area.addChild(pager);
    }

    return area;
  }

  private Widget createTile(MineClubPlusActivity activity, ClientShopResponse.Item item) {
    String material = item.material() == null ? "" : item.material();
    String name = item.displayName() == null ? material : item.displayName();
    boolean selected = material.equals(activity.shopItem());

    DivWidget tile = Widgets.panel("mcp-tile");
    Widgets.clickable(tile, () -> activity.selectShopItem(material));
    tile.setActive(selected);
    tile.setHoverComponent(Component.text(
        name + "  -  " + Translations.get(
            "mineclubplus.shop.each",
            Formats.number(item.unitPrice())
        )
    ));
    if (selected) {
      tile.addId("mcp-tile-selected");
    }

    // The price sits where the transporter shows the amount: it is what one picks by here.
    tile.addChild(Widgets.text(Formats.compact(item.unitPrice()), "mcp-tile-amount"));
    tile.addChild(ItemIcons.of(material, "mcp-tile-icon"));
    tile.addChild(Widgets.component(ItemIcons.nameOf(material, name), "mcp-tile-name"));

    return tile;
  }

  // -------------------------------------------------------------------------------------------

  private Widget createDetailsColumn(
      MineClubPlusActivity activity,
      List<ClientShopResponse.Item> visible
  ) {
    DivWidget column = Widgets.panel("mcp-details-column");

    ClientShopResponse.Item item = this.findSelected(activity, visible);
    if (item != null) {
      String material = item.material() == null ? "" : item.material();
      String name = item.displayName() == null ? material : item.displayName();

      DivWidget head = Widgets.panel("mcp-details-head");
      head.addChild(ItemIcons.of(material, "mcp-details-icon"));
      head.addChild(Widgets.component(ItemIcons.nameOf(material, name), "mcp-details-name"));
      column.addChild(head);
    }

    column.addChild(Cards.scroll(
        this.createDetails(activity, visible),
        "mcp-details-scroll",
        item == null ? "mcp-details-scroll-full" : "mcp-details-scroll-below-head"
    ));

    return column;
  }

  /**
   * The selected item: its price, the amount and the two buy buttons.
   *
   * <p>No rendered blocks in here. The head that shows the block sits outside the scroll, because
   * a rendered item cannot be clipped by one.
   */
  private VerticalListWidget<Widget> createDetails(
      MineClubPlusActivity activity,
      List<ClientShopResponse.Item> visible
  ) {
    VerticalListWidget<Widget> panel = Widgets.column("mcp-details");

    ClientShopResponse.Item item = this.findSelected(activity, visible);
    if (item == null) {
      panel.addChild(Cards.placeholder("mineclubplus.shop.selectHint"));
      return panel;
    }

    String material = item.material() == null ? "" : item.material();
    String name = item.displayName() == null ? material : item.displayName();
    int amount = activity.shopAmount();

    panel.addChild(Cards.translatedRow(
        "mineclubplus.shop.unitPrice",
        Formats.number(item.unitPrice())
    ));

    // The sign's own batch is shown because the cost is derived from it: "3 for 1" and
    // "1 for 0.33" do not round the same way.
    panel.addChild(Cards.translatedRow(
        "mineclubplus.shop.signPrice",
        Formats.number(item.amount()) + "x " + Formats.number(item.price())
    ));

    if (item.sellPrice() != null) {
      panel.addChild(Cards.translatedRow(
          "mineclubplus.shop.sellPrice",
          Formats.number(item.sellPrice())
      ));
    }

    panel.addChild(this.createStepper(activity, amount));
    panel.addChild(this.createQuickAmounts(activity));

    panel.addChild(Cards.translatedRow(
        "mineclubplus.shop.total",
        Formats.number(item.total(amount))
    ));

    panel.addChild(Widgets.i18nButton(
        "mineclubplus.shop.buy",
        () -> this.buy(activity, material, name, amount, item.total(amount)),
        "mcp-accent-button",
        "mcp-details-button"
    ));

    panel.addChild(Widgets.i18nButton(
        "mineclubplus.shop.buyStack",
        () -> this.buy(activity, material, name, STACK, item.total(STACK)),
        "mcp-ghost-button",
        "mcp-details-button"
    ));

    return panel;
  }

  /** Minus, the amount and plus on one row. */
  private Widget createStepper(MineClubPlusActivity activity, int amount) {
    HorizontalListWidget stepper = Widgets.row("mcp-stepper");

    ButtonWidget less = Widgets.button("-", () -> activity.setShopAmount(amount - STEP),
        "mcp-stepper-button");
    less.setEnabled(amount > 1);
    stepper.addEntry(less);

    stepper.addEntry(Widgets.text(Formats.number(amount), "mcp-stepper-value"));

    ButtonWidget more = Widgets.button("+", () -> activity.setShopAmount(amount + STEP),
        "mcp-stepper-button");
    more.setEnabled(amount < MineClubPlusActivity.MAX_SHOP_AMOUNT);
    stepper.addEntry(more);

    return stepper;
  }

  private Widget createQuickAmounts(MineClubPlusActivity activity) {
    HorizontalListWidget quick = Widgets.row("mcp-stepper");
    for (int amount : QUICK) {
      quick.addEntry(Widgets.button(
          Formats.number(amount),
          () -> activity.setShopAmount(amount),
          "mcp-stepper-quick"
      ));
    }

    return quick;
  }

  /**
   * Sends the purchase.
   *
   * <p>The window stays open. Players rarely buy a single item, and closing after every purchase
   * would mean walking back through the menu for each one.
   */
  private void buy(
      MineClubPlusActivity activity,
      String material,
      String name,
      int amount,
      double total
  ) {
    activity.runShopPurchase(
        Component.text(Translations.get(
            "mineclubplus.confirm.buy",
            Formats.number(amount),
            name,
            Formats.number(total)
        )),
        () -> ServerCommands.buyFromShop(material, amount)
    );
  }

  private @Nullable ClientShopResponse.Item findSelected(
      MineClubPlusActivity activity,
      List<ClientShopResponse.Item> visible
  ) {
    String selected = activity.shopItem();
    if (selected == null) {
      return null;
    }

    for (ClientShopResponse.Item item : visible) {
      if (selected.equals(item.material())) {
        return item;
      }
    }

    return null;
  }
}
