package org.jeecg.modules.wms.wmstask.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.jeecg.modules.wms.inventory.entity.WmsInventory;
import org.jeecg.modules.wms.inventory.service.IWmsInventoryService;
import org.jeecg.modules.wms.inventory.service.impl.WmsInventoryTransByReceiving;
import org.jeecg.modules.wms.inventory.vo.WmsInventoryTransParam;
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
    private WmsInventoryTransByReceiving wmsInventoryTransByReceiving;

    @Autowired
    private IWmsInventoryService wmsInventoryService;

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
        //存储库存
        WmsInventoryTransParam inventoryTransParam = new WmsInventoryTransParam();
        inventoryTransParam.setProductId(wmsTasks.getProductId());//商品ID
        inventoryTransParam.setExecQuantity(wmsTasksRecords.getExecQuantity());//执行数量
        inventoryTransParam.setWarehouseId(wmsTasks.getTargetWarehouseId());
        inventoryTransParam.setTargetLocationCode(wmsTasksRecords.getTargetLocationCode());//目的储位编码
        inventoryTransParam.setSourceLocationCode(wmsTasks.getSourceLocationCode());
        inventoryTransParam.setBatchNumber(wmsTasksRecords.getBatchNumber());
        inventoryTransParam.setExpiryDate(wmsTasksRecords.getExpiryDate());
        //库存属性为良品则可售
        inventoryTransParam.setIsSellable(wmsTasksRecords.getInventoryAttribute().equals(WarehouseDictEnum.INVENTORY_ATTRIBUTE_GOOD.getCode()) ? "1" : "0");
        inventoryTransParam.setTransactionType(WarehouseDictEnum.INVENTORY_RECEIVING.getCode());//库存变更 类型
        inventoryTransParam.setOperator(wmsTasksRecords.getOperator());
        inventoryTransParam.setOperationTime(new Date());
        wmsInventoryTransByReceiving.transfer(inventoryTransParam);
        //如果入库单收货完成则创建上架任务,根据收货记录创建上架任务
        WmsStockInOrders stockInOrders = stockInOrdersService.getById(wmsTasks.getStockInOrderId());
        if (stockInOrders == null) {
            throw new JeecgBootException("入库单不存在");
        }
        if (WarehouseDictEnum.INBOUND_RECEIVED.getCode().equals(stockInOrders.getStatus())) {
            createPutawayTask(stockInOrders.getId());
        }
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
     * 根据入库单创建上架任务
     *
     * @param stockInOrderId 入库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void createPutawayTask(String stockInOrderId) {
        /*只给良品收货记录创建上架任务；
        一条良品收货记录创建一条上架任务；
        上架任务的数量 = 该良品收货记录的执行数量；
        任务类型 = 上架任务；
        来源仓库 / 目标仓库 = 收货记录中的目标仓库；
        来源储位 = 收货时放到的暂存储位；
        商品、批次、保质期等从收货记录带过去。*/
        //入库单ID不能为空
        if (stockInOrderId == null || stockInOrderId.trim().isEmpty()) {
            throw new JeecgBootException("入库单ID不能为空");
        }
        //查询入库单
        WmsStockInOrders stockInOrders = stockInOrdersService.getById(stockInOrderId);
        if (stockInOrders == null) {
            throw new JeecgBootException("入库单不存在");
        }
        // 只有收货完成的入库单才能创建上架任务
        if (!WarehouseDictEnum.INBOUND_RECEIVED.getCode().equals(stockInOrders.getStatus())) {
            throw new JeecgBootException("入库单收货完成后才能创建上架任务");
        }
        // 查询该入库单下的良品收货记录
        LambdaQueryWrapper<WmsTasksRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsTasksRecords::getStockInOrderId, stockInOrderId)
                .eq(WmsTasksRecords::getTaskType, WarehouseDictEnum.TASK_TYPE_RECEIVING.getCode())
                .eq(WmsTasksRecords::getInventoryAttribute, WarehouseDictEnum.INVENTORY_ATTRIBUTE_GOOD.getCode());
        List<WmsTasksRecords> receiveRecords = wmsTasksRecordsService.list(queryWrapper);
        if (receiveRecords == null || receiveRecords.isEmpty()) {
            throw new JeecgBootException("没有可创建上架任务的良品收货记录");
        }
        //遍历收获记录
        for (WmsTasksRecords record : receiveRecords) {
            // 防止重复创建：同一条收货记录如果已经创建过上架任务，则跳过
            LambdaQueryWrapper<WmsTasks> existsWrapper = new LambdaQueryWrapper<>();
            existsWrapper.eq(WmsTasks::getTaskType, WarehouseDictEnum.TASK_TYPE_PUTAWAY.getCode())
                    .eq(WmsTasks::getStockInOrderId, record.getStockInOrderId())
                    .eq(WmsTasks::getStockInOrderItemId, record.getStockInOrderItemId())
                    .eq(WmsTasks::getProductId, record.getProductId())
                    .eq(WmsTasks::getBatchNumber, record.getBatchNumber())
                    .eq(WmsTasks::getSourceLocationCode, record.getTargetLocationCode());
            long existsCount = this.count(existsWrapper);
            if (existsCount > 0) {
                continue;
            }
            WmsTasks putawayTask = new WmsTasks();
            // 上架任务
            putawayTask.setTaskType(WarehouseDictEnum.TASK_TYPE_PUTAWAY.getCode());
            putawayTask.setTaskStatus(WarehouseDictEnum.TASK_STATUS_CREATED.getCode());
            putawayTask.setCreateTime(new Date());
            putawayTask.setTaskNumber(generateTaskCode());
            // 入库相关信息
            putawayTask.setStockInOrderId(record.getStockInOrderId());
            putawayTask.setStockInOrderItemId(record.getStockInOrderItemId());
            // 商品信息
            putawayTask.setProductId(record.getProductId());
            // 数量：良品收货数量
            putawayTask.setQuantity(record.getExecQuantity());
            putawayTask.setCompletedQuantity(0);
            // 仓库与储位
            putawayTask.setSourceWarehouseId(record.getTargetWarehouseId());
            putawayTask.setTargetWarehouseId(record.getTargetWarehouseId());
            putawayTask.setSourceLocationCode(record.getTargetLocationCode());
            // 批次、保质期
            putawayTask.setBatchNumber(record.getBatchNumber());
            putawayTask.setExpiryDate(record.getExpiryDate());
            // 操作人：可以先沿用收货执行人，也可以后续由页面分配
            putawayTask.setOperator(record.getOperator());
            boolean save = this.save(putawayTask);
            if (!save) {
                throw new JeecgBootException("创建上架任务失败");
            }
        }
    }

    /**
     * 执行上架任务
     *
     * @param wmsTasksRecords 上架执行记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void putaway(WmsTasksRecords wmsTasksRecords) {
        //任务ID
        String taskId = wmsTasksRecords.getTaskId();
        if(taskId==null||taskId.trim().isEmpty()){
            throw new JeecgBootException("任务ID不能为空");
        }
        //通过ID查询上架任务
        WmsTasks wmsTasks = getById(taskId);
        if(wmsTasks==null){
            throw new JeecgBootException("上架任务不存在");
        }
        if (!WarehouseDictEnum.TASK_TYPE_PUTAWAY.getCode().equals(wmsTasks.getTaskType())) {
            throw new JeecgBootException("只有上架任务可以执行上架");
        }
        Integer completedQuantity = ObjectUtils.defaultIfNull(wmsTasks.getCompletedQuantity(), 0);
        Integer execQuantity = ObjectUtils.defaultIfNull(wmsTasksRecords.getExecQuantity(), 0);
        Integer planQuantity = ObjectUtils.defaultIfNull(wmsTasks.getQuantity(), 0);
        if(execQuantity<=0){
            throw new JeecgBootException("本次上架数量必须大于0");
        }
        if (completedQuantity + execQuantity > planQuantity) {
            throw new JeecgBootException("本次上架数量不能大于待上架数量");
        }
        if (wmsTasksRecords.getTargetLocationCode() == null || wmsTasksRecords.getTargetLocationCode().trim().isEmpty()) {
            throw new JeecgBootException("目标储位不能为空");
        }
        if (wmsTasks.getSourceLocationCode() != null
                && wmsTasks.getSourceLocationCode().equals(wmsTasksRecords.getTargetLocationCode())) {
            throw new JeecgBootException("目标储位不能与来源储位相同");
        }
        // 补充任务执行记录信息
        wmsTasksRecords.setTaskId(wmsTasks.getId());
        wmsTasksRecords.setTaskNumber(wmsTasks.getTaskNumber());
        wmsTasksRecords.setTaskType(wmsTasks.getTaskType());
        wmsTasksRecords.setProductId(wmsTasks.getProductId());
        wmsTasksRecords.setStockInOrderId(wmsTasks.getStockInOrderId());
        wmsTasksRecords.setStockInOrderItemId(wmsTasks.getStockInOrderItemId());
        wmsTasksRecords.setSourceWarehouseId(wmsTasks.getSourceWarehouseId());
        wmsTasksRecords.setTargetWarehouseId(wmsTasks.getTargetWarehouseId());
        // 来源储位：收货暂存位
        wmsTasksRecords.setSourceLocationCode(wmsTasks.getSourceLocationCode());
        // 目标储位：前端输入的上架储位，不覆盖
        // wmsTasksRecords.setTargetLocationCode(...)
        wmsTasksRecords.setBatchNumber(wmsTasks.getBatchNumber());
        wmsTasksRecords.setExpiryDate(wmsTasks.getExpiryDate());
        wmsTasksRecords.setOperationTime(new Date());
        // 如果前端没有传执行人，则使用任务执行人
        if (wmsTasksRecords.getOperator() == null || wmsTasksRecords.getOperator().trim().isEmpty()) {
            wmsTasksRecords.setOperator(wmsTasks.getOperator());
        }
        // 上架只处理良品库存
        wmsTasksRecords.setInventoryAttribute(WarehouseDictEnum.INVENTORY_ATTRIBUTE_GOOD.getCode());
        boolean save = wmsTasksRecordsService.save(wmsTasksRecords);
        if(!save){
            throw new JeecgBootException("保存上架记录失败");
        }
        // 更新任务完成数量
        int newCompletedQuantity = completedQuantity + execQuantity;
        LambdaUpdateWrapper<WmsTasks> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .set(WmsTasks::getCompletedQuantity, newCompletedQuantity)
                .eq(WmsTasks::getId, taskId)
                .eq(WmsTasks::getCompletedQuantity, completedQuantity);
        int update = getBaseMapper().update(null, updateWrapper);
        if (update <= 0) {
            throw new JeecgBootException("任务数量已变化，请刷新后重试");
        }
        // 如果任务完成，更新任务状态
        WmsTasks latestTask = getById(taskId);
        Integer latestCompletedQuantity = ObjectUtils.defaultIfNull(latestTask.getCompletedQuantity(), 0);
        if(latestCompletedQuantity.intValue()==planQuantity.intValue()){
            latestTask.setTaskStatus(WarehouseDictEnum.TASK_STATUS_COMPLETED.getCode());
            boolean updateStatus = updateById(latestTask);
            if (!updateStatus) {
                throw new JeecgBootException("更新上架任务状态失败");
            }
        }
        // 更新入库单明细已上架数量和状态
        updateStockInOrderItemShelvedStatus(wmsTasks.getStockInOrderItemId());
        // 更新入库单主表已上架数量和状态
        updateStockInOrderShelvedStatus(wmsTasks.getStockInOrderId());
        // 上架库存转移：从收货暂存储位扣减，增加到正式上架储位
        transferInventoryByPutawayRecord(wmsTasksRecords);
    }

    /**
     * 根据上架记录进行库存转移
     * 从收货暂存储位扣减库存，增加到正式上架储位
     */
    private void transferInventoryByPutawayRecord(WmsTasksRecords record) {
        if (record == null) {
            throw new JeecgBootException("上架记录不能为空");
        }

        Integer execQuantity = ObjectUtils.defaultIfNull(record.getExecQuantity(), 0);
        if (execQuantity <= 0) {
            throw new JeecgBootException("上架数量必须大于0");
        }

        if (record.getSourceLocationCode() == null || record.getSourceLocationCode().trim().isEmpty()) {
            throw new JeecgBootException("来源储位不能为空");
        }

        if (record.getTargetLocationCode() == null || record.getTargetLocationCode().trim().isEmpty()) {
            throw new JeecgBootException("目标储位不能为空");
        }

        WmsStockInOrders order = stockInOrdersService.getById(record.getStockInOrderId());
        if (order == null) {
            throw new JeecgBootException("入库单不存在");
        }

        String isSellable = WarehouseDictEnum.INVENTORY_ATTRIBUTE_GOOD.getCode()
                .equals(record.getInventoryAttribute()) ? "1" : "0";

        // 1. 扣减来源储位库存
        decreaseInventory(
                record.getProductId(),
                record.getSourceLocationCode(),
                isSellable,
                execQuantity
        );

        // 2. 增加目标储位库存
        increaseInventory(
                record,
                order,
                isSellable,
                execQuantity
        );
    }

    /**
     * 扣减来源储位库存
     */
    private void decreaseInventory(String productId,
                                   String sourceLocationCode,
                                   String isSellable,
                                   Integer execQuantity) {
        LambdaQueryWrapper<WmsInventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInventory::getProductId, productId)
                .eq(WmsInventory::getLocationCode, sourceLocationCode)
                .eq(WmsInventory::getIsSellable, isSellable);

        WmsInventory sourceInventory = wmsInventoryService.getOne(queryWrapper, false);

        if (sourceInventory == null) {
            throw new JeecgBootException("来源储位库存不存在");
        }

        Integer stockQuantity = ObjectUtils.defaultIfNull(sourceInventory.getStockQuantity(), 0);
        Integer allocatedQuantity = ObjectUtils.defaultIfNull(sourceInventory.getAllocatedQuantity(), 0);
        Integer availableQuantity = ObjectUtils.defaultIfNull(sourceInventory.getAvailableQuantity(), 0);

        if (availableQuantity < execQuantity) {
            throw new JeecgBootException("来源储位可用库存不足");
        }

        sourceInventory.setStockQuantity(stockQuantity - execQuantity);
        sourceInventory.setAvailableQuantity(availableQuantity - execQuantity);

        boolean update = wmsInventoryService.updateById(sourceInventory);
        if (!update) {
            throw new JeecgBootException("扣减来源储位库存失败");
        }
    }

    /**
     * 增加目标储位库存
     */
    private void increaseInventory(WmsTasksRecords record,
                                   WmsStockInOrders order,
                                   String isSellable,
                                   Integer execQuantity) {
        LambdaQueryWrapper<WmsInventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsInventory::getProductId, record.getProductId())
                .eq(WmsInventory::getLocationCode, record.getTargetLocationCode())
                .eq(WmsInventory::getIsSellable, isSellable);

        WmsInventory targetInventory = wmsInventoryService.getOne(queryWrapper, false);

        if (targetInventory == null) {
            targetInventory = new WmsInventory();

            targetInventory.setProductId(record.getProductId());
            targetInventory.setLocationCode(record.getTargetLocationCode());
            targetInventory.setStockQuantity(execQuantity);
            targetInventory.setAllocatedQuantity(0);
            targetInventory.setAvailableQuantity(execQuantity);
            targetInventory.setBatchNumber(record.getBatchNumber());
            targetInventory.setExpiryDate(record.getExpiryDate());
            targetInventory.setStockInTime(new Date());
            targetInventory.setOwnerId(order.getOwnerId());
            targetInventory.setIsSellable(isSellable);
            targetInventory.setWarehouseId(record.getTargetWarehouseId());

            boolean save = wmsInventoryService.save(targetInventory);
            if (!save) {
                throw new JeecgBootException("新增目标储位库存失败");
            }
        } else {
            Integer stockQuantity = ObjectUtils.defaultIfNull(targetInventory.getStockQuantity(), 0);
            Integer allocatedQuantity = ObjectUtils.defaultIfNull(targetInventory.getAllocatedQuantity(), 0);

            targetInventory.setStockQuantity(stockQuantity + execQuantity);
            targetInventory.setAvailableQuantity(stockQuantity + execQuantity - allocatedQuantity);

            boolean update = wmsInventoryService.updateById(targetInventory);
            if (!update) {
                throw new JeecgBootException("更新目标储位库存失败");
            }
        }
    }

    /**
     * 更新入库单明细已上架数量和状态
     */
    private void updateStockInOrderItemShelvedStatus(String stockInOrderItemId) {
        if (stockInOrderItemId == null || stockInOrderItemId.trim().isEmpty()) {
            throw new JeecgBootException("入库单明细ID不能为空");
        }

        WmsStockInOrderItems item = stockInOrderItemsService.getById(stockInOrderItemId);
        if (item == null) {
            throw new JeecgBootException("入库单明细不存在");
        }

        LambdaQueryWrapper<WmsTasksRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsTasksRecords::getStockInOrderItemId, stockInOrderItemId)
                .eq(WmsTasksRecords::getTaskType, WarehouseDictEnum.TASK_TYPE_PUTAWAY.getCode());

        List<WmsTasksRecords> putawayRecords = wmsTasksRecordsService.list(queryWrapper);

        int shelvedQuantity = putawayRecords.stream()
                .mapToInt(record -> ObjectUtils.defaultIfNull(record.getExecQuantity(), 0))
                .sum();

        item.setShelvedQuantity(shelvedQuantity);

        Integer receivedQuantity = ObjectUtils.defaultIfNull(item.getReceivedQuantity(), 0);

        if (shelvedQuantity >= receivedQuantity && receivedQuantity > 0) {
            item.setStatus(WarehouseDictEnum.INBOUND_DETAIL_PUTAWAYED.getCode());
        }

        boolean update = stockInOrderItemsService.updateById(item);
        if (!update) {
            throw new JeecgBootException("更新入库单明细上架状态失败");
        }
    }

    /**
     * 更新入库单主表已上架数量和状态
     */
    private void updateStockInOrderShelvedStatus(String stockInOrderId) {
        if (stockInOrderId == null || stockInOrderId.trim().isEmpty()) {
            throw new JeecgBootException("入库单ID不能为空");
        }

        WmsStockInOrders order = stockInOrdersService.getById(stockInOrderId);
        if (order == null) {
            throw new JeecgBootException("入库单不存在");
        }

        List<WmsStockInOrderItems> itemList = stockInOrderItemsService.selectByMainId(stockInOrderId);

        int totalShelvedQuantity = itemList.stream()
                .mapToInt(item -> ObjectUtils.defaultIfNull(item.getShelvedQuantity(), 0))
                .sum();

        order.setTotalShelvedQuantity(totalShelvedQuantity);

        boolean allPutawayed = itemList.stream().allMatch(item ->
                WarehouseDictEnum.INBOUND_DETAIL_PUTAWAYED.getCode().equals(item.getStatus())
        );

        if (allPutawayed && !itemList.isEmpty()) {
            order.setStatus(WarehouseDictEnum.INBOUND_PUTAWAYED.getCode());
        }

        boolean update = stockInOrdersService.updateById(order);
        if (!update) {
            throw new JeecgBootException("更新入库单上架状态失败");
        }
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
