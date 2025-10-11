package com.example.hotelres.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface NoticeRepo extends
        JpaRepository<Notice, Long>,
        JpaSpecificationExecutor<Notice> {

    // pinned 우선 정렬이 필요하면 아래 메소드 사용 가능(선택)
    Page<Notice> findAllByOrderByPinnedDescIdDesc(Pageable pageable);

    Optional<Notice> findFirstByIdLessThanOrderByIdDesc(Long id);
    Optional<Notice> findFirstByIdGreaterThanOrderByIdAsc(Long id);
}
