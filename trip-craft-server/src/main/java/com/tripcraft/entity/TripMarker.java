package com.tripcraft.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 旅行地图打卡标记 (trip_marker).
 */
@Data
@TableName("trip_marker")
public class TripMarker {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标记地点名称 */
    private String title;

    /** 经度 (GCJ-02) */
    private BigDecimal longitude;

    /** 纬度 (GCJ-02) */
    private BigDecimal latitude;

    /** 打卡备注 */
    private String notes;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
