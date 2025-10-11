package com.example.hotelres.amenity;

import com.example.hotelres.amenity.dto.AmenityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityService {
    private final AmenityRepository repo;

    @Transactional(readOnly = true)
    public List<AmenityDto> list(String scope) {
        return repo.list(scope);
    }
}
