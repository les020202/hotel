package com.example.hotelres.amenity;

import com.example.hotelres.amenity.dto.AmenityDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
public class AmenityController {
    private final AmenityService service;

    @GetMapping
    public List<AmenityDto> list(@RequestParam(required = false) String scope) {
        return service.list((scope == null || scope.isBlank()) ? "HOTEL" : scope);
    }
}
