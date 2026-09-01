package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.store.StoreResponse;
import com.jingxu.catmaintain.mapper.MerchantStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final MerchantStoreMapper merchantStoreMapper;

    @GetMapping
    public List<StoreResponse> listActive() {
        return merchantStoreMapper.findActiveStores().stream().map(StoreResponse::from).toList();
    }
}
