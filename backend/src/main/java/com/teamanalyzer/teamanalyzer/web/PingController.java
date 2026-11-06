package com.teamanalyzer.teamanalyzer.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cheapest possible public endpoint. The frontend calls it on page load and before requests
 * after a quiet period, so a backend that the hosting platform stopped is started (and waited
 * for) before real requests with short timeouts are sent. It touches neither the database nor
 * the session.
 */
@RestController
public class PingController {

  @GetMapping("/api/ping")
  public ResponseEntity<Void> ping() {
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
}
