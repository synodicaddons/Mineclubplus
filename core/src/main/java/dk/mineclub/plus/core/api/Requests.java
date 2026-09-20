package dk.mineclub.plus.core.api;

import net.labymod.api.util.io.web.request.Request;
import net.labymod.api.util.io.web.request.Request.Method;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How the addon talks to MineClub.
 *
 * <p>Timeouts and the user agent used to be repeated in every call, which let one of them wait
 * twice as long as the rest. They live here instead, so login and data requests behave alike.
 */
final class Requests {

  /**
   * Deliberately short: a request that does not arrive should leave one empty field, not hold the
   * whole window waiting.
   */
  private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
  private static final int READ_TIMEOUT_MILLIS = 8_000;

  private Requests() {
  }

  /**
   * @param bearer the client token, or {@code null} on the calls that need none
   */
  static <T> @NotNull Request<T> get(
      @NotNull Class<T> type,
      @NotNull String url,
      @NotNull String userAgent,
      @Nullable String bearer
  ) {
    return prepare(type, url, userAgent, bearer);
  }

  static <T> @NotNull Request<T> post(
      @NotNull Class<T> type,
      @NotNull String url,
      @NotNull String userAgent,
      @NotNull Object body,
      @Nullable String bearer
  ) {
    return prepare(type, url, userAgent, bearer).method(Method.POST).json(body);
  }

  private static <T> Request<T> prepare(
      Class<T> type,
      String url,
      String userAgent,
      @Nullable String bearer
  ) {
    Request<T> request = Request.ofGson(type)
        .url(url)
        .userAgent(userAgent)
        .connectTimeout(CONNECT_TIMEOUT_MILLIS)
        .readTimeout(READ_TIMEOUT_MILLIS);

    if (bearer != null) {
      request = request.addHeader("Authorization", "Bearer " + bearer);
    }

    return request.async();
  }
}
