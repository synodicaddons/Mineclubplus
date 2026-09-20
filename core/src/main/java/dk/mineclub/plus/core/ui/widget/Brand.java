package dk.mineclub.plus.core.ui.widget;

import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.component.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

/**
 * The MineClub+ wordmark.
 *
 * <p>Minecraft colours text per character, so the gradient is built by colouring each letter a
 * step further along the ramp. The ramp runs from the orange of MINE to the yellow of CLUB in the
 * server logo, and the plus takes the brightest end so it reads as a mark of its own.
 */
public final class Brand {

  /**
   * The orange of "MINE" in the logo.
   */
  public static final int ORANGE = 0xF5961B;

  /**
   * The yellow of "CLUB" in the logo.
   */
  public static final int YELLOW = 0xFFE14A;

  private static final String WORDMARK = "MINECLUB";

  private Brand() {
  }

  public static @NotNull Component wordmark() {
    return gradient(WORDMARK, ORANGE, YELLOW)
        .append(Component.text("+", TextColor.color(YELLOW), TextDecoration.BOLD));
  }

  /**
   * @param from the colour of the first character
   * @param to   the colour of the last character
   */
  public static @NotNull Component gradient(@NotNull String text, int from, int to) {
    Component result = Component.empty();

    int length = text.length();
    for (int index = 0; index < length; index++) {
      float progress = length <= 1 ? 0.0F : (float) index / (length - 1);

      result = result.append(Component.text(
          String.valueOf(text.charAt(index)),
          TextColor.color(mix(from, to, progress)),
          TextDecoration.BOLD
      ));
    }

    return result;
  }

  private static int mix(int from, int to, float progress) {
    int red = channel(from >> 16, to >> 16, progress);
    int green = channel(from >> 8, to >> 8, progress);
    int blue = channel(from, to, progress);

    return red << 16 | green << 8 | blue;
  }

  private static int channel(int from, int to, float progress) {
    int start = from & 0xFF;
    int end = to & 0xFF;

    return Math.round(start + (end - start) * progress) & 0xFF;
  }
}
