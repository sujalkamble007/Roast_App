package dev.roastapp.identity.repo;

import dev.roastapp.common.dto.Provider;
import dev.roastapp.identity.domain.LinkedAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccountEntity, UUID> {

    Optional<LinkedAccountEntity> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}
