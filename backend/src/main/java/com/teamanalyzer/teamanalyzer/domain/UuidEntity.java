// backend/src/main/java/com/teamanalyzer/teamanalyzer/domain/UuidEntity.java
package com.teamanalyzer.teamanalyzer.domain;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
public abstract class UuidEntity {

  @Id
  @Setter(AccessLevel.PROTECTED)
  @JdbcTypeCode(SqlTypes.BINARY)
  @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
  private UUID id = UUID.randomUUID();

  @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", updatable = false, insertable = false, nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @PrePersist
  void ensureId() {
    if (id == null)
      id = UUID.randomUUID();
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null)
      return false;
    if (Hibernate.getClass(this) != Hibernate.getClass(o))
      return false;
    UuidEntity other = (UuidEntity) o;
    return id != null && id.equals(other.id);
  }

  @Override
  public final int hashCode() {
    return (id != null) ? id.hashCode() : 0;
  }
}
