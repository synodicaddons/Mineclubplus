package dk.mineclub.plus.core.cache;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.labymod.api.util.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single asynchronously loaded value with a time-to-live.
 *
 * <p>Three properties matter for the addon: a stale value keeps being served while the refresh is
 * in flight (so the UI never flashes empty), concurrent refreshes collapse into one request, and
 * listeners are always notified on the render thread.
 *
 * @param <T> the loaded value
 */
public final class CachedResource<T> {

  private final Loader<T> loader;
  private final long timeToLiveMillis;
  private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
  private final AtomicBoolean loading = new AtomicBoolean(false);

  private volatile T value;
  private volatile long loadedAt;
  private volatile String error;

  public CachedResource(@NotNull Loader<T> loader, long timeToLiveMillis) {
    this.loader = loader;
    this.timeToLiveMillis = timeToLiveMillis;
  }

  public @Nullable T get() {
    return this.value;
  }

  public boolean isLoading() {
    return this.loading.get();
  }

  public boolean hasValue() {
    return this.value != null;
  }

  public @Nullable String error() {
    return this.error;
  }

  public long loadedAt() {
    return this.loadedAt;
  }

  public boolean isStale() {
    return this.value == null
        || System.currentTimeMillis() - this.loadedAt > this.timeToLiveMillis;
  }

  /**
   * Starts a refresh unless one is already running, or the cached value is still fresh and
   * {@code force} is not set.
   */
  public void refresh(boolean force) {
    if (!force && !this.isStale()) {
      return;
    }

    if (!this.loading.compareAndSet(false, true)) {
      return;
    }

    this.notifyListeners();
    this.loader.load(this::onSuccess, this::onFailure);
  }

  public void invalidate() {
    this.loadedAt = 0L;
  }

  public void addListener(@NotNull Runnable listener) {
    this.listeners.add(listener);
  }

  public void removeListener(@NotNull Runnable listener) {
    this.listeners.remove(listener);
  }

  private void onSuccess(@Nullable T loaded) {
    if (loaded != null) {
      this.value = loaded;
      this.loadedAt = System.currentTimeMillis();
      this.error = null;
    }

    this.loading.set(false);
    this.notifyListeners();
  }

  private void onFailure(@NotNull Throwable throwable) {
    this.error = throwable.getMessage() == null
        ? throwable.getClass().getSimpleName()
        : throwable.getMessage();
    this.loading.set(false);
    this.notifyListeners();
  }

  private void notifyListeners() {
    if (this.listeners.isEmpty()) {
      return;
    }

    ThreadSafe.executeOnRenderThread(() -> {
      for (Runnable listener : this.listeners) {
        listener.run();
      }
    });
  }

  /**
   * Loads the value. Implementations must call exactly one of the two consumers.
   */
  @FunctionalInterface
  public interface Loader<T> {

    void load(@NotNull Consumer<T> success, @NotNull Consumer<Throwable> failure);
  }
}
