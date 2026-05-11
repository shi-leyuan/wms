package org.jeecg.modules.wms.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.wms.inventory.entity.WmsInventory;
import org.jeecg.modules.wms.inventory.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.inventory.service.IWmsInventoryService;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrdersItems;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
 * @Description: 库存表
 * @Author: jeecg-boot
 * @Date: 2026-05-06
 * @Version: V1.0
 */
@Service
public class WmsInventoryServiceImpl extends ServiceImpl<WmsInventoryMapper, WmsInventory> implements IWmsInventoryService {

    /**
     * 根据唯一键获取库存
     *
     * @param productId
     * @param locationCode
     * @param batchNumber
     * @return
     */
    public WmsInventory getInventoryByUniqueKey(String productId, String locationCode, String batchNumber) {
        WmsInventory wmsInventorySource = getOne(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getProductId, productId)
                .eq(WmsInventory::getLocationCode, locationCode)
                .eq(StringUtils.isNotEmpty(batchNumber), WmsInventory::getBatchNumber, batchNumber)
                .eq(StringUtils.isEmpty(batchNumber), WmsInventory::getBatchNumber, ""));
        return wmsInventorySource;
    }

    /**
     * 查询可用库存
     */
    public List<WmsInventory> selectAvailableBySku(String warehouseId, WmsOutOrdersItems skuItem) {
        /**
         *"SELECT * FROM wms_inventory " +
         *             "WHERE is_sellable = '1' and product_id = #{skuId} " +
         *             "AND available_quantity > 0 " +
         *             "AND batch_number = #{batchNo} " +
         *             "AND warehouse_id = #{warehouseId} " +
         *             "ORDER BY expiry_date ASC, stock_in_time ASC")
         */
        //使用MybatisPlus的查询条件
        LambdaQueryWrapper<WmsInventory> queryWrapper = new LambdaQueryWrapper<>();
        //可售库存
        queryWrapper.eq(WmsInventory::getIsSellable, "1");
        queryWrapper.eq(WmsInventory::getProductId, skuItem.getSkuId());
        //如果有批次号是针对某批次的商品进行出库
        queryWrapper.eq(StringUtils.isNotEmpty(skuItem.getBatchNumber()), WmsInventory::getBatchNumber, skuItem.getBatchNumber());
        queryWrapper.gt(WmsInventory::getAvailableQuantity, 0);
        //仓库id
        queryWrapper.eq(WmsInventory::getWarehouseId, warehouseId);
        //保质期到期日升序，入库时间升序
        queryWrapper.orderByAsc(WmsInventory::getExpiryDate).orderByAsc(WmsInventory::getStockInTime);
        return baseMapper.selectList(queryWrapper);
    }
}
