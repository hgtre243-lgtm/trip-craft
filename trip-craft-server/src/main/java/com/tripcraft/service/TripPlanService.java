package com.tripcraft.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tripcraft.entity.TripPlan;
import com.tripcraft.vo.TripDetailVO;

public interface TripPlanService extends IService<TripPlan> {
    TripDetailVO getTripDetail(Long planId);

    TripPlan createTripWithDays(TripPlan tripPlan);

}
