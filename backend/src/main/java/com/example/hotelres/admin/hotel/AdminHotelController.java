package com.example.hotelres.admin.hotel;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/hotels")
@RequiredArgsConstructor
public class AdminHotelController {

    private final AdminHotelService service;

    @GetMapping
    public Page<HotelDtos.ListItem> list(
        @RequestParam(required=false) String region,
        @RequestParam(required=false) String q,
        @RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="100") int size
    ){
        return service.list(region, q, page, size);
    }

    @GetMapping("/{id}")
    public HotelDtos.Detail detail(@PathVariable Long id){
        return service.detail(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody HotelDtos.UpdateReq req){
        service.update(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
