package com.tripcraft.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class FootprintStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer totalMarkers; // 累计打卡总数
    private Integer visitedProvinceCount; // 已点亮省份数量
    private Double coverageRate; // 全国覆盖率（百分比）
    private List<ProvinceStat> provinceList; // 各省份统计明细

    @Data
    public static class ProvinceStat implements Serializable {
        private String province; // 省份名称
        private Long count; // 该省打卡数量
    }
}
