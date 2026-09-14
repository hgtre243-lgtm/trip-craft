package com.tripcraft.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data

@TableName("trip_plan")
// //@TableName("trip_plan") 的含义
// 这个注解来自 MyBatis-Plus，作用是：将当前 Java 实体类 TripPlan 与数据库中的 trip_plan 表建立映射关系。
public class TripPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private String coverColor;
    private LocalDateTime createdAt;
}
