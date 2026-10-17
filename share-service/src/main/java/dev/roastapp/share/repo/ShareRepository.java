package dev.roastapp.share.repo;

import dev.roastapp.share.domain.ShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareRepository extends JpaRepository<ShareEntity, String> {

    Optional<ShareEntity> findByIdAndRevokedAtIsNull(String id);

    Optional<ShareEntity> findFirstByRoastIdAndRevokedAtIsNull(UUID roastId);
}
