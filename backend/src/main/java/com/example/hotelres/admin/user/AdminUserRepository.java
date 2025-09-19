package com.example.hotelres.admin.user;
import java.time.LocalDateTime;
import java.util.List;

import com.example.hotelres.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface AdminUserRepository extends JpaRepository<User, Long> {

  interface AdminUserRow {
    Long getId();
    String getLoginId();
    String getName();
    String getEmail();
    String getPhone();
    String getRole();
    String getStatus();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
  }

  @Query(value = """
    SELECT
      id,
      login_id  AS loginId,
      name,
      email,
      phone,
      role,
      status,
      created_at AS createdAt,
      updated_at AS updatedAt
    FROM users
    WHERE (:q IS NULL OR LOWER(login_id) LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(name)  LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(email) LIKE LOWER(CONCAT('%',:q,'%')))
      AND (:role IS NULL OR role = :role)
      AND (:status IS NULL OR status = :status)
    ORDER BY created_at DESC
  """, nativeQuery = true)
  List<AdminUserRow> search(@Param("q") String q,
                            @Param("role") String role,
                            @Param("status") String status);

  @Query(value = """
    SELECT
      id,
      login_id  AS loginId,
      name,
      email,
      phone,
      role,
      status,
      created_at AS createdAt,
      updated_at AS UpdatedAt
    FROM users WHERE id = :id
  """, nativeQuery = true)
  AdminUserRow findViewById(@Param("id") Long id);

  @Transactional
  @Modifying
  @Query(value = "UPDATE users SET role=:role, updated_at=NOW() WHERE id=:id", nativeQuery = true)
  int updateRole(@Param("id") Long id, @Param("role") String role);

  // ✅ DELETED 관련 컬럼 갱신 제거 (우린 하드 삭제이므로)
  @Transactional
  @Modifying
  @Query(value = """
    UPDATE users
       SET status=:status,
           locked_at = CASE WHEN :status='LOCKED' THEN NOW() ELSE NULL END,
           updated_at = NOW()
     WHERE id=:id
  """, nativeQuery = true)
  int updateStatus(@Param("id") Long id, @Param("status") String status);
}
