package org.jeecg.modules.wms.warehouse.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.warehouse.entity.WmsStorageLocations;
import org.jeecg.modules.wms.warehouse.entity.WmsStorageZones;
import org.jeecg.modules.wms.warehouse.entity.WmsWarehouses;
import org.jeecg.modules.wms.warehouse.mapper.WmsStorageLocationsMapper;
import org.jeecg.modules.wms.warehouse.service.IWmsStorageLocationsService;
import org.jeecg.modules.wms.warehouse.service.IWmsStorageZonesService;
import org.jeecg.modules.wms.warehouse.service.IWmsWarehousesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 储位表
 * @Author: jeecg-boot
 * @Date: 2026-04-23
 * @Version: V1.0
 */
@Service
public class WmsStorageLocationsServiceImpl extends ServiceImpl<WmsStorageLocationsMapper, WmsStorageLocations> implements IWmsStorageLocationsService {


    @Autowired
    private IWmsWarehousesService iWmsWarehousesService;
    @Autowired
    private IWmsStorageZonesService iWmsStorageZonesService;

    /**
     * 启用
     *
     * @param wmsStorageLocations
     * @return
     */
    public void enable(WmsStorageLocations wmsStorageLocations) {
        if (wmsStorageLocations == null || wmsStorageLocations.getId() == null || "".equals(wmsStorageLocations.getId().trim())) {
            throw new JeecgBootException("储位ID不能为空");
        }
        //根据ID查询储位
        WmsStorageLocations dbLocation = this.getById(wmsStorageLocations.getId());
        if (dbLocation == null) {
            throw new JeecgBootException("储位不存在");
        }
        // 只有“创建”或“禁用”才可以启用
        String status = dbLocation.getStatus();
        if (!status.equals(WarehouseDictEnum.STATUS_CREATED.getCode()) && !status.equals(WarehouseDictEnum.STATUS_INACTIVE.getCode())) {
            throw new JeecgBootException("只有“创建”或“禁用”才可以启用");
        }
        // 校验所属仓库
        String warehouseId = dbLocation.getWarehouseId();
        if (warehouseId == null || warehouseId.trim().equals("")) {
            throw new JeecgBootException("所属仓库不能为空");
        }
        WmsWarehouses wmsWarehouse = iWmsWarehousesService.getById(warehouseId);
        if (wmsWarehouse == null) {
            throw new JeecgBootException("仓库不存在");
        }
        // 校验所属储区
        String zoneId = dbLocation.getZoneId();
        if (zoneId == null || zoneId.trim().equals("")) {
            throw new JeecgBootException("所属储区不能为空");
        }
        WmsStorageZones wmsStorageZone = iWmsStorageZonesService.getById(zoneId);
        if (wmsStorageZone == null) {
            throw new JeecgBootException("储区不存在");
        }
        // 更新状态
        dbLocation.setStatus(WarehouseDictEnum.STATUS_ACTIVE.getCode());
        this.updateById(dbLocation);
    }

    /**
     * 禁用
     *
     * @param wmsStorageLocations
     * @return
     */
    public void disable(WmsStorageLocations wmsStorageLocations) {
        if (wmsStorageLocations == null || wmsStorageLocations.getId() == null || "".equals(wmsStorageLocations.getId().trim())) {
            throw new JeecgBootException("储位ID不能为空");
        }

        // 根据ID查储位
        WmsStorageLocations dbLocation = this.getById(wmsStorageLocations.getId());
        if (dbLocation == null) {
            throw new JeecgBootException("储位不存在");
        }
        // 只有“启用”状态才可以禁用
        String status = dbLocation.getStatus();
        if (!WarehouseDictEnum.STATUS_ACTIVE.getCode().equals(status)) {
            throw new JeecgBootException("储位状态为‘启用’时才可以禁用");
        }
        // 更新状态
        dbLocation.setStatus(WarehouseDictEnum.STATUS_INACTIVE.getCode());
        this.updateById(dbLocation);
    }
}
