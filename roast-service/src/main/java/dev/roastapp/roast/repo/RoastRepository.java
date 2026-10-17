package dev.roastapp.roast.repo;

import dev.roastapp.roast.domain.RoastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoastRepository extends JpaRepository<RoastEntity, UUID> {

    List<RoastEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<RoastEntity> findByIdAndUserId(UUID id, String userId);

    Optional<RoastEntity> findFirstByStatsHashAndToneAndPromptVersion(
            String statsHash, dev.roastapp.common.dto.Tone tone, String promptVersion);
}
