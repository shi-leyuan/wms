package org.jeecg.modules.wms.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.wms.inventory.entity.WmsInventory;
import org.jeecg.modules.wms.inventory.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.inventory.service.IWmsInventoryService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

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
}
