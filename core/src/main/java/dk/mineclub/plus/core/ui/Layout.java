package dk.mineclub.plus.core.ui;

import net.labymod.api.Laby;
import net.labymod.api.client.gui.window.Window;
import org.jetbrains.annotations.NotNull;

/**
 * Every measurement the window makes of itself.
 *
 * <p>Minecraft's GUI scale divides the screen into scaled pixels, and those are the units the
 * stylesheet works in: a 1920x1080 monitor is 960x540 to LSS at scale 2 and 480x270 at scale 4.
 * The window is a fraction of that, capped -- so the screen and the window are two different
 * numbers, and changing the scale moves them apart.
 *
 * <p>The layout belongs to the window, so every breakpoint here is measured on the window box and
 * never on the screen. That is the whole point of the class. A stylesheet media query cannot do
 * it: LabyMod resolves {@code @media screen} against the Minecraft window and {@code @media
 * document} against the full-screen activity document, and neither one is a widget -- so a rule
 * that has to react to the window's own size is decided here and handed to the stylesheet as an
 * id.
 *
 * <p>The numbers mirror {@code mineclubplus.lss}. They are declared once here and once there, and
 * nothing else re-derives them.
 */
public final class Layout {

  // ---------------------------------------------------------------------------- the window box -

  /** {@code .mcp-window}: 96% of the screen, never wider than 620. */
  private static final float WIDTH_RATIO = 0.96F;
  private static final int MAX_WIDTH = 620;

  /** {@code .mcp-window}: 94% of the screen, never taller than 340. */
  private static final float HEIGHT_RATIO = 0.94F;
  private static final int MAX_HEIGHT = 340;

  // ------------------------------------------------------------------------------ shell chrome -

  /** The header and the divider under it, from the shell metrics in the LSS. */
  private static final int CHROME_HEIGHT = 26 + 1;

  /** {@code .mcp-content} left edge: the sidebar and its divider. */
  private static final int SIDEBAR = 105;
  private static final int SIDEBAR_COMPACT = 31;

  /** {@code .mcp-content} padding across: left 11 plus right 10, or 9 plus 8 compact. */
  private static final int PADDING_X = 11 + 10;
  private static final int PADDING_X_COMPACT = 9 + 8;

  /**
   * {@code .mcp-content} padding down: 11 above, and below an inset wide enough to clear the
   * window's 12 border-radius -- so a scroll never cuts a row off inside the rounded corner.
   * Wider than the horizontal padding on purpose, which is why the two are separate.
   */
  private static final int PADDING_Y = 11 + 14;
  private static final int PADDING_Y_COMPACT = 9 + 12;

  // ------------------------------------------------------------------------------- breakpoints -

  /**
   * Under this the side rails cannot hold their contents -- the transporter detail column gets
   * 28% of the content box, and below roughly 400 there that is narrower than the item name.
   */
  private static final int STACK_WIDTH = 520;

  /**
   * The overview's own threshold, and the reason {@link #isStacked(MineClubPage)} takes a page.
   *
   * <p>The two pages do not need the same width. The transporter's detail column has to hold an
   * item name, which is what sets {@link #STACK_WIDTH}; the overview's rail only holds "players
   * online" and four commands, and does that in about 95 pixels. Stacking the overview at the
   * transporter's threshold cost it the two-column layout earlier than it had to -- and with it
   * the real item blocks, because stacking means scrolling and a scrolled item block cannot be
   * clipped.
   */
  private static final int STACK_WIDTH_OVERVIEW = 440;

  /**
   * Under this a two-column page cannot show its minimum two rows even with the compressed
   * metrics below, so it stacks on height alone however wide the window is.
   */
  private static final int STACK_HEIGHT = 250;

  /**
   * Under this the pages keep two columns but tighten their vertical rhythm. The normal rhythm
   * needs 132 + 30 + the row safety + two 28 rows of content, so it clears down to a 274
   * window; the rest is margin.
   */
  private static final int SHORT_HEIGHT = 276;

  /** At or under this the filter pills tighten and the totals pill steps aside. */
  private static final int TIGHT_WIDTH = 460;

  /** Under this the navigation keeps its icons and drops its labels. */
  private static final int COMPACT_WIDTH = 405;

  // -------------------------------------------------------------------------- page metrics ----

  /** Where the holdings card starts, and what its title and padding cost. */
  private static final int HOLDINGS_TOP = 132;
  private static final int HOLDINGS_TOP_SHORT = 108;
  private static final int HOLDING_ROW = 28;
  private static final int HOLDING_ROW_SHORT = 26;
  private static final int CARD_CHROME = 30;

  /**
   * Slack kept under the last row of a card, and under the last row of the grid.
   *
   * <p>{@link #CARD_CHROME} and the row heights are this file's reading of the stylesheet, and a
   * title's rendered height is not something either side can measure exactly -- a few pixels of
   * disagreement is normal. Without slack that disagreement lands on the last row, and because a
   * card cannot clip a rendered item (Minecraft draws item blocks in a pass of their own, after
   * any scissor has been popped) the block hangs outside the card instead of being cut off.
   *
   * <p>So the count is deliberately pessimistic. Unused pixels at the bottom of a card are
   * invisible; an item sticking out of one is the first thing anybody notices. Raised from 8
   * after a row still reached past the card at the largest window size.
   */
  private static final int ROW_SAFETY = 16;

  /**
   * One, not two.
   *
   * <p>A floor above what actually fits is not a floor, it is an overflow: the card would be told
   * to draw a row it has no room for, and a holdings row draws a real item block that nothing can
   * clip. Showing one row on a window that only has room for one is the honest answer.
   */
  private static final int MIN_HOLDINGS = 1;
  private static final int MAX_HOLDINGS = 8;

  /** Where the transporter grid starts -- under the page header and the filter row. */
  private static final int GRID_TOP = 50;
  private static final int GRID_TOP_SHORT = 44;
  private static final int PAGER_HEIGHT = 16;

  /**
   * {@code .mcp-log-row} plus the gap the card puts between rows.
   *
   * <p>The row is 18 tall, but {@code .mcp-card} carries {@code space-between-entries: 4}. Left
   * out of the arithmetic it made room for a couple of rows too many, which pushed the pager past
   * the window edge.
   */
  private static final int LIST_ROW = 22;

  private static final int MIN_LIST_ROWS = 3;
  private static final int MAX_LIST_ROWS = 20;

  /** {@code .mcp-tile}. The width is fixed on purpose -- see {@code gridColumns()}. */
  private static final int TILE_WIDTH = 56;
  private static final int TILE_HEIGHT = 52;
  private static final int TILE_HEIGHT_SHORT = 46;
  private static final int TILE_GAP = 5;

  private static final int MIN_ROWS = 2;
  private static final int MIN_COLUMNS = 3;

  /**
   * The share of the content box the stacked overview gives its holdings card.
   *
   * <p>Stacked, the card is pinned to the floor and everything else scrolls above it, because a
   * holdings row draws a real item block and a scrolled block cannot be clipped. 55% is what
   * makes that work at every size: two rows in the smallest window a client can produce, four in
   * the largest, and never a row past the card edge. Mirrored by
   * {@code .mcp-overview-stacked-holdings}.
   */
  private static final float STACKED_HOLDINGS_SHARE = 0.55F;

  /** {@code .mcp-grid-area} keeps 71% of the content box; the detail column takes the rest. */
  private static final float GRID_SHARE = 0.71F;

  // ---------------------------------------------------------------------------------------------

  private final int width;
  private final int height;
  private final boolean stacked;
  private final boolean overviewStacked;
  private final boolean shortened;
  private final boolean tight;
  private final boolean compact;

  private Layout(int screenWidth, int screenHeight) {
    this.width = Math.min(Math.round(screenWidth * WIDTH_RATIO), MAX_WIDTH);
    this.height = Math.min(Math.round(screenHeight * HEIGHT_RATIO), MAX_HEIGHT);

    this.stacked = this.width < STACK_WIDTH || this.height < STACK_HEIGHT;
    this.overviewStacked = this.width < STACK_WIDTH_OVERVIEW || this.height < STACK_HEIGHT;
    this.shortened = this.height < SHORT_HEIGHT;
    this.tight = this.width <= TIGHT_WIDTH;
    this.compact = this.width < COMPACT_WIDTH;
  }

  /**
   * @param screenWidth  the scaled screen width, as handed to {@code Activity#resize}
   * @param screenHeight the scaled screen height
   */
  public static @NotNull Layout of(int screenWidth, int screenHeight) {
    return new Layout(screenWidth, screenHeight);
  }

  /**
   * Measures the window as it stands. Used when the activity opens, where no resize has run yet.
   */
  public static @NotNull Layout current() {
    Window window = Laby.labyAPI().minecraft().minecraftWindow();
    return new Layout(window.getScaledWidth(), window.getScaledHeight());
  }

  /**
   * @return the screen width in scaled pixels -- the space the window has to stay inside
   */
  public static int screenWidth() {
    return Laby.labyAPI().minecraft().minecraftWindow().getScaledWidth();
  }

  public static int screenHeight() {
    return Laby.labyAPI().minecraft().minecraftWindow().getScaledHeight();
  }

  // ------------------------------------------------------------------------------------ the box -

  /** The window width in scaled pixels. */
  public int width() {
    return this.width;
  }

  /** The window height in scaled pixels. */
  public int height() {
    return this.height;
  }

  /** The content area inside the sidebar and the padding. */
  public int contentWidth() {
    return this.width
        - (this.compact ? SIDEBAR_COMPACT : SIDEBAR)
        - (this.compact ? PADDING_X_COMPACT : PADDING_X);
  }

  /** The content area inside the header and the padding. */
  public int contentHeight() {
    return this.height - CHROME_HEIGHT - (this.compact ? PADDING_Y_COMPACT : PADDING_Y);
  }

  // ------------------------------------------------------------------------------------- modes -

  /**
   * @return true when the window cannot hold a side rail, so the pages become one scrolling
   *     column -- either because it is too narrow for the rail or too short for two rows beside
   *     it
   */
  public boolean isStacked() {
    return this.stacked;
  }

  /**
   * @param page the page asking -- the overview keeps its two columns on a narrower window than
   *             the rest, because its rail needs less room than the transporter's detail column
   * @return true when that page cannot hold a side rail, so it becomes one scrolling column
   */
  public boolean isStacked(@NotNull MineClubPage page) {
    return page == MineClubPage.OVERVIEW ? this.isOverviewStacked() : this.isStacked();
  }

  /**
   * The overview's answer on its own, without naming a page.
   *
   * @return true when the overview cannot hold its rail
   */
  public boolean isOverviewStacked() {
    return this.overviewStacked;
  }

  /**
   * @return true when the window is short enough that the pinned pages tighten their vertical
   *     rhythm, but still tall enough to keep their two columns
   */
  public boolean isShort() {
    return this.shortened;
  }

  /**
   * @return true when the filter pills have to tighten
   */
  public boolean isTight() {
    return this.tight;
  }

  /**
   * @return true when the navigation drops its labels
   */
  public boolean isCompact() {
    return this.compact;
  }

  // ------------------------------------------------------------------------------ page metrics -

  /**
   * How many rows fit on a page that does not scroll.
   *
   * <p>The log and the reference render real items, which cannot sit in a scrolling area without
   * drawing past the window, so they page instead. This is what fits.
   */
  public int listRows() {
    int region = this.contentHeight() - CARD_CHROME - PAGER_HEIGHT - ROW_SAFETY;
    return Math.max(MIN_LIST_ROWS, Math.min(MAX_LIST_ROWS, region / LIST_ROW));
  }

  /**
   * How many holdings fit under the welcome card and the figures on the overview page.
   */
  public int holdingRows() {
    int top = this.shortened ? HOLDINGS_TOP_SHORT : HOLDINGS_TOP;
    int row = this.shortened ? HOLDING_ROW_SHORT : HOLDING_ROW;
    int region = this.contentHeight() - top - CARD_CHROME - ROW_SAFETY;

    return Math.max(MIN_HOLDINGS, Math.min(MAX_HOLDINGS, region / row));
  }

  /**
   * How many holdings fit in the card pinned to the floor of the stacked overview.
   */
  public int stackedHoldingRows() {
    int region = Math.round(this.contentHeight() * STACKED_HOLDINGS_SHARE)
        - CARD_CHROME
        - ROW_SAFETY;

    return Math.max(MIN_HOLDINGS, Math.min(MAX_HOLDINGS, region / HOLDING_ROW));
  }

  /** What the stacked transporter keeps for its capacity strip, filters and detail card. */
  private static final int STACKED_SCROLL_MIN = 60;

  /** The gap between the scroll and the grid pinned under it. */
  private static final int STACKED_GAP = 11;

  private static final int MIN_STACKED_GRID_ROWS = 1;
  private static final int MAX_STACKED_GRID_ROWS = 3;

  /**
   * How many rows of tiles the grid pinned to the floor of the stacked transporter can show.
   *
   * <p>A tile is 52 tall against a holdings row's 24, so a percentage share would waste most of
   * a row at some sizes. The count is measured instead, and the stylesheet carries one exact
   * height per count -- {@code .mcp-grid-pinned-1} through {@code -3}, which must agree with
   * {@link #stackedGridHeight(int)}.
   */
  public int stackedGridRows() {
    int available = this.contentHeight()
        - STACKED_SCROLL_MIN
        - STACKED_GAP
        - TILE_GAP
        - PAGER_HEIGHT;
    int rows = available / (TILE_HEIGHT + TILE_GAP);

    return Math.max(MIN_STACKED_GRID_ROWS, Math.min(MAX_STACKED_GRID_ROWS, rows));
  }

  /**
   * @return the height {@code .mcp-grid-pinned-<rows>} gives that many rows plus the pager
   */
  public static int stackedGridHeight(int rows) {
    return TILE_HEIGHT * rows + TILE_GAP * (rows - 1) + TILE_GAP + PAGER_HEIGHT;
  }

  /**
   * How many rows of tiles the transporter grid can show above its pager.
   */
  public int gridRows() {
    int top = this.shortened ? GRID_TOP_SHORT : GRID_TOP;
    int tile = this.shortened ? TILE_HEIGHT_SHORT : TILE_HEIGHT;
    int available = this.contentHeight() - top - TILE_GAP - PAGER_HEIGHT - ROW_SAFETY;

    return Math.max(MIN_ROWS, available / (tile + TILE_GAP));
  }

  /**
   * How many tiles fit across the transporter grid.
   *
   * <p>The tile has a fixed width on purpose. A tile stretched by its row would only learn its
   * width once the row lays out, and by then the name has already decided how to wrap -- which is
   * why long names used to render on one line straight out of the box. A known width lets the
   * name wrap where the stylesheet says it should, and the column count carries the responsive
   * part instead.
   */
  public int gridColumns() {
    int content = this.contentWidth();
    int grid = this.stacked ? content : Math.round(content * GRID_SHARE);

    return Math.max(MIN_COLUMNS, (grid + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
  }

  /**
   * How many tiles fit across when they get the whole content box.
   *
   * <p>The transporter grid shares its width with the detail column; the reference shelf does
   * not, so it would lose a tile or two by asking {@link #gridColumns()}.
   */
  public int shelfColumns() {
    return Math.max(MIN_COLUMNS, (this.contentWidth() + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
  }

  // ---------------------------------------------------------------------------------------------

  /**
   * Whether two measurements would build the same widget tree.
   *
   * <p>A resize fires on every dragged pixel, so the activity rebuilds only when this says the
   * result would actually differ. Comparing the derived counts rather than the modes alone is
   * what catches a window that loses a grid row without crossing a breakpoint.
   */
  public boolean sameAs(@NotNull Layout other) {
    return this.stacked == other.stacked
        && this.overviewStacked == other.overviewStacked
        && this.shortened == other.shortened
        && this.tight == other.tight
        && this.compact == other.compact
        && this.holdingRows() == other.holdingRows()
        && this.stackedHoldingRows() == other.stackedHoldingRows()
        && this.gridRows() == other.gridRows()
        && this.stackedGridRows() == other.stackedGridRows()
        && this.gridColumns() == other.gridColumns()
        && this.shelfColumns() == other.shelfColumns();
  }
}
