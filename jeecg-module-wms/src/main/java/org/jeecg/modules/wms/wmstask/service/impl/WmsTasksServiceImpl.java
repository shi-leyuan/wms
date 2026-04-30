package org.jeecg.modules.wms.wmstask.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrderItems;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrders;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrderItemsService;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrdersService;
import org.jeecg.modules.wms.wmstask.entity.WmsTasks;
import org.jeecg.modules.wms.wmstask.entity.WmsTasksRecords;
import org.jeecg.modules.wms.wmstask.mapper.WmsTasksMapper;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksRecordsService;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Description: 任务表
 * @Author: jeecg-boot
 * @Date: 2026-04-28
 * @Version: V1.0
 */
@Service
public class WmsTasksServiceImpl extends ServiceImpl<WmsTasksMapper, WmsTasks> implements IWmsTasksService {


    @Autowired
    private IWmsStockInOrdersService stockInOrdersService;

    @Autowired
    private IWmsStockInOrderItemsService stockInOrderItemsService;

    @Autowired
    private IWmsTasksRecordsService wmsTasksRecordsService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 创建收货任务
     *
     * @param
     * @param
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void createReceiveTask(String orderId, String operator) {
        //查询入库单信息
        WmsStockInOrders stockInOrders = stockInOrdersService.getById(orderId);
        //仓库
        String warehouseId = stockInOrders.getWarehouseId();
        //入库单状态
        String status = stockInOrders.getStatus();
        //入库单状态非审核通过时不允许创建收货任务
        if (!WarehouseDictEnum.INBOUND_APPROVED.getCode().equals(status)) {
            throw new JeecgBootException("入库单审核通过方可创建收货任务");
        }
        //根据入库单id查询入库单明细列表
        List<WmsStockInOrderItems> stockInOrderItems = stockInOrderItemsService.selectByMainId(orderId);
        //根据入库明细列表创建收货任务
        for (WmsStockInOrderItems stockInOrderItem : stockInOrderItems) {
            //创建收货任务
            WmsTasks wmsTasks = new WmsTasks();
            //任务类型:收货任务
            wmsTasks.setTaskType(WarehouseDictEnum.TASK_TYPE_RECEIVING.getCode());
            //任务状态:已创建
            wmsTasks.setTaskStatus(WarehouseDictEnum.TASK_STATUS_CREATED.getCode());
            //任务创建时间
            wmsTasks.setCreateTime(new Date());
            //任务号
            wmsTasks.setTaskNumber(generateTaskCode());
            //目的仓库
            wmsTasks.setTargetWarehouseId(warehouseId);
            //入库单id
            wmsTasks.setStockInOrderId(stockInOrderItem.getOrderId());
            //入库单明细id
            wmsTasks.setStockInOrderItemId(stockInOrderItem.getId());
            //商品id
            wmsTasks.setProductId(stockInOrderItem.getProductId());
            //商品采购数量
            wmsTasks.setQuantity(stockInOrderItem.getExpectedQuantity());
            //完成数量为0
            wmsTasks.setCompletedQuantity(0);
            //执行人
            wmsTasks.setOperator(operator);
            //创建任务
            save(wmsTasks);
        }
        //更新入库单状态为收货中
        WmsStockInOrders stockInOrdersUpdate = new WmsStockInOrders();
        stockInOrdersUpdate.setId(orderId);//入库单id
        stockInOrdersUpdate.setStatus(WarehouseDictEnum.INBOUND_RECEIVING.getCode());//收货中

        boolean b = stockInOrdersService.updateById(stockInOrdersUpdate);
        if (!b) {
            throw new JeecgBootException("创建收货任务过程中更新入库单状态失败");
        }

        //根据入库单id更新入库单明细状态为收货中
        boolean update = stockInOrderItemsService.update(null,
                new LambdaUpdateWrapper<WmsStockInOrderItems>().eq(WmsStockInOrderItems::getOrderId, orderId)
                        .set(WmsStockInOrderItems::getStatus, WarehouseDictEnum.INBOUND_DETAIL_RECEIVING.getCode()));
        if (!update) {
            throw new JeecgBootException("创建收货任务过程中更新入库单明细状态失败");
        }
    }

    /**
     * 查询待办理任务列表
     *
     * @param wmsTasks
     * @return
     */
    public IPage<WmsTasks> list(WmsTasks wmsTasks, Integer pageNo, Integer pageSize) {
        Page<WmsTasks> page = PageHelper.startPage(pageNo, pageSize);
        List<WmsTasks> list = baseMapper.queryTaskList(wmsTasks);
        PageDTO<WmsTasks> wmsTasksPageDTO = new PageDTO<>();
        wmsTasksPageDTO.setRecords(list);
        wmsTasksPageDTO.setTotal(page.getTotal());
        wmsTasksPageDTO.setSize(page.getPageSize());
        wmsTasksPageDTO.setCurrent(page.getPageNum());
        wmsTasksPageDTO.setPages(page.getPages());
        return wmsTasksPageDTO;
    }

    /**
     * 收货
     *
     * @param wmsTasksRecords
     */
    @Transactional(rollbackFor = Exception.class)
    public void receive(WmsTasksRecords wmsTasksRecords) {
        //执行收货任务，向任务表中更新收货数量；如果收货完成，更新状态，记录收获记录
        WmsTasks wmsTasks = execute(wmsTasksRecords);
        //入库单明细ID
        String stockInOrderItemId = wmsTasks.getStockInOrderItemId();
        //更新入库单明细中的收货数量及不良品数量，当良品数量加不良品数量等采购数量，更新状态为收货完成
        stockInOrderItemsService.updateReceivedStatus(stockInOrderItemId);
        //更新入库单中收货总数量，如果所有明细的状态为收货完成则么入库单的状态为收货完成
        stockInOrdersService.updateReceivedStatus(wmsTasks.getStockInOrderId());
        //todo 如果入库单收货完成则创建上架任务,根据收货记录创建上架任务
    }

    /**
     * 执行任务
     */
    @Transactional(rollbackFor = Exception.class)
    public WmsTasks execute(WmsTasksRecords wmsTasksRecords) {
        //任务ID
        String taskId = wmsTasksRecords.getTaskId();
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new JeecgBootException("任务ID不能为空");
        }
        //查询任务信息
        WmsTasks wmsTasks = getById(taskId);
        if (wmsTasks == null) {
            throw new JeecgBootException("任务不存在");
        }
        //已完成数量
        Integer completedQuantity = ObjectUtils.defaultIfNull(wmsTasks.getCompletedQuantity(), 0);
        //执行数量
        Integer execQuantity = wmsTasksRecords.getExecQuantity();
        if (execQuantity + completedQuantity > wmsTasks.getQuantity()) {
            throw new JeecgBootException("执行数量不能大于计划数量");
        }
        //拷贝执行任务的信息到任务记录
        wmsTasksRecords.setTargetWarehouseId(wmsTasks.getTargetWarehouseId());//仓库id
        wmsTasksRecords.setStockInOrderId(wmsTasks.getStockInOrderId());//入库单id
        wmsTasksRecords.setStockInOrderItemId(wmsTasks.getStockInOrderItemId());//入库单明细id
        wmsTasksRecords.setWaveOrderId(wmsTasks.getWaveOrderId());//波次id 用于波次管理
        wmsTasksRecords.setWaveSkuSummaryId(wmsTasks.getWaveSkuSummaryId());//波次拣货明细id 用于波次管理
        wmsTasksRecords.setTaskId(wmsTasks.getId());//任务id
        wmsTasksRecords.setProductId(wmsTasks.getProductId());//商品 id
        wmsTasksRecords.setTaskNumber(wmsTasks.getTaskNumber());//任务编码
        wmsTasksRecords.setTaskType(wmsTasks.getTaskType()); //任务类型
        wmsTasksRecords.setOperationTime(new Date());//执行时间
        wmsTasksRecords.setOperator(wmsTasks.getOperator());//执行人
        //添加执行任务记录
        boolean save = wmsTasksRecordsService.save(wmsTasksRecords);
        if (!save) {
            throw new JeecgBootException("添加任务执行记录失败!");
        }
        //更新任务表中的完成数量,新的完成数量为原有完成数量加收货记录的完成数量
        LambdaUpdateWrapper<WmsTasks> wmsTasksLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        wmsTasksLambdaUpdateWrapper.setSql("completed_quantity = completed_quantity + " + execQuantity)
                .eq(WmsTasks::getId, taskId)
                .le(WmsTasks::getCompletedQuantity, wmsTasks.getQuantity() - wmsTasksRecords.getExecQuantity());
        int update = getBaseMapper().update(null, wmsTasksLambdaUpdateWrapper);
        if (update <= 0) {
            throw new JeecgBootException("执行数量不能大于计划数量!");
        }
        //如果完成数量等于计划数量,更新任务状态为已完成
        wmsTasks = getById(taskId);
        //完成数量
        completedQuantity = wmsTasks.getCompletedQuantity();
        if (completedQuantity == wmsTasks.getQuantity()) {
            wmsTasks.setTaskStatus(WarehouseDictEnum.TASK_STATUS_COMPLETED.getCode());
            boolean b = updateById(wmsTasks);
            if (!b) {
                throw new JeecgBootException("更新任务状态失败!");
            }
        }
        return wmsTasks;
    }

    /**
     * 生成任务编号
     * 规则: TSK+年月日+5位序号，序号使用redis自增序号实现
     */
    private String generateTaskCode() {
        String time = DateUtils.now().substring(0, 10).replace("-", "");
        String key = "tsk_number" + time;
        long incr = redisUtil.incr(key, 1);
        if (incr == 1) {
            //设置过期时间，设置24小时+10秒的目的是避免并发产生订单号重复
            redisUtil.expire(key, 24 * 60 * 60 + 60);
        }
        String incrStr = String.format("%05d", incr);
        return "TSK" + time + incrStr;
    }
}
