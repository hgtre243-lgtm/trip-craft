package com.tripcraft.vo;

import java.util.List;
import com.tripcraft.entity.TripDay;
import com.tripcraft.entity.TripPlan;
import lombok.Data;

@Data
public class TripDetailVO {
    private TripPlan plan;
    private List<TripDay> days;
}
