package org.jeecg.modules.wms.wmstask.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.wms.wmstask.entity.WmsTasks;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.wms.wmstask.entity.WmsTasksRecords;

/**
 * @Description: 任务表
 * @Author: jeecg-boot
 * @Date: 2026-04-28
 * @Version: V1.0
 */
public interface IWmsTasksService extends IService<WmsTasks> {

    /**
     * 创建收货任务
     *
     * @param
     * @param
     * @return
     */
    void createReceiveTask(String orderId, String operator);

    /**
     * 查询待办理任务列表
     *
     * @param wmsTasks
     * @return
     */
    IPage<WmsTasks> list(WmsTasks wmsTasks, Integer pageNo, Integer pageSize);

    /**
     * 收货方法
     * 共用
     */
    void receive(WmsTasksRecords wmsTasksRecords);

    /**
     * 执行任务
     */
    public WmsTasks execute(WmsTasksRecords wmsTasksRecords);

    /**
     * 根据入库单创建上架任务
     *
     * @param stockInOrderId 入库单ID
     */
    void createPutawayTask(String stockInOrderId);

    /**
     * 执行上架任务
     *
     * @param wmsTasksRecords 上架执行记录
     */
    void putaway(WmsTasksRecords wmsTasksRecords);

    /**
     * 生成任务编号
     * 规则: TSK+年月日+5位序号，序号使用redis自增序号实现
     */
    public String generateTaskCode();

}
