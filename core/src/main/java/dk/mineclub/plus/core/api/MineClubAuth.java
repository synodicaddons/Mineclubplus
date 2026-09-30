package dk.mineclub.plus.core.api;

import dk.mineclub.plus.core.MineClubPlusAddon;
import dk.mineclub.plus.core.api.model.ClientChallengeResponse;
import dk.mineclub.plus.core.api.model.ClientVerifyResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.labymod.api.Laby;
import net.labymod.api.client.session.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Authenticates the addon as the player sitting at the client.
 *
 * <p>The API cannot trust a uuid the addon states, so the player proves ownership of the account
 * the same way the client does when joining an online-mode server:
 *
 * <ol>
 *   <li>the API issues a {@code serverId},
 *   <li>the addon sends it to Mojang with the player's own session,
 *   <li>the API has Mojang confirm it and issues a token.
 * </ol>
 *
 * <p>The token is held in memory only. Closing the client means logging in again, and a token
 * copied off disk cannot be replayed.
 */
public final class MineClubAuth {

  private static final String CHALLENGE_PATH = "/v2/client/auth/challenge";
  private static final String VERIFY_PATH = "/v2/client/auth/verify";

  private static final String MOJANG_JOIN =
      "https://sessionserver.mojang.com/session/minecraft/join";

  /** A token lives for a week server side; this renews well before it expires. */
  private static final long TOKEN_LIFETIME_MILLIS = 6L * 24L * 60L * 60L * 1000L;

  /** Back-off after a failed login, so an unreachable server is not hammered. */
  private static final long RETRY_DELAY_MILLIS = 60L * 1000L;

  /**
   * How long a login may be in flight before it is written off.
   *
   * <p>The three steps time out at 13 seconds each, so anything past this has stopped rather than
   * slowed down. Without the deadline a step whose callback never arrives would leave
   * {@link #authenticating} set for the rest of the session, and every later request would hand
   * back no token without a word -- the window would then show sample numbers and never say why.
   */
  private static final long AUTH_TIMEOUT_MILLIS = 45L * 1000L;

  private final MineClubPlusAddon addon;

  private volatile String token;
  private volatile long tokenExpiresAt;
  private volatile long nextAttemptAt;

  /** Set while a login is running, so two pages cannot log in separately. */
  private volatile boolean authenticating;

  /** When the login in flight started, so one that never answers can be written off. */
  private volatile long authenticatingSince;

  public MineClubAuth(@NotNull MineClubPlusAddon addon) {
    this.addon = addon;
  }

  public boolean hasToken() {
    return this.token != null && System.currentTimeMillis() < this.tokenExpiresAt;
  }

  /** Forgets the token, for example when the API stops accepting it. */
  public void invalidate() {
    this.token = null;
    this.tokenExpiresAt = 0L;
  }

  /**
   * Hands a valid token to the caller, or {@code null} when the player cannot be verified.
   *
   * @param consumer always called, including on failure
   */
  public void token(@NotNull Consumer<String> consumer) {
    if (this.hasToken()) {
      consumer.accept(this.token);
      return;
    }

    long now = System.currentTimeMillis();
    if (this.authenticating) {
      if (now - this.authenticatingSince < AUTH_TIMEOUT_MILLIS) {
        consumer.accept(null);
        return;
      }

      // The login stopped answering. Say so and start over rather than staying silent.
      this.addon.logger().warn("MineClub-login svarede ikke inden for "
          + (AUTH_TIMEOUT_MILLIS / 1000L) + " sekunder, prøver igen");
      this.authenticating = false;
    }

    if (now < this.nextAttemptAt) {
      consumer.accept(null);
      return;
    }

    Session session = Laby.labyAPI().minecraft().sessionAccessor().getSession();
    if (session == null || !session.hasUniqueId() || !session.isPremium()) {
      // Without a real Mojang session there is nothing to prove ownership with.
      this.fail("spilleren har ingen premium-session", consumer);
      return;
    }

    String accessToken = session.getAccessToken();
    if (accessToken == null || accessToken.isEmpty()) {
      this.fail("klienten har ingen access token", consumer);
      return;
    }

    this.authenticating = true;
    this.authenticatingSince = now;
    this.challenge(session, accessToken, consumer);
  }

  private void challenge(Session session, String accessToken, Consumer<String> consumer) {
    Map<String, Object> body = new HashMap<>();
    body.put("username", session.getUsername());

    Requests.post(
            ClientChallengeResponse.class,
            MineClubApi.baseUrl() + CHALLENGE_PATH,
            this.addon.userAgent(),
            body,
            null
        )
        .execute(response -> this.step("udfordringen", consumer, () -> {
          ClientChallengeResponse challenge = response.isPresent() ? response.get() : null;
          if (challenge == null || challenge.serverId() == null) {
            this.fail("api'en gav ingen udfordring (HTTP " + response.getStatusCode() + ")",
                consumer);
            return;
          }

          this.join(session, accessToken, challenge.serverId(), consumer);
        }));
  }

  /**
   * The same call the client makes when joining an online-mode server. Mojang answers 204 once
   * the session is accepted, which is what lets the API confirm it from its side.
   */
  private void join(
      Session session,
      String accessToken,
      String serverId,
      Consumer<String> consumer
  ) {
    Map<String, Object> body = new HashMap<>();
    body.put("accessToken", accessToken);
    body.put("selectedProfile", session.getUniqueId().toString().replace("-", ""));
    body.put("serverId", serverId);

    Requests.post(Void.class, MOJANG_JOIN, this.addon.userAgent(), body, null)
        .execute(response -> this.step("Mojang-kaldet", consumer, () -> {
          int status = response.getStatusCode();
          if (status != 204 && status != 200) {
            this.fail("Mojang afviste sessionen (HTTP " + status + ")", consumer);
            return;
          }

          this.verify(session, serverId, consumer);
        }));
  }

  private void verify(Session session, String serverId, Consumer<String> consumer) {
    Map<String, Object> body = new HashMap<>();
    body.put("username", session.getUsername());
    body.put("serverId", serverId);
    body.put("client", this.addon.userAgent());

    Requests.post(
            ClientVerifyResponse.class,
            MineClubApi.baseUrl() + VERIFY_PATH,
            this.addon.userAgent(),
            body,
            null
        )
        .execute(response -> this.step("tokenet", consumer, () -> {
          ClientVerifyResponse verified = response.isPresent() ? response.get() : null;
          if (verified == null || verified.token() == null) {
            this.fail("api'en ville ikke udstede et token (HTTP " + response.getStatusCode() + ")",
                consumer);
            return;
          }

          this.token = verified.token();
          this.tokenExpiresAt = System.currentTimeMillis() + TOKEN_LIFETIME_MILLIS;
          this.authenticating = false;
          this.nextAttemptAt = 0L;

          consumer.accept(this.token);
        }));
  }

  /**
   * Runs one step of the login, turning anything it throws into an ordinary failure.
   *
   * <p>A throw inside a request callback is swallowed by the worker that ran it, and the step
   * after it never happens. The login would then stay in flight, holding back every request that
   * needs a token without anything being written anywhere.
   */
  private void step(String what, Consumer<String> consumer, Runnable body) {
    try {
      body.run();
    } catch (Exception exception) {
      this.fail(what + " fejlede: " + exception, consumer);
    }
  }

  private void fail(String reason, Consumer<String> consumer) {
    this.authenticating = false;
    this.nextAttemptAt = System.currentTimeMillis() + RETRY_DELAY_MILLIS;
    this.addon.logger().warn("MineClub-login mislykkedes: " + reason);
    consumer.accept(null);
  }
}
