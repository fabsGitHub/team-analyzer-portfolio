package com.teamanalyzer.teamanalyzer.web;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Demo starts per client address in a sliding window, kept in memory by one API instance.
 *
 * <p>Memory is bounded: at most {@code maxAddresses} addresses are tracked. When that many are
 * active, the address seen least recently is forgotten. A flood of new addresses therefore never
 * locks out other visitors; at worst a forgotten address can start again early. What all
 * addresses together can start is capped by the database-backed limits in
 * {@code DemoWorkspaceService}, which hold across instances.
 */
final class DemoStartLimiter {

  private final int maxPerAddress;
  private final Duration window;
  private final Map<String, ArrayDeque<Instant>> starts;

  DemoStartLimiter(int maxPerAddress, Duration window, int maxAddresses) {
    this.maxPerAddress = maxPerAddress;
    this.window = window;
    // Access order: iteration starts with the address seen least recently.
    this.starts = new LinkedHashMap<>(16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, ArrayDeque<Instant>> eldest) {
        return size() > maxAddresses;
      }
    };
  }

  /**
   * Counts a start for the address, unless it already used its limit in the window.
   *
   * @return empty if the start is allowed, otherwise how long until the next one is
   */
  synchronized Optional<Duration> tryAcquire(String address, Instant now) {
    Instant cutoff = now.minus(window);
    forgetIdleAddresses(cutoff);

    ArrayDeque<Instant> recent = starts.computeIfAbsent(address, ignored -> new ArrayDeque<>());
    while (!recent.isEmpty() && !recent.peekFirst().isAfter(cutoff)) {
      recent.removeFirst();
    }
    if (recent.size() >= maxPerAddress) {
      return Optional.of(Duration.between(now, recent.peekFirst().plus(window)));
    }
    recent.addLast(now);
    return Optional.empty();
  }

  synchronized int trackedAddresses() {
    return starts.size();
  }

  /** Drops addresses without a start in the window, beginning with the least recently seen. */
  private void forgetIdleAddresses(Instant cutoff) {
    Iterator<ArrayDeque<Instant>> it = starts.values().iterator();
    while (it.hasNext()) {
      ArrayDeque<Instant> recent = it.next();
      if (!recent.isEmpty() && recent.peekLast().isAfter(cutoff)) {
        break;
      }
      it.remove();
    }
  }
}
