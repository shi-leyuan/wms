package org.jeecg.modules.wms.inventory.service;

import org.jeecg.modules.wms.inventory.entity.WmsInventory;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrdersItems;

import java.util.List;

/**
 * @Description: 库存表
 * @Author: jeecg-boot
 * @Date:   2026-05-06
 * @Version: V1.0
 */
public interface IWmsInventoryService extends IService<WmsInventory> {

    /**
     * 根据唯一键获取库存
     * @param productId
     * @param locationCode
     * @param batchNumber
     * @return
     */
    public WmsInventory getInventoryByUniqueKey(String productId, String locationCode, String batchNumber);


    /**
     * 查询可用库存
     * @param warehouseId 仓库id
     * @param  skuItem sku信息
     */
    public List<WmsInventory> selectAvailableBySku(String warehouseId, WmsOutOrdersItems skuItem);
}
