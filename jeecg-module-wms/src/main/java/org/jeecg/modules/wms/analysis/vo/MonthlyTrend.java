package org.jeecg.modules.wms.analysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度趋势 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrend {

    /**
     * 月份：1-12
     */
    private Integer month;

    /**
     * 数量
     */
    private Integer count;
}