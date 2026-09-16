package com.tripcraft.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tripcraft.entity.TripNode;
import com.tripcraft.mapper.TripNodeMapper;
import com.tripcraft.service.TripNodeService;
import com.tripcraft.service.impl.TripNodeServiceImpl;

@Service
public class TripNodeServiceImpl extends ServiceImpl<TripNodeMapper, TripNode> implements TripNodeService {

    /**
     * 1. 按游览次序升序查询某天的所有节点
     */
    @Override
    public List<TripNode> listByDayId(Long dayId) {
        LambdaQueryWrapper<TripNode> query = new LambdaQueryWrapper<>();
        query.eq(TripNode::getDayId, dayId).orderByAsc(TripNode::getOrderNum);
        return this.list(query);
    }

    /**
     * 2. 新增节点（自动推算当前最大的 orderNum + 1）
     */
    @Override
    public TripNode addNode(TripNode node) {
        LambdaQueryWrapper<TripNode> query = new LambdaQueryWrapper<>();
        query.eq(TripNode::getDayId, node.getDayId()).orderByAsc(TripNode::getOrderNum).last("LIMIT 1");
        TripNode maxNode = this.getOne(query);

        int nextOrder = (maxNode == null) ? 1 : maxNode.getOrderNum() + 1;
        node.setOrderNum(nextOrder);
        this.save(node);
        return node;
    }

    /**
     * 3. 拖拽核心：根据前端传来的有序 ID 列表，批量重置 order_num（强事务保证）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderNodes(Long dayId, List<Long> orderedNodeIds) {
        for (int i = 0; i < orderedNodeIds.size(); i++) {
            Long nodeId = orderedNodeIds.get(i);
            int newOrder = i + 1;
            this.lambdaUpdate()
                    .eq(TripNode::getId, nodeId)
                    .eq(TripNode::getDayId, dayId)
                    .set(TripNode::getOrderNum, newOrder)
                    .update();

        }
    }
}
