package dev.roastapp.identity.repo;

import dev.roastapp.common.dto.Provider;
import dev.roastapp.identity.domain.LinkedAccountEntity;
import dev.roastapp.identity.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
}
