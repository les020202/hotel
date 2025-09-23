package com.example.hotelres.main.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import com.example.hotelres.admin.hotel.*;

@Repository
public interface HotelJpaRepository
        extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {
}
