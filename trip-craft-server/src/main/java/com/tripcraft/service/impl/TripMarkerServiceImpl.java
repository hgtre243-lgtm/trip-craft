package com.tripcraft.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tripcraft.entity.TripMarker;
import com.tripcraft.mapper.TripMarkerMapper;
import com.tripcraft.service.TripMarkerService;
import org.springframework.stereotype.Service;

/**
 * 打卡点 Service 实现.
 * <p>
 * 增删查逻辑直接复用 {@link ServiceImpl} / {@code IService} 提供的能力。
 * </p>
 */
@Service
public class TripMarkerServiceImpl extends ServiceImpl<TripMarkerMapper, TripMarker> implements TripMarkerService {
}
