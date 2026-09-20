package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.util.Translations;
import dk.mineclub.plus.core.api.model.ItemCategory;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.api.model.TransporterItem;
import dk.mineclub.plus.core.api.model.TransporterSnapshot;
import dk.mineclub.plus.core.MineClubPlusConfiguration;
import dk.mineclub.plus.core.service.ServerCommands;
import dk.mineclub.plus.core.ui.Layout;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.widget.Cards;
import dk.mineclub.plus.core.ui.widget.ItemIcons;
import dk.mineclub.plus.core.ui.widget.Widgets;
import dk.mineclub.plus.core.util.Formats;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.screen.key.Key;
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
 * The transporter: a filterable grid of everything inside it, and a detail panel with the
 * take-out and sell actions for the selected stack.
 *
 * <p>There is no capacity figure. The transporter holds an unlimited amount, so a used-of-total
 * reading would be inventing a ceiling that does not exist.
 *
 * <p>The grid is paged rather than scrolled. Minecraft flushes rendered items in a pass of their
 * own, after the scissor around a scrolling area has already been popped, so a half scrolled row
 * draws its blocks over whatever sits above the list. Showing a whole page at a time removes the
 * overlap at its source, and it also keeps the widget count flat no matter how much the
 * transporter holds.
 *
 * <p>How many tiles that page holds comes from {@link Layout}, which measures the window rather
 * than the screen -- the tile metrics live there next to the rest of the shell geometry.
 */
public final class TransporterPage implements PageRenderer {

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    TransporterSnapshot transporter = data == null ? null : data.transporter();
    if (transporter == null) {
      return Cards.placeholder("mineclubplus.state.loading");
    }

    List<TransporterItem> visible = this.filter(activity, transporter);

    // Stacked: the grid is pinned to the floor and the rest scrolls above it, the same split the
    // overview uses. A tile draws a real item block, and Minecraft draws those after any scissor
    // has been popped -- inside a scroll they land outside the window. Pinning the grid is what
    // lets the small layout show the same real blocks as the wide one.
    if (activity.isStacked()) {
      DivWidget stacked = Widgets.panel("mcp-transporter-stacked");

      VerticalListWidget<Widget> above = Widgets.column("mcp-page");
      above.addChild(this.createFilters(activity, visible));
      above.addChild(this.createDetailsCard(activity, transporter));
      stacked.addChild(Cards.scroll(
          above,
          "mcp-transporter-stacked-scroll",
          "mcp-transporter-stacked-scroll-" + activity.layout().stackedGridRows()
      ));

      stacked.addChild(this.createGrid(activity, visible));

      return stacked;
    }

    DivWidget page = Widgets.panel("mcp-transporter");
    page.addChild(this.createHeader(activity, transporter));
    page.addChild(this.createFilters(activity, visible));
    page.addChild(this.createGrid(activity, visible));
    page.addChild(this.createDetailsColumn(activity, transporter));

    return page;
  }

  // -------------------------------------------------------------------------------------------

  private List<TransporterItem> filter(
      MineClubPlusActivity activity,
      TransporterSnapshot transporter
  ) {
    ItemCategory category = activity.categoryFilter();
    String search = activity.search().toLowerCase(Locale.ROOT);

    List<TransporterItem> visible = new ArrayList<>();
    for (TransporterItem item : transporter.items()) {
      if (category != ItemCategory.ALL && item.category() != category) {
        continue;
      }

      if (!search.isEmpty()
          && !item.displayName().toLowerCase(Locale.ROOT).contains(search)
          && !item.id().toLowerCase(Locale.ROOT).contains(search)) {
        continue;
      }

      visible.add(item);
    }

    visible.sort(activity.transporterSort().comparator());
    return visible;
  }

  private Widget createHeader(MineClubPlusActivity activity, TransporterSnapshot transporter) {
    DivWidget header = Widgets.panel("mcp-page-header");

    header.addChild(Widgets.i18n("mineclubplus.page.transporter.title", "mcp-page-title"));
    header.addChild(Widgets.i18nArgs(
        "mineclubplus.transporter.subtitle",
        new Object[]{transporter.items().size()},
        "mcp-page-subtitle"
    ));

    HorizontalListWidget actions = Widgets.row("mcp-page-actions");

    TextFieldWidget search = new TextFieldWidget();
    search.addId("mcp-search");
    search.setText(activity.search());
    search.placeholder(Component.text(
        Translations.get("mineclubplus.transporter.searchPlaceholder")
    ));
    // Filters as you type; the window rebuilds on the next tick and hands focus back.
    search.updateListener(activity::setSearch);
    search.submitHandler(activity::setSearch);
    if (activity.isSearchFocused()) {
      search.setFocused(true);

      // setText leaves the caret at index 0, so without this every keystroke would be inserted
      // in front of the last one and the word would come out backwards.
      search.setCursorAtEnd();
    }
    actions.addEntry(search);

    actions.addEntry(Widgets.i18nButton(
        activity.transporterSort().translationKey(),
        activity::cycleTransporterSort,
        "mcp-ghost-button"
    ));
    actions.addEntry(Widgets.i18nButton(
        "mineclubplus.transporter.putAll",
        () -> activity.runServerAction(
            Component.text(Translations.get("mineclubplus.confirm.putAll")),
            ServerCommands::putAll
        ),
        "mcp-accent-button"
    ));
    header.addChild(actions);

    return header;
  }

  private Widget createFilters(MineClubPlusActivity activity, List<TransporterItem> visible) {
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

    double value = 0.0D;
    for (TransporterItem item : visible) {
      value += item.totalValue();
    }

    // Built like the capacity strip above it -- caption, then number, on one pill -- so the two
    // rows read as a pair. The matching item count lives in the pager under the grid.
    DivWidget total = Widgets.panel("mcp-filters-total");
    total.addChild(Widgets.i18n("mineclubplus.transporter.totalValue", "mcp-filters-label"));
    total.addChild(Widgets.text(Formats.compact(value), "mcp-filters-value"));
    filters.addChild(total);

    return filters;
  }

  private Widget createGrid(MineClubPlusActivity activity, List<TransporterItem> items) {
    int columns = activity.layout().gridColumns();
    int perPage = columns * (activity.isStacked()
        ? activity.layout().stackedGridRows()
        : activity.layout().gridRows());
    int pages = Math.max(1, (items.size() + perPage - 1) / perPage);
    int page = Math.min(Math.max(activity.gridPage(), 0), pages - 1);

    VerticalListWidget<Widget> grid = Widgets.column("mcp-grid");
    if (items.isEmpty()) {
      grid.addChild(Cards.placeholder("mineclubplus.state.noMatches"));
      return gridArea(activity, grid, null);
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

    int remainder = (to - from) % columns;
    if (remainder != 0) {
      for (int filler = remainder; filler < columns; filler++) {
        row.addEntry(Widgets.panel("mcp-tile-filler"));
      }
    }

    return gridArea(activity, grid, this.createPager(activity, page, pages, items.size()));
  }

  /**
   * Wraps the grid and its pager in whatever the current layout needs.
   *
   * <p>A list, not a box with the two pinned to opposite edges. Pinning the pager to the floor
   * meant that one row too many drew its tiles straight over it; a list can only ever place the
   * pager after the last row. Wide, the box fills what is left of the column; stacked, it is
   * pinned to the floor at the exact height its row count needs -- see
   * {@link Layout#stackedGridRows()}.
   */
  private static Widget gridArea(
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

  private Widget createPager(
      MineClubPlusActivity activity,
      int page,
      int pages,
      int total
  ) {
    // A list, so the arrows sit beside the label instead of being nudged onto it by a margin.
    HorizontalListWidget pager = Widgets.row("mcp-pager");

    ButtonWidget previous = Widgets.button("<", () -> activity.setGridPage(page - 1),
        "mcp-pager-button");
    previous.setEnabled(page > 0);
    pager.addEntry(previous);

    pager.addEntry(Widgets.i18nArgs(
        "mineclubplus.transporter.pageOf",
        new Object[]{page + 1, pages, Formats.number(total)},
        "mcp-pager-label"
    ));

    ButtonWidget next = Widgets.button(">", () -> activity.setGridPage(page + 1),
        "mcp-pager-button");
    next.setEnabled(page < pages - 1);
    pager.addEntry(next);

    return pager;
  }

  private Widget createTile(MineClubPlusActivity activity, TransporterItem item) {
    boolean selected = item.id().equals(activity.selectedItemId());

    DivWidget tile = Widgets.panel("mcp-tile");
    Widgets.clickable(tile, () -> activity.selectItem(item.id()));
    tile.setActive(selected);
    tile.setHoverComponent(Component.text(
        item.displayName() + "  -  " + Formats.number(item.totalValue())
    ));
    if (selected) {
      tile.addId("mcp-tile-selected");
    }

    tile.addChild(Widgets.text(Formats.compact(item.amount()), "mcp-tile-amount"));
    tile.addChild(ItemIcons.of(item.id(), "mcp-tile-icon"));
    tile.addChild(Widgets.component(
        ItemIcons.nameOf(item.id(), item.displayName()),
        "mcp-tile-name"
    ));

    return tile;
  }

  // -------------------------------------------------------------------------------------------

  /**
   * The detail column: a fixed head with the block and the name, and the figures scrolling under
   * it. The head is deliberately outside the scroll -- a rendered item cannot be clipped by one.
   */
  private Widget createDetailsColumn(
      MineClubPlusActivity activity,
      TransporterSnapshot transporter
  ) {
    DivWidget column = Widgets.panel("mcp-details-column");

    TransporterItem item = this.findSelected(activity, transporter);
    if (item != null) {
      DivWidget head = Widgets.panel("mcp-details-head");
      head.addChild(ItemIcons.of(item.id(), "mcp-details-icon"));
      head.addChild(Widgets.component(
          ItemIcons.nameOf(item.id(), item.displayName()),
          "mcp-details-name"
      ));
      column.addChild(head);
    }

    column.addChild(Cards.scroll(
        this.createDetailsCard(activity, transporter),
        "mcp-details-scroll",
        item == null ? "mcp-details-scroll-full" : "mcp-details-scroll-below-head"
    ));

    return column;
  }

  private VerticalListWidget<Widget> createDetailsCard(
      MineClubPlusActivity activity,
      TransporterSnapshot transporter
  ) {
    VerticalListWidget<Widget> panel = Widgets.column("mcp-details");

    TransporterItem item = this.findSelected(activity, transporter);
    if (item == null) {
      panel.addChild(Cards.placeholder("mineclubplus.transporter.selectHint"));
      panel.addChild(this.createDistribution(transporter));
      return panel;
    }

    // Amount and stacks read as rows like every other figure, rather than as a cramped line
    // squeezed under the name.
    panel.addChild(Cards.translatedRow(
        "mineclubplus.transporter.amount",
        Formats.number(item.amount())
    ));
    panel.addChild(Cards.translatedRow(
        "mineclubplus.transporter.stacks",
        Formats.number(item.stacks())
    ));
    panel.addChild(Cards.translatedRow(
        "mineclubplus.transporter.unitValue",
        Formats.number(item.unitValue())
    ));
    panel.addChild(Cards.translatedRow(
        "mineclubplus.transporter.stackValue",
        Formats.number(item.totalValue())
    ));
    panel.addChild(Cards.widgetRow(
        "mineclubplus.transporter.change24h",
        Cards.change(item.change24h())
    ));
    panel.addChild(Cards.translatedRow(
        "mineclubplus.transporter.category",
        Translations.get(item.category().translationKey())
    ));

    long taken = Math.min(activity.addon().configuration().quickAmount().get(), item.amount());
    panel.addChild(Widgets.i18nArgs(
        "mineclubplus.transporter.take",
        new Object[]{Formats.number(taken)},
        "mcp-details-action-label"
    ));
    panel.addChild(Widgets.i18nButton(
        "mineclubplus.transporter.takeButton",
        () -> activity.runServerAction(
            Component.text(Translations.get(
                "mineclubplus.confirm.take",
                Formats.number(taken),
                item.displayName()
            )),
            () -> ServerCommands.takeOut(item.id(), taken)
        ),
        "mcp-accent-button",
        "mcp-details-button"
    ));
    panel.addChild(Widgets.i18nButton(
        "mineclubplus.transporter.takeAllButton",
        () -> activity.runServerAction(
            Component.text(Translations.get(
                "mineclubplus.confirm.takeAll",
                item.displayName()
            )),
            () -> ServerCommands.takeAll(item.id())
        ),
        "mcp-ghost-button",
        "mcp-details-button"
    ));

    this.addCustomAmount(panel, activity, item);
    this.addKeybindRow(panel, activity, item);

    panel.addChild(Widgets.i18nButton(
        "mineclubplus.transporter.openButton",
        () -> {
          ServerCommands.openInGame();
          activity.closeWindow();
        },
        "mcp-ghost-button",
        "mcp-details-button"
    ));

    panel.addChild(this.createDistribution(transporter));

    return panel;
  }

  /**
   * The field for a custom amount, with its button directly below.
   *
   * <p>The field accepts digits only, and the button stays disabled until it holds a usable
   * number, so an empty field cannot send a command that would fail anyway. The amount never
   * exceeds what the transporter holds.
   */
  private void addCustomAmount(
      VerticalListWidget<Widget> panel,
      MineClubPlusActivity activity,
      TransporterItem item
  ) {
    TextFieldWidget field = new TextFieldWidget();
    field.addId("mcp-input");
    field.setText(activity.takeInput());
    field.placeholder(Component.text(
        Translations.get("mineclubplus.transporter.customPlaceholder")
    ));
    field.maximalLength(6);
    field.validator(text -> text.chars().allMatch(Character::isDigit));
    field.updateListener(activity::setTakeInput);
    field.submitHandler(activity::setTakeInput);
    if (activity.isTakeInputFocused()) {
      field.setFocused(true);

      // As with the search field: setText leaves the caret at index 0, which would type the
      // number backwards.
      field.setCursorAtEnd();
    }

    // No label: the row put the name on top of the field's own placeholder, and two texts on
    // top of each other are worse than no label. The placeholder already says what it is.
    field.addId("mcp-details-amount");
    panel.addChild(field);

    long wanted = parseAmount(activity.takeInput());
    long custom = Math.min(wanted, item.amount());

    ButtonWidget take = Widgets.i18nButton(
        "mineclubplus.transporter.takeCustomButton",
        () -> {
          if (custom < 1L) {
            return;
          }

          activity.runServerAction(
              Component.text(Translations.get(
                  "mineclubplus.confirm.take",
                  Formats.number(custom),
                  item.displayName()
              )),
              () -> ServerCommands.takeOut(item.id(), custom)
          );
        },
        "mcp-accent-button",
        "mcp-details-button"
    );
    take.setEnabled(custom >= 1L);
    panel.addChild(take);
  }

  /**
   * The button that binds the item to the shortcut key, and the line explaining what the key
   * does.
   *
   * <p>The key itself and the amount live in the settings; only the item is chosen here, which is
   * where the player is already looking at it.
   */
  private void addKeybindRow(
      VerticalListWidget<Widget> panel,
      MineClubPlusActivity activity,
      TransporterItem item
  ) {
    MineClubPlusConfiguration configuration = activity.addon().configuration();
    String bound = configuration.takeKeyItem().get();
    boolean isBound = bound != null && bound.equalsIgnoreCase(item.id());

    Key key = configuration.takeKey().get();
    boolean hasKey = key != null && key != Key.NONE;

    panel.addChild(Widgets.i18nButton(
        isBound ? "mineclubplus.transporter.unbindButton" : "mineclubplus.transporter.bindButton",
        () -> {
          configuration.takeKeyItem().set(isBound ? "" : item.id().toUpperCase(Locale.ROOT));
          activity.addon().save();
          activity.markDirty();
        },
        "mcp-ghost-button",
        "mcp-details-button"
    ));

    if (isBound) {
      String amount = Formats.number(configuration.takeKeyAmount().get());

      // The strings take their placeholders in order, so the one without a key gets only
      // the amount.
      panel.addChild(hasKey
          ? Widgets.i18nArgs(
              "mineclubplus.transporter.bindHint",
              new Object[]{key.getName(), amount},
              "mcp-details-action-label")
          : Widgets.i18nArgs(
              "mineclubplus.transporter.bindNoKey",
              new Object[]{amount},
              "mcp-details-action-label"));
    }
  }

  /**
   * @return the number the player typed, or 0 when the field is empty or not a usable number
   */
  private static long parseAmount(String raw) {
    String trimmed = raw == null ? "" : raw.trim();
    if (trimmed.isEmpty()) {
      return 0L;
    }

    try {
      return Math.max(0L, Long.parseLong(trimmed));
    } catch (NumberFormatException exception) {
      return 0L;
    }
  }

  private @Nullable TransporterItem findSelected(
      MineClubPlusActivity activity,
      TransporterSnapshot transporter
  ) {
    String selected = activity.selectedItemId();
    if (selected == null) {
      return null;
    }

    for (TransporterItem item : transporter.items()) {
      if (item.id().equals(selected)) {
        return item;
      }
    }

    return null;
  }

  /**
   * Share of the transporter value per category, so the player sees at a glance what is filling
   * their slots.
   */
  private Widget createDistribution(TransporterSnapshot transporter) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.transporter.distribution");

    double total = transporter.totalValue();
    if (total <= 0.0D) {
      card.addChild(Cards.placeholder("mineclubplus.state.empty"));
      return card;
    }

    for (ItemCategory category : ItemCategory.values()) {
      if (category == ItemCategory.ALL) {
        continue;
      }

      double value = 0.0D;
      for (TransporterItem item : transporter.items()) {
        if (item.category() == category) {
          value += item.totalValue();
        }
      }

      if (value <= 0.0D) {
        continue;
      }

      card.addChild(Cards.translatedRow(
          category.translationKey(),
          Math.round(value / total * 100.0D) + " %"
      ));
    }

    return card;
  }
}
