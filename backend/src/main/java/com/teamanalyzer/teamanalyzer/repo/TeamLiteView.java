// backend/src/main/java/com/teamanalyzer/teamanalyzer/repo/TeamLiteView.java
package com.teamanalyzer.teamanalyzer.repo;

import java.util.UUID;

/**
 * Schlanke Projektion (nur das, was im UI benötigt wird).
 */
public interface TeamLiteView {
    UUID getId();

    String getName();
}
