package com.tripcraft.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tripcraft.entity.TripMarker;
import com.tripcraft.mapper.TripMarkerMapper;
import com.tripcraft.service.TripMarkerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打卡点 Service 实现.
 */
@Service
public class TripMarkerServiceImpl extends ServiceImpl<TripMarkerMapper, TripMarker> implements TripMarkerService {

    @Override
    public List<TripMarker> listMarkers() {
        return list(Wrappers.<TripMarker>lambdaQuery().orderByDesc(TripMarker::getCreatedAt));
    }

    @Override
    public TripMarker createMarker(TripMarker marker) {
        marker.setCreatedAt(LocalDateTime.now());
        save(marker); // 自增主键回填到 marker.id
        return marker;
    }
}
