package org.jeecg.modules.wms.warehouse.service;

import org.jeecg.modules.wms.warehouse.entity.WmsStorageLocations;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 储位表
 * @Author: jeecg-boot
 * @Date:   2026-04-23
 * @Version: V1.0
 */
public interface IWmsStorageLocationsService extends IService<WmsStorageLocations> {

    /**
     *  启用
     *
     * @param wmsStorageLocations
     * @return
     */
    void enable(WmsStorageLocations wmsStorageLocations);

    /**
     *  禁用
     *
     * @param wmsStorageLocations
     * @return
     */
    void disable(WmsStorageLocations wmsStorageLocations);
}
