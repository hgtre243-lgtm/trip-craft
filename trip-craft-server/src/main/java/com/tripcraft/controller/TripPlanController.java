package com.tripcraft.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripcraft.common.Result;
import com.tripcraft.entity.TripPlan;
import com.tripcraft.service.TripPlanService;
import com.tripcraft.vo.TripDetailVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trip-plan")
@RequiredArgsConstructor
public class TripPlanController {

    private final TripPlanService tripPlanService;

    // 1. 获取所有行程列表
    @GetMapping
    public Result<List<TripPlan>> list() {
        return Result.success(tripPlanService.list());
    }

    // 2.获取行程详情
    @PostMapping
    public Result<TripPlan> create(@RequestBody TripPlan Plan) {
        return Result.success(tripPlanService.createTripWithDays(Plan));
    }

    // 3.获取单个行程详情及其分天列表
    @GetMapping("/{id}")
    public Result<TripDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(tripPlanService.getTripDetail(id));
    }
}
