package com.example.hotelres.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByEmail(String email);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);

     // ✅ userId만 필요할 때 가볍게 조회 (엔티티 전체 로딩 X)
    @Query("select u.id from User u where u.loginId = :loginId")
    Optional<Long> findIdByLoginId(@Param("loginId") String loginId);
}
