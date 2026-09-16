package com.tripcraft.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tripcraft.entity.TripNode;

import java.util.List;

public interface TripNodeService extends IService<TripNode> {
    List<TripNode> listByDayId(Long dayId);

    TripNode addNode(TripNode node);

    void reorderNodes(Long dayId, List<Long> orderedNodeIds);
}
