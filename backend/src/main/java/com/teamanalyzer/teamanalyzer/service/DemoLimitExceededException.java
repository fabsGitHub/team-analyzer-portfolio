package com.teamanalyzer.teamanalyzer.service;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * A demo limit was reached. The API answers with a problem detail that carries {@link #code()}
 * and {@link #limit()}, so the frontend can show a translated message.
 */
public class DemoLimitExceededException extends ResponseStatusException {

  public enum Limit {
    /** Demo starts from one client address in the last hour. */
    ADDRESS("demo.limit.address", HttpStatus.TOO_MANY_REQUESTS,
        "Too many demos were started from this network in the last hour (limit %d)."),
    /** Demo starts across all visitors in the last hour. */
    HOURLY("demo.limit.hourly", HttpStatus.TOO_MANY_REQUESTS,
        "Too many demos were started in the last hour (limit %d)."),
    /** Demo workspaces that exist at the same time. */
    CAPACITY("demo.limit.capacity", HttpStatus.TOO_MANY_REQUESTS,
        "All %d demo workspaces are in use."),
    /** Surveys in one demo workspace. */
    SURVEYS("demo.limit.surveys", HttpStatus.FORBIDDEN,
        "A demo workspace can hold at most %d surveys."),
    /** Participation tokens for one demo survey. */
    TOKENS("demo.limit.tokens", HttpStatus.FORBIDDEN,
        "A demo survey can have at most %d participation tokens.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    Limit(String code, HttpStatus status, String message) {
      this.code = code;
      this.status = status;
      this.message = message;
    }
  }

  private final transient Limit kind;
  private final int limit;
  private final transient Duration retryAfter;

  public DemoLimitExceededException(Limit kind, int limit) {
    this(kind, limit, null);
  }

  /** @param retryAfter when trying again makes sense, sent as {@code Retry-After}; may be null */
  public DemoLimitExceededException(Limit kind, int limit, Duration retryAfter) {
    super(kind.status, kind.message.formatted(limit));
    this.kind = kind;
    this.limit = limit;
    this.retryAfter = retryAfter;
  }

  public String code() {
    return kind.code;
  }

  public int limit() {
    return limit;
  }

  public Duration retryAfter() {
    return retryAfter;
  }
}
