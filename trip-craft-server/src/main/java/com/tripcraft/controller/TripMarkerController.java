package com.tripcraft.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tripcraft.common.Result;
import com.tripcraft.entity.TripMarker;
import com.tripcraft.service.TripMarkerService;
import com.tripcraft.vo.FootprintStatsVO;
import org.springframework.util.StringUtils;
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
     * 1. 查询打卡点列表（支持按省份筛选，如 /api/markers?province=浙江省）
     */
    @GetMapping
    public Result<List<TripMarker>> list(@RequestParam(required = false) String province) {
        LambdaQueryWrapper<TripMarker> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(province)) {
            wrapper.eq(TripMarker::getProvince, province);
        }
        wrapper.orderByDesc(TripMarker::getCreatedAt);
        return Result.success(tripMarkerService.list(wrapper));
    }

    /**
     * 2. 新增打卡标记
     */
    @PostMapping
    public Result<TripMarker> add(@RequestBody TripMarker marker) {
        tripMarkerService.saveMarker(marker);
        return Result.success(marker);
    }

    /**
     * 3. 删除指定打卡标记（RESTful 幂等删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tripMarkerService.deleteMarker(id);
        return Result.success();
    }

    /**
     * 4. 获取全国足迹统计指标（带 Redis 缓存的高性能接口）
     */
    @GetMapping("/stats")
    public Result<FootprintStatsVO> getStats() {
        return Result.success(tripMarkerService.getFootprintStats());
    }
}
