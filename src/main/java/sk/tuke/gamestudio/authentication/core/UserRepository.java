package sk.tuke.gamestudio.authentication.core;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.tuke.gamestudio.authentication.core.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);
    boolean existsByNickname(String nickname);
    java.util.List<User> findByNicknameIsNotNull();
    Optional<User> findByVerificationToken(String token);
    Optional<User> findByResetToken(String token);
}