package com.tripcraft.controller;

import com.tripcraft.common.Result;
import com.tripcraft.entity.TripMarker;
import com.tripcraft.service.TripMarkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 旅行足迹打卡标记 控制器
 */
@RestController
@RequestMapping("/api/markers")
@RequiredArgsConstructor // 推荐：Spring 官方推荐的构造器注入，替代字段上的 @Autowired
public class TripMarkerController {

    private final TripMarkerService tripMarkerService;

    /**
     * 1. 查询所有打卡点列表
     */
    @GetMapping
    public Result<List<TripMarker>> list() {
        List<TripMarker> list = tripMarkerService.list();
        return Result.success(list);
    }

    /**
     * 2. 新增打卡标记
     */
    @PostMapping
    public Result<TripMarker> add(@RequestBody TripMarker marker) {
        tripMarkerService.save(marker);
        return Result.success(marker);
    }

    /**
     * 3. 删除指定打卡标记（RESTful 幂等删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tripMarkerService.removeById(id);
        return Result.success();
    }
}
