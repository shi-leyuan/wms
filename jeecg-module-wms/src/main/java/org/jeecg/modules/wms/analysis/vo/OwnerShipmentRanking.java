package org.jeecg.modules.wms.analysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 货主出库排行 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerShipmentRanking {

    /**
     * 货主 ID
     */
    private String ownerId;

    /**
     * 货主名称
     */
    private String ownerName;

    /**
     * 出库商品总数
     */
    private Integer totalQuantity;
}