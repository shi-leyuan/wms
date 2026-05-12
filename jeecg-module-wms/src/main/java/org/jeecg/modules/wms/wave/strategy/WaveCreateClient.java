package org.jeecg.modules.wms.wave.strategy;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrders;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrdersAllocation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 创建波次客户端类
 */
public class WaveCreateClient {

    private IWaveStrategy firstStrategy;
    /**
     * 构造方法
     *
     * @param strategies         全部波次策略
     * @param selectedStrategies 选中的波次策略编码
     */
    public WaveCreateClient(List<IWaveStrategy> strategies, List<String> selectedStrategies) {
        // 从全部策略中，获取选中的策略，按优先级排序策略
        List<IWaveStrategy> filteredStrategies = strategies.stream()
                .filter(strategy -> selectedStrategies.contains(strategy.getStrategyType()))
                .sorted((s1, s2) -> Integer.compare(s1.getPriority(), s2.getPriority()))
                .collect(Collectors.toList());
        if(CollectionUtils.isEmpty( filteredStrategies )){
            return;
        }
        //将filteredStrategies中的策略对象组成责任链
        //获取第一个策略
        firstStrategy = filteredStrategies.get(0);
        //将剩余的策略设置成第一个策略的nextStrategy
        for (int i = 1; i < filteredStrategies.size(); i++) {
            filteredStrategies.get(i - 1).setNextStrategy(filteredStrategies.get(i));
        }

    }

    /**
     * 处理订单波次分配
     */
    public void process(List<WmsOutOrders> orders,
                        Map<String, List<WmsOutOrdersAllocation>> allocationsMap) {
        if(firstStrategy == null){
            throw new JeecgBootException("没有配置波次策略");
        }
        firstStrategy.process(orders, allocationsMap);
    }
}