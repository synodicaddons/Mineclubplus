package dk.mineclub.plus.core.ui.widget;

import dk.mineclub.plus.core.util.Translations;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.action.Pressable;
import net.labymod.api.client.gui.screen.widget.cursor.CursorTypes;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import net.labymod.api.client.sound.SoundType;
import org.jetbrains.annotations.NotNull;

/**
 * Small factory helpers so page code reads as a layout tree instead of widget plumbing.
 *
 * <p>Two layout primitives carry the whole window, both picked because they place their children
 * from their own bounds in a single pass:
 *
 * <ul>
 *   <li>{@link #panel} -- a plain box whose children position themselves with
 *       {@code left/top/right/bottom} in the stylesheet.
 *   <li>{@link #column} / {@link #row} -- lists that stack their children.
 * </ul>
 *
 * <p>Deeply nested self-sizing containers are deliberately avoided: LabyMod caps bounds updates at
 * one per widget per frame, so a tall chain of containers that each size themselves from their
 * children can settle on a wrong layout and never recover.
 */
public final class Widgets {

  private Widgets() {
  }

  /**
   * A box that positions nothing: every child places itself from the stylesheet.
   */
  public static @NotNull DivWidget panel(String... ids) {
    return identify(new DivWidget(), ids);
  }

  /**
   * Stacks its children top to bottom.
   */
  public static @NotNull VerticalListWidget<Widget> column(String... ids) {
    VerticalListWidget<Widget> list = new VerticalListWidget<>();
    list.selectable().set(false);
    return identify(list, ids);
  }

  /**
   * Lines its children up left to right.
   */
  public static @NotNull HorizontalListWidget row(String... ids) {
    return identify(new HorizontalListWidget(), ids);
  }

  public static @NotNull ComponentWidget text(String text, String... ids) {
    return identify(ComponentWidget.text(text), ids);
  }

  /**
   * A translated text.
   *
   * <p>Goes through {@link Translations} rather than LabyMod's own {@code i18n}, because the
   * language is chosen inside the addon and LabyMod only knows the client's own.
   */
  public static @NotNull ComponentWidget i18n(String key, String... ids) {
    return identify(ComponentWidget.text(Translations.get(key)), ids);
  }

  public static @NotNull ComponentWidget i18nArgs(String key, Object[] args, String... ids) {
    return identify(ComponentWidget.text(Translations.get(key, args)), ids);
  }

  public static @NotNull ComponentWidget component(Component component, String... ids) {
    return identify(ComponentWidget.component(component), ids);
  }

  public static @NotNull IconWidget icon(Icon icon, String... ids) {
    return identify(new IconWidget(icon), ids);
  }

  public static @NotNull ButtonWidget button(String label, Pressable pressable, String... ids) {
    return identify(ButtonWidget.text(label, pressable), ids);
  }

  public static @NotNull ButtonWidget i18nButton(String key, Pressable pressable, String... ids) {
    return identify(ButtonWidget.text(Translations.get(key), pressable), ids);
  }

  public static @NotNull BarWidget bar(float ratio, String... ids) {
    return identify(new BarWidget(ratio), ids);
  }

  /**
   * A flat colour square standing in for the item texture.
   */
  public static @NotNull DivWidget swatch(int color, String... ids) {
    DivWidget widget = identify(new DivWidget(), ids);
    widget.backgroundColor().set(color);
    return widget;
  }

  /**
   * Turns a plain container into a clickable row: press handler, pointing hand and the standard
   * click sound, so a card behaves like a button without looking like one.
   */
  public static <T extends AbstractWidget<?>> T clickable(T widget, Pressable pressable) {
    widget.setPressable(pressable);
    widget.setHoverCursor(CursorTypes.POINTING_HAND, true);
    widget.setInteractionSound(SoundType.BUTTON_CLICK);
    return widget;
  }

  private static <T extends Widget> T identify(T widget, String... ids) {
    for (String id : ids) {
      widget.addId(id);
    }

    return widget;
  }
}
