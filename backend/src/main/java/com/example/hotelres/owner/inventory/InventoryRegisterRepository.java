package com.example.hotelres.owner.inventory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class InventoryRegisterRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * booking_day 대량 등록.
     *  - overwrite=false: 존재하면 건너뜀(no-op)
     *  - overwrite=true : 존재하면 allotment/price/status만 덮어쓰기( booked 는 유지 )
     */
    public int bulkUpsert(Long hotelId, Long roomTypeId,
                          List<LocalDate> dates,
                          Integer price, Integer allotment, String status,
                          boolean overwrite) {
        if (dates == null || dates.isEmpty()) return 0;

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("""
            INSERT INTO booking_day (hotel_id, room_type_id, stay_date, allotment, booked, price, status)
            VALUES
        """);

        int idx = 0;
        for (LocalDate d : dates) {
            if (idx++ > 0) sql.append(", ");
            sql.append("(?, ?, ?, ?, 0, ?, ?)");
            params.add(hotelId);
            params.add(roomTypeId);
            params.add(java.sql.Date.valueOf(d));
            // null을 0/OPEN으로 넣도록 설계(컬럼이 NOT NULL일 가능성 고려)
            params.add(allotment == null ? 0 : allotment);
            params.add(price == null ? 0 : price);
            params.add(status == null ? "OPEN" : status);
        }

        if (overwrite) {
            // 기존 행이 있으면 allotment/price/status 만 업데이트 (booked 유지)
            sql.append("""
              ON DUPLICATE KEY UPDATE
                allotment = VALUES(allotment),
                price     = VALUES(price),
                status    = VALUES(status)
            """);
        } else {
            // 기존 행이 있으면 그대로 두기
            sql.append(" ON DUPLICATE KEY UPDATE stay_date = stay_date");
        }

        var q = em.createNativeQuery(sql.toString());
        int i = 1;
        for (Object p : params) q.setParameter(i++, p);

        // MySQL/MariaDB: INSERT+UPDATE 모두 '영향 받은 행 수'로 카운팅됨
        return q.executeUpdate();
    }
}
