package com.tripcraft.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tripcraft.entity.TripDay;
import com.tripcraft.entity.TripPlan;
import com.tripcraft.mapper.TripDayMapper;
import com.tripcraft.mapper.TripPlanMapper;
import com.tripcraft.service.TripPlanService;
import com.tripcraft.service.impl.TripPlanServiceImpl;
import com.tripcraft.vo.TripDetailVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TripPlanServiceImpl extends ServiceImpl<TripPlanMapper, TripPlan> implements TripPlanService {

    private final TripDayMapper tripDayMapper;

    /**
     * 创建行程，并基于日期自动生成分天子记录（强事务保证）。<br>
     * 用户只需提供行程标题、起止日期等基本信息，系统自动计算天数...
     *
     * @param plan 前端传入的行程基本信息
     * @return 已持久化的行程对象（id 已由 MyBatis-Plus 自动回填）
     * @throws IllegalArgumentException 当结束日期小于开始日期时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TripPlan createTripWithDays(TripPlan plan) {
        // 1.计算总天数（结束日期-开始日期）
        long daysBetween = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
        if (daysBetween <= 0) {
            throw new IllegalArgumentException("结束日期必须大于或等于开始日期");
        }
        plan.setTotalDays((int) daysBetween);

        // 2.保存行程主表
        this.save(plan);

        // 3.循环生成每一天都日程记录并批量入库
        List<TripDay> dayList = new ArrayList<>();
        LocalDate currentDate = plan.getStartDate();
        for (int i = 1; i <= daysBetween; i++) {
            TripDay day = new TripDay();
            day.setTripId(plan.getId());
            day.setDayIndex(i);
            day.setPlanDate(currentDate);
            tripDayMapper.insert(day);
            currentDate = currentDate.plusDays(1);
        }
        return plan;
    }

    /**
     * 根据行程 ID 查询完整行程详情（含行程主信息 + 分天子列表）。<br>
     * 先查 trip_plan 获取行程基本信息，再查 trip_day 获取该行程下的所有天数明细，
     * 最后组装为 TripDetailVO 统一返回，避免前端多次请求。
     *
     * @param tripId 行程 ID
     * @return TripDetailVO（包含 plan + days），若行程不存在则返回 null
     */
    @Override
    public TripDetailVO getTripDetail(Long tripId) {
        TripPlan plan = this.getById(tripId);
        if (plan == null) {
            return null;
        }

        LambdaQueryWrapper<TripDay> query = new LambdaQueryWrapper<>();
        query.eq(TripDay::getTripId, tripId).orderByAsc(TripDay::getDayIndex);
        List<TripDay> days = tripDayMapper.selectList(query);

        TripDetailVO vo = new TripDetailVO();
        vo.setPlan(plan);
        vo.setDays(days);
        return vo;
    }

}
