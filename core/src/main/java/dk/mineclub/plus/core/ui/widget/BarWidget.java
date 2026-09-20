package dk.mineclub.plus.core.ui.widget;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.gui.screen.widget.SimpleWidget;
import net.labymod.api.client.gui.screen.widget.attributes.bounds.Bounds;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.util.bounds.ModifyReason;

/**
 * A progress bar whose fill is the MineClub gradient.
 *
 * <p>LabyMod's own {@code ProgressBarWidget} is not used, for two reasons. It fills with one flat
 * colour, and it only repositions its fill when the <em>value</em> changes -- never when the
 * widget is resized. A bar that learns its width from a list layout therefore computes its fill
 * once, while the width is still zero, and stays empty for good. This one measures against the
 * current bounds every frame, which costs two float comparisons and cannot get stuck.
 *
 * <p>The fill is a one pixel tall gradient stretched to size, so the ramp is smooth at any width
 * rather than a row of stepped segments.
 */
@AutoWidget
public class BarWidget extends SimpleWidget {

  private static final ResourceLocation TEXTURE = ResourceLocation.create(
      "mineclubplus",
      "textures/bar.png"
  );

  private static final ModifyReason FILL_BOUNDS = ModifyReason.of("mineclubplus:barFill");

  private final float progress;

  private IconWidget fill;

  /**
   * @param progress 0 to 1; anything outside is clamped
   */
  public BarWidget(float progress) {
    this.progress = Math.max(0.0F, Math.min(1.0F, progress));
  }

  public float progress() {
    return this.progress;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.fill = new IconWidget(Icon.texture(TEXTURE));
    this.fill.addId("fill");
    this.addChild(this.fill);
  }

  @Override
  public void renderWidget(ScreenContext context) {
    this.updateFill();

    super.renderWidget(context);
  }

  private void updateFill() {
    if (this.fill == null) {
      return;
    }

    Bounds track = this.bounds();
    float left = track.getLeft();
    float right = left + track.getWidth() * this.progress;

    Bounds bounds = this.fill.bounds();
    if (bounds.getLeft() == left
        && bounds.getRight() == right
        && bounds.getTop() == track.getTop()
        && bounds.getBottom() == track.getBottom()) {
      return;
    }

    bounds.setBounds(left, track.getTop(), right, track.getBottom(), FILL_BOUNDS);
  }
}
