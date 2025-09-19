package com.example.hotelres.reservation;

import com.example.common.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingHoldRepository bookingHoldRepository;

    /** 결제 시점: holdCode → Booking 생성 */
    @Transactional
    public Booking createFromHold(String holdCode, String orderId, int amount) {
        var hold = bookingHoldRepository.findByHoldCode(holdCode);
        if (hold == null) {
            throw new ApiException(404, "HOLD_NOT_FOUND", "홀드를 찾을 수 없습니다.");
        }

        if (hold.getTotalAmount() != amount) {
            throw new ApiException(409, "AMOUNT_MISMATCH", "결제 금액이 홀드 금액과 다릅니다.");
        }

        // 이미 동일 voucher_no(=orderId) 로 예약이 있으면 그대로 반환(멱등)
        var exists = bookingRepository.findByVoucherNo(orderId).orElse(null);
        if (exists != null) return exists;

        Booking b = new Booking();
        b.setUserId(hold.getUserId());
        b.setHotelId(hold.getHotelId());
        b.setStatus(BookingStatus.PENDING);
        b.setCheckIn(hold.getCheckIn());
        b.setCheckOut(hold.getCheckOut());
        b.setNights((int) ChronoUnit.DAYS.between(hold.getCheckIn(), hold.getCheckOut()));
        b.setGuests(hold.getGuests());
        b.setTotalAmount(hold.getTotalAmount());
        b.setCurrency(hold.getCurrency());
        b.setVoucherNo(orderId); // Toss orderId == voucher_no

        Booking saved = bookingRepository.save(b);

        // 하드 홀드 → 실제 예약으로 승격: booked 수량은 그대로 유지, hold row는 정리
        bookingHoldRepository.delete(hold);

        return saved;
    }

    /** 예약 확정 */
    @Transactional
    public void confirm(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }
    }
}
