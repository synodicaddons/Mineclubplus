package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.api.model.ClientCatalogResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reference: machine prices and the server's own crafting recipes.
 *
 * <p>Machines are bought rather than crafted, so they are listed with their price and the block
 * they place. Recipes sit on a shelf along the bottom; selecting one shows its pattern.
 *
 * <p>The page pages rather than scrolls. Tiles render real item blocks, and Minecraft flushes
 * those after the scissor around a scrolling area has been popped, so a half scrolled row would
 * draw outside the window.
 */
public final class MaterialsPage implements PageRenderer {

  /** An empty slot in the pattern. Written as a space in the configuration. */
  private static final char EMPTY = ' ';

  /**
   * One row of tiles, no more.
   *
   * <p>The shelf is pinned to the bottom at a height the stylesheet fixes ({@code .mcp-shelf}),
   * so its tiles cannot push themselves past the window edge. A second row would take the space
   * the machines need.
   */
  private static final int RECIPE_ROWS = 1;

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    // Fetched separately from the rest: the catalog only changes when the server does.
    activity.addon().api().catalog().refresh(false);

    ClientCatalogResponse catalog = activity.addon().api().catalog().get();
    if (catalog == null) {
      return activity.addon().api().catalog().error() == null
          ? States.loading()
          : States.failed(() -> activity.addon().api().catalog().refresh(true));
    }

    if (!catalog.ready()) {
      // The server answered, but the plugin has not written the lists yet. Retrying helps
      // when the server has only just started.
      return States.message(
          "mineclubplus.materials.empty",
          () -> activity.addon().api().catalog().refresh(true),
          false
      );
    }

    ClientCatalogResponse.Recipe selected = this.findSelected(activity, catalog);
    if (selected != null) {
      // One at a time. With the list below the recipe there was room for neither it nor a
      // way back out of what had just been opened.
      return this.createDetail(activity, selected);
    }

    DivWidget frame = Widgets.panel("mcp-paged");
    List<ClientCatalogResponse.Recipe> recipes = catalog.recipes();

    // Tiles when they fit on a single row, which lets each machine show its real block. The
    // card is pinned: a rendered block inside a scroll draws past the window, because Minecraft
    // renders items after the scissor has been popped.
    if (activity.layout().shelfColumns() >= catalog.machines().size()
        && !catalog.machines().isEmpty()
        && !activity.layout().isShort()) {
      frame.addChild(this.createMachineTiles(activity, catalog));
    } else {
      // Too narrow for a row of tiles, so the machines fall back to name and price inside a
      // scroll, where a rendered block must not go.
      VerticalListWidget<Widget> page = Widgets.column("mcp-page");
      page.addChild(this.createMachines(activity, catalog));
      frame.addChild(recipes.isEmpty()
          ? Cards.scroll(page)
          : Cards.scroll(page, "mcp-shelf-above"));
    }

    if (!recipes.isEmpty()) {
      frame.addChild(this.createShelf(activity, recipes));
    }

    return frame;
  }

  /**
   * Machines as tiles, each with its block and price.
   *
   * <p>Clicking one opens the in-game machine shop. There is no command that buys a specific
   * machine, so the menu is the only route.
   */
  private Widget createMachineTiles(
      MineClubPlusActivity activity,
      ClientCatalogResponse catalog
  ) {
    VerticalListWidget<Widget> card = Cards.card(
        "mineclubplus.materials.machines",
        "mcp-machines"
    );

    // The access price belongs in the hint line: without it no machine can be used, and a row
    // of its own would take the height the tiles need.
    card.addChild(Widgets.i18nArgs(
        "mineclubplus.materials.machineHint",
        new Object[]{Formats.number(catalog.accessPrice())},
        "mcp-settings-hint"
    ));

    HorizontalListWidget row = Widgets.row("mcp-grid-row");
    for (ClientCatalogResponse.Machine machine : catalog.machines()) {
      row.addEntry(this.createMachineTile(activity, machine));
    }
    card.addChild(row);

    return card;
  }

  private Widget createMachineTile(
      MineClubPlusActivity activity,
      ClientCatalogResponse.Machine machine
  ) {
    String block = machine.block() == null ? "" : machine.block();
    String name = machine.displayName() == null ? "" : machine.displayName();

    DivWidget tile = Widgets.panel("mcp-tile");
    Widgets.clickable(tile, () -> {
      ServerCommands.openMachineShop();
      activity.closeWindow();
    });
    tile.setHoverComponent(Component.text(name + "  -  " + Formats.number(machine.price())));

    tile.addChild(Widgets.text(Formats.compact(machine.price()), "mcp-tile-amount"));
    if (!block.isEmpty()) {
      tile.addChild(ItemIcons.of(block, "mcp-tile-icon"));
    }
    tile.addChild(Widgets.text(name, "mcp-tile-name"));

    return tile;
  }

  /** Machines as name and price, for windows with no room for the tiles. */
  private Widget createMachines(
      MineClubPlusActivity activity,
      ClientCatalogResponse catalog
  ) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.materials.machines");

    // Machines have no recipe; without this line players look for a pattern that does not
    // exist.
    card.addChild(Widgets.i18n("mineclubplus.materials.bought", "mcp-settings-hint"));

    if (catalog.accessPrice() > 0.0D) {
      card.addChild(Cards.translatedRow(
          "mineclubplus.materials.access",
          Formats.number(catalog.accessPrice())
      ));
    }

    // Name and price, no block: this list scrolls, and a scrolled item renders past the
    // window.
    for (ClientCatalogResponse.Machine machine : catalog.machines()) {
      card.addChild(Cards.row(
          machine.displayName() == null ? "" : machine.displayName(),
          Formats.number(machine.price())
      ));
    }

    // The purchase itself happens in the in-game menu; this opens it.
    card.addChild(Widgets.i18nButton(
        "mineclubplus.materials.buy",
        () -> {
          ServerCommands.openMachineShop();
          activity.closeWindow();
        },
        "mcp-accent-button",
        "mcp-settings-button"
    ));

    return card;
  }

  /**
   * The shelf along the bottom: one row of recipes and the arrows to the next page.
   *
   * <p>Its height is fixed by the stylesheet, so it neither scrolls nor grows, and its tiles can
   * therefore render real blocks without leaving the window.
   */
  private Widget createShelf(
      MineClubPlusActivity activity,
      List<ClientCatalogResponse.Recipe> recipes
  ) {
    int columns = Math.max(1, activity.layout().shelfColumns());
    int perPage = columns * RECIPE_ROWS;
    int pages = Math.max(1, (recipes.size() + perPage - 1) / perPage);
    int current = Math.min(activity.listPage(), pages - 1);

    DivWidget shelf = Widgets.panel("mcp-shelf");

    VerticalListWidget<Widget> card = Cards.card(
        "mineclubplus.materials.recipes",
        "mcp-shelf-card"
    );

    HorizontalListWidget row = Widgets.row("mcp-grid-row");
    int from = current * perPage;
    int to = Math.min(from + perPage, recipes.size());
    for (int index = from; index < to; index++) {
      row.addEntry(this.createTile(activity, recipes.get(index)));
    }

    card.addChild(row);
    shelf.addChild(card);

    shelf.addChild(Pager.create(
        current, pages, recipes.size(), activity::setListPage, "mcp-paged-pager"));

    return shelf;
  }

  /**
   * A recipe as a tile, in the same shape as the transporter's.
   *
   * <p>The result amount sits in the corner and the name below the block; the ingredients are in
   * the tooltip, which keeps the tile small enough to show several at once.
   */
  private Widget createTile(
      MineClubPlusActivity activity,
      ClientCatalogResponse.Recipe recipe
  ) {
    String key = recipe.key() == null ? "" : recipe.key();
    String name = recipe.resultName() == null ? "" : recipe.resultName();

    DivWidget tile = Widgets.panel("mcp-tile");
    Widgets.clickable(tile, () -> activity.selectRecipe(key));
    tile.setHoverComponent(Component.text(name + "  -  " + ingredients(recipe)));

    tile.addChild(Widgets.text(
        Formats.compact(recipe.resultAmount()) + "x",
        "mcp-tile-amount"
    ));

    if (recipe.result() != null && !recipe.result().isEmpty()) {
      tile.addChild(ItemIcons.of(recipe.result(), "mcp-tile-icon"));
    }

    tile.addChild(Widgets.text(name, "mcp-tile-name"));

    return tile;
  }

  /**
   * The selected recipe's pattern.
   *
   * <p>A shapeless recipe has none, so its ingredients are listed instead; a grid would imply an
   * order that does not exist.
   */
  private Widget createDetail(
      MineClubPlusActivity activity,
      ClientCatalogResponse.Recipe recipe
  ) {
    DivWidget frame = Widgets.panel("mcp-paged");
    VerticalListWidget<Widget> page = Widgets.column("mcp-paged-body");

    VerticalListWidget<Widget> card = Cards.titled(
        Formats.number(recipe.resultAmount()) + "x "
            + (recipe.resultName() == null ? "" : recipe.resultName())
    );

    if (recipe.shape().isEmpty()) {
      card.addChild(Widgets.i18n("mineclubplus.materials.loose", "mcp-settings-hint"));
    } else {
      card.addChild(this.createGrid(recipe));
    }

    card.addChild(Widgets.text(ingredients(recipe), "mcp-log-text"));

    page.addChild(card);
    frame.addChild(page);

    // Without this the recipe is a dead end: the list is gone and nothing leads back to it.
    frame.addChild(Widgets.i18nButton(
        "mineclubplus.materials.back",
        () -> activity.selectRecipe(recipe.key() == null ? "" : recipe.key()),
        "mcp-ghost-button",
        "mcp-paged-back"
    ));

    return frame;
  }

  private Widget createGrid(ClientCatalogResponse.Recipe recipe) {

    Map<Character, String> items = new HashMap<>();
    for (ClientCatalogResponse.Ingredient ingredient : recipe.ingredients()) {
      String slot = ingredient.slot();
      if (slot != null && !slot.isEmpty() && ingredient.item() != null) {
        items.put(slot.charAt(0), ingredient.item());
      }
    }

    DivWidget grid = Widgets.panel("mcp-recipe");
    List<String> shape = recipe.shape();

    for (int row = 0; row < shape.size(); row++) {
      String line = shape.get(row);

      for (int column = 0; column < line.length(); column++) {
        char slot = line.charAt(column);
        String item = slot == EMPTY ? null : items.get(slot);

        // Every slot carries its own id so the stylesheet can place it by row and column. A
        // list would decide the positions itself, and a pattern has to stay where it is.
        String place = "mcp-recipe-" + row + "-" + column;
        grid.addChild(item == null
            ? Widgets.panel("mcp-recipe-slot", place)
            : ItemIcons.of(item, "mcp-recipe-slot", place));
      }
    }

    return grid;
  }

  private @Nullable ClientCatalogResponse.Recipe findSelected(
      MineClubPlusActivity activity,
      ClientCatalogResponse catalog
  ) {
    String selected = activity.selectedRecipe();
    if (selected == null) {
      return null;
    }

    for (ClientCatalogResponse.Recipe recipe : catalog.recipes()) {
      if (selected.equals(recipe.key())) {
        return recipe;
      }
    }

    return null;
  }

  /** @return the ingredients on one line, for example "8x Sand, 1x Coal" */
  private static String ingredients(ClientCatalogResponse.Recipe recipe) {
    Map<String, Long> counted = new HashMap<>();
    List<String> order = new ArrayList<>();

    for (ClientCatalogResponse.Ingredient ingredient : recipe.ingredients()) {
      String name = ingredient.itemName() == null ? "" : ingredient.itemName();
      if (name.isEmpty()) {
        continue;
      }

      // A letter can appear several times in the pattern, so occurrences are counted rather
      // than repeated.
      long count = countIn(recipe, ingredient);
      if (!counted.containsKey(name)) {
        order.add(name);
      }

      counted.merge(name, count, Long::sum);
    }

    StringBuilder text = new StringBuilder();
    for (String name : order) {
      if (text.length() > 0) {
        text.append(", ");
      }

      text.append(Formats.number(counted.get(name))).append("x ").append(name);
    }

    return text.toString();
  }

  /**
   * @return how many times the ingredient appears in the pattern, or its own amount when the
   *     recipe is shapeless
   */
  private static long countIn(
      ClientCatalogResponse.Recipe recipe,
      ClientCatalogResponse.Ingredient ingredient
  ) {
    String slot = ingredient.slot();
    if (slot == null || slot.isEmpty() || recipe.shape().isEmpty()) {
      return Math.max(1L, ingredient.amount());
    }

    char letter = slot.charAt(0);
    long count = 0L;
    for (String line : recipe.shape()) {
      for (int index = 0; index < line.length(); index++) {
        if (line.charAt(index) == letter) {
          count++;
        }
      }
    }

    return Math.max(1L, count);
  }
}
