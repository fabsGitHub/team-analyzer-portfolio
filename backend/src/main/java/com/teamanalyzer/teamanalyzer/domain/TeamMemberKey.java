package com.teamanalyzer.teamanalyzer.domain;

import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TeamMemberKey implements Serializable {
    private UUID teamId;
    private UUID userId;
}
