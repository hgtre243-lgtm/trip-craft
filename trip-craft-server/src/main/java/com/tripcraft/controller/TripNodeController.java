package com.tripcraft.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripcraft.common.Result;
import com.tripcraft.entity.TripNode;
import com.tripcraft.service.TripNodeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripNodeController {
    private final TripNodeService tripNodeService;

    // 1. 获取某天的节点列表
    @GetMapping("/days/{dayId}/nodes")
    public Result<List<TripNode>> getnodes(@PathVariable Long dayId) {
        return Result.success(tripNodeService.listByDayId(dayId));

    }

    // 2. 向某天添加新节点
    @PostMapping("/nodes")
    public Result<TripNode> addNode(@RequestBody TripNode node) {
        return Result.success(tripNodeService.addNode(node));

    }

    // 3. 删除指定节点
    @DeleteMapping("/nodes/{id}")
    public Result<Void> deleteNode(@PathVariable Long id) {
        tripNodeService.removeById(id);
        return Result.success();

    }

    // 4. 核心：拖拽完成后批量更新排序
    @PutMapping("/days/{dayId}/nodes/reorder")
    public Result<Void> reorderNodes(@PathVariable Long dayId, @RequestBody List<Long> orderedNodeIds) {
        tripNodeService.reorderNodes(dayId, orderedNodeIds);
        return Result.success();
    }
}
