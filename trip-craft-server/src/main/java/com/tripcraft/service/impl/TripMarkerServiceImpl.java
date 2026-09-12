package com.tripcraft.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripcraft.entity.TripMarker;
import com.tripcraft.mapper.TripMarkerMapper;
import com.tripcraft.service.TripMarkerService;
import com.tripcraft.vo.FootprintStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripMarkerServiceImpl extends ServiceImpl<TripMarkerMapper, TripMarker> implements TripMarkerService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis 缓存键名
    private static final String FOOTPRINT_STATS_KEY = "trip:footprint:stats";

    @Override
    public FootprintStatsVO getFootprintStats() {
        // 1. 先查 Redis 缓存
        String cachedJson = redisTemplate.opsForValue().get(FOOTPRINT_STATS_KEY);
        if (StringUtils.hasText(cachedJson)) {
            try {
                log.info("🎯 命中 Redis 缓存足迹统计数据");
                return objectMapper.readValue(cachedJson, FootprintStatsVO.class);
            } catch (JsonProcessingException e) {
                log.error("Redis 反序列化失败，转为查询数据库", e);
            }
        }

        // 2. 缓存未命中（Cache Miss），查询数据库执行聚合统计
        log.info("🔍 查询 MySQL 数据库并重新构建足迹统计缓存");
        FootprintStatsVO stats = buildStatsFromDb();

        // 3. 回写 Redis 并设置 2 小时过期时间（防止冷数据常驻内存）
        try {
            String jsonStr = objectMapper.writeValueAsString(stats);
            redisTemplate.opsForValue().set(FOOTPRINT_STATS_KEY, jsonStr, Duration.ofHours(2));
        } catch (JsonProcessingException e) {
            log.error("写入 Redis 失败", e);
        }

        return stats;
    }

    /**
     * Cache-Aside 写入模式：先写库，后删缓存
     */
    @Override
    public boolean saveMarker(TripMarker marker) {
        boolean success = this.save(marker);
        if (success) {
            evictCache();
        }
        return success;
    }

    /**
     * Cache-Aside 删除模式：先删库，后删缓存
     */
    @Override
    public boolean deleteMarker(Long id) {
        boolean success = this.removeById(id);
        if (success) {
            evictCache();
        }
        return success;
    }

    private void evictCache() {
        redisTemplate.delete(FOOTPRINT_STATS_KEY);
        log.info("🗑️ 数据库变更，成功失效 Redis 缓存：{}", FOOTPRINT_STATS_KEY);
    }

    private FootprintStatsVO buildStatsFromDb() {
        FootprintStatsVO vo = new FootprintStatsVO();

        // 1. 查总打卡数
        long total = this.count();
        vo.setTotalMarkers((int) total);

        // 2. SQL GROUP BY 聚合省份：SELECT province, COUNT(1) AS cnt FROM trip_marker WHERE
        // province != '' GROUP BY province
        QueryWrapper<TripMarker> wrapper = new QueryWrapper<>();
        wrapper.select("province", "COUNT(1) as cnt")
                .isNotNull("province")
                .ne("province", "")
                .groupBy("province");

        List<Map<String, Object>> maps = this.listMaps(wrapper);
        List<FootprintStatsVO.ProvinceStat> provinceList = new ArrayList<>();

        for (Map<String, Object> map : maps) {
            FootprintStatsVO.ProvinceStat stat = new FootprintStatsVO.ProvinceStat();
            stat.setProvince((String) map.get("province"));
            stat.setCount(((Number) map.get("cnt")).longValue());
            provinceList.add(stat);
        }

        vo.setProvinceList(provinceList);
        vo.setVisitedProvinceCount(provinceList.size());

        // 计算全国覆盖率（总共 34 个省级行政区）
        double rate = BigDecimal.valueOf((double) provinceList.size() / 34.0 * 100)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
        vo.setCoverageRate(rate);

        return vo;
    }
}
