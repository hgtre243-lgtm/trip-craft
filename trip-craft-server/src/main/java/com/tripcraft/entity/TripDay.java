package com.tripcraft.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@TableName("trip_day")
public class TripDay {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tripId;
    private Integer dayIndex;
    private LocalDate planDate;
    private LocalDateTime createdAt;
}
