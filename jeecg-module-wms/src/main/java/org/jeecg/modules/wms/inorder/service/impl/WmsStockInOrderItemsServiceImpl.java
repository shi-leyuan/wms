package org.jeecg.modules.wms.inorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrderItems;
import org.jeecg.modules.wms.inorder.mapper.WmsStockInOrderItemsMapper;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrderItemsService;
import org.jeecg.modules.wms.wmstask.entity.WmsTasks;
import org.jeecg.modules.wms.wmstask.entity.WmsTasksRecords;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksRecordsService;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.ObjectUtils;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description: 入库单明细
 * @Author: jeecg-boot
 * @Date: 2026-04-27
 * @Version: V1.0
 */
@Service
public class WmsStockInOrderItemsServiceImpl extends ServiceImpl<WmsStockInOrderItemsMapper, WmsStockInOrderItems> implements IWmsStockInOrderItemsService {

    @Autowired
    private WmsStockInOrderItemsMapper wmsStockInOrderItemsMapper;
    @Autowired
    private IWmsTasksRecordsService wmsTasksRecordsService;

    @Override
    public List<WmsStockInOrderItems> selectByMainId(String mainId) {
        return wmsStockInOrderItemsMapper.selectByMainId(mainId);
    }

    /**
     * 更新收货完成状态
     *
     * @param stockInOrderItemId 入库单明细id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivedStatus(String stockInOrderItemId) {
        if (stockInOrderItemId == null || stockInOrderItemId.trim().isEmpty()) {
            throw new JeecgBootException("入库单明细ID不能为空");
        }

        // 查询入库单明细
        WmsStockInOrderItems stockInOrderItemUpdate = getById(stockInOrderItemId);
        if (stockInOrderItemUpdate == null) {
            throw new JeecgBootException("入库单明细不存在");
        }

        // 根据入库单明细id查询任务记录表
        LambdaQueryWrapper<WmsTasksRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmsTasksRecords::getStockInOrderItemId, stockInOrderItemId);

        List<WmsTasksRecords> wmsTasksRecordsList = wmsTasksRecordsService.list(queryWrapper);

        // 不良品数量
        int badQuantity = wmsTasksRecordsList.stream()
                .filter(record -> WarehouseDictEnum.INVENTORY_ATTRIBUTE_DEFECTIVE.getCode()
                        .equals(record.getInventoryAttribute()))
                .mapToInt(record -> ObjectUtils.defaultIfNull(record.getExecQuantity(), 0))
                .sum();

        // 良品数量
        int goodQuantity = wmsTasksRecordsList.stream()
                .filter(record -> WarehouseDictEnum.INVENTORY_ATTRIBUTE_GOOD.getCode()
                        .equals(record.getInventoryAttribute()))
                .mapToInt(record -> ObjectUtils.defaultIfNull(record.getExecQuantity(), 0))
                .sum();

        stockInOrderItemUpdate.setDefectiveQuantity(badQuantity);
        stockInOrderItemUpdate.setReceivedQuantity(goodQuantity);

        Integer expectedQuantity = ObjectUtils.defaultIfNull(stockInOrderItemUpdate.getExpectedQuantity(), 0);
        Integer receivedQuantity = ObjectUtils.defaultIfNull(stockInOrderItemUpdate.getReceivedQuantity(), 0);
        Integer defectiveQuantity = ObjectUtils.defaultIfNull(stockInOrderItemUpdate.getDefectiveQuantity(), 0);

        // 如果良品数量 + 不良品数量 等于采购数量，则更新状态为收货完成
        if (expectedQuantity.equals(receivedQuantity + defectiveQuantity)) {
            stockInOrderItemUpdate.setStatus(WarehouseDictEnum.INBOUND_DETAIL_RECEIVED.getCode());
        }

        boolean update = updateById(stockInOrderItemUpdate);
        if (!update) {
            throw new JeecgBootException("更新入库单明细收货状态失败");
        }
    }
}
