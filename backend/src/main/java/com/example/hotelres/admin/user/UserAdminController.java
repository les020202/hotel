package com.example.hotelres.admin.user;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.example.hotelres.admin.user.AdminUserRepository.AdminUserRow;
import com.example.hotelres.admin.user.dto.UpdateRoleRequest;
import com.example.hotelres.admin.user.dto.UpdateStatusRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

  private final AdminUserRepository repo;

  @GetMapping
  public List<AdminUserRow> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) String role,
                                 @RequestParam(required = false) String status) {
    return repo.search(emptyToNull(q), emptyToNull(role), emptyToNull(status));
  }

  @PutMapping("/{id}/role")
  public ResponseEntity<AdminUserRow> changeRole(@PathVariable Long id,
                                                 @RequestBody UpdateRoleRequest req) {
    String role = req.getRole();
    if (role == null || !role.matches("ROLE_USER|ROLE_ADMIN|ROLE_OWNER")) {
      return ResponseEntity.badRequest().build();
    }
    int n = repo.updateRole(id, role);
    if (n == 0) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(repo.findViewById(id));
  }

  @PutMapping("/{id}/status")
  public ResponseEntity<AdminUserRow> changeStatus(@PathVariable Long id,
                                                   @RequestBody UpdateStatusRequest req) {
    String status = req.getStatus();
    // ✅ DELETED 는 드롭다운에서 제외했으므로 서버도 허용하지 않음
    if (status == null || !status.matches("ACTIVE|LOCKED|INACTIVE")) {
      return ResponseEntity.badRequest().build();
    }
    int n = repo.updateStatus(id, status);
    if (n == 0) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(repo.findViewById(id));
  }

  // ✅ 하드 삭제 (DB에서 완전 제거)
  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> delete(@PathVariable Long id) {
    try {
      repo.deleteById(id);                 // JPA 기본 메서드 (DELETE FROM users WHERE id=?)
      return ResponseEntity.noContent().build();
    } catch (DataIntegrityViolationException e) {
      // 외래키 참조 등으로 삭제가 막힐 때
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", "연관 데이터가 있어 삭제할 수 없습니다."));
    }
  }

  private String emptyToNull(String s){ return (s==null || s.isBlank()) ? null : s; }
}
