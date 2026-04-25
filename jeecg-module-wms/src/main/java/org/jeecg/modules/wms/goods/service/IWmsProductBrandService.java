package org.jeecg.modules.wms.goods.service;

import org.jeecg.modules.wms.goods.entity.WmsProductBrand;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 商品品牌
 * @Author: jeecg-boot
 * @Date:   2026-04-24
 * @Version: V1.0
 */
public interface IWmsProductBrandService extends IService<WmsProductBrand> {

    /**
     * 编辑
     * @param wmsProductBrand
     */
    void edit(WmsProductBrand wmsProductBrand);
}
