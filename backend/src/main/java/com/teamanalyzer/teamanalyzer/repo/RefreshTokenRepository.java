// backend/src/main/java/com/teamanalyzer/teamanalyzer/repo/RefreshTokenRepository.java
package com.teamanalyzer.teamanalyzer.repo;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.teamanalyzer.teamanalyzer.domain.RefreshToken;

@Repository
@Transactional(readOnly = true)
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  // ------------------ Portables JPQL (byte[]) ------------------

  @Query("""
      select distinct rt
        from RefreshToken rt
        join fetch rt.user u
        left join fetch u.roles
       where rt.tokenHash = :hash
         and rt.revoked = false
      """)
  Optional<RefreshToken> findActiveByHashWithUserAndRolesBytes(@Param("hash") byte[] hash);

  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update RefreshToken rt
         set rt.revoked = true
       where rt.tokenHash = :hash
         and rt.revoked = false
      """)
  int revokeByHashBytes(@Param("hash") byte[] hash);

  /** Convenience-Lookup ohne Fetch-Graph; sparsam einsetzen. */
  Optional<RefreshToken> findByTokenHash(byte[] tokenHash);

  // ---------- String-Wrapper (Base64-URL) für Controller ----------

  /**
   * Erwartet einen Base64-URL-kodierten SHA-256-Hash (ohne Padding) und
   * wandelt ihn portabel in das byte[] für die JPQL-Query um.
   */
  default Optional<RefreshToken> findActiveByHashWithUserAndRoles(String base64UrlHash) {
    byte[] hash = Base64.getUrlDecoder().decode(base64UrlHash);
    return findActiveByHashWithUserAndRolesBytes(hash);
  }

  /**
   * Idempotentes Revoke per Base64-URL-Hash.
   * 
   * @return 0 oder 1
   */
  @Transactional
  default int revokeByHash(String base64UrlHash) {
    byte[] hash = Base64.getUrlDecoder().decode(base64UrlHash);
    return revokeByHashBytes(hash);
  }
}
