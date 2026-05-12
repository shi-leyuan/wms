package org.jeecg.modules.wms.wave.strategy;

import org.jeecg.modules.wms.outorder.entity.WmsOutOrders;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrdersAllocation;

import java.util.List;
import java.util.Map;

/**
 * 波次策略接口
 */
public abstract class IWaveStrategy {

    /**
     * 下一个策略
     */
    private IWaveStrategy nextStrategy;

    public void setNextStrategy(IWaveStrategy nextStrategy) {
        this.nextStrategy = nextStrategy;
    }

    /**
     * 创建波次
     * @param orders 待处理的订单列表
     * @param allocationsMap 订单分配明细映射
     */
    abstract void process(List<WmsOutOrders> orders,
                          Map<String, List<WmsOutOrdersAllocation>> allocationsMap);

    /**
     * 获取策略类型
     */
    abstract String getStrategyType();

    /**
     * 获取策略优先级(数值越小优先级越高)
     */
    abstract int getPriority();

    /**
     * 下一个策略
     */
    public IWaveStrategy  next(){
        return nextStrategy;
    }
    /**
     * 执行下一个策略
     */
    public void processNext(List<WmsOutOrders> orders, Map<String, List<WmsOutOrdersAllocation>> allocationsMap) {
        if (orders.size() > 0 && next() != null) {
            next().process(orders, allocationsMap);
        }
    }
}