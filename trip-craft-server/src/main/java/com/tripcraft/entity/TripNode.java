package com.tripcraft.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("trip_node")
public class TripNode {
    // 主键ID
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dayId;
    private String spotName;
    // 经度
    private BigDecimal longitude;
    // 纬度
    private BigDecimal latitude;
    // 当天游览次序（1, 2, 3...）
    private Integer orderNum;
    private String notes;
    private LocalDateTime createdAt;

}
