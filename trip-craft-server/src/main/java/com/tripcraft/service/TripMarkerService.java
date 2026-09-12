package com.tripcraft.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tripcraft.entity.TripMarker;
import com.tripcraft.vo.FootprintStatsVO;

/**
 * 打卡点 Service.
 * <p>
 * 增删查直接用 {@link IService} 内置能力，暂无需自定义方法。
 * </p>
 */
public interface TripMarkerService extends IService<TripMarker> {
    FootprintStatsVO getFootprintStats();

    boolean saveMarker(TripMarker marker);

    boolean deleteMarker(Long id);
}
