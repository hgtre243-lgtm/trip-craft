package com.tripcraft.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tripcraft.entity.TripMarker;

import java.util.List;

/**
 * 打卡点 Service.
 */
public interface TripMarkerService extends IService<TripMarker> {

    /**
     * 查询所有打卡点，按创建时间倒序返回.
     */
    List<TripMarker> listMarkers();

    /**
     * 保存一个新的打卡点，返回带自增 id 与创建时间的完整记录.
     */
    TripMarker createMarker(TripMarker marker);
}
