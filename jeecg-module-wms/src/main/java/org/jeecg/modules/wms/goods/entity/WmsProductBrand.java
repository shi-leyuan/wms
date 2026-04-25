package org.jeecg.modules.wms.goods.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;
import org.jeecg.common.constant.ProvinceCityArea;
import org.jeecg.common.util.SpringContextUtils;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.baomidou.mybatisplus.annotation.TableField;

/**
 * @Description: 商品品牌
 * @Author: jeecg-boot
 * @Date: 2026-04-24
 * @Version: V1.0
 */
@Data
@TableName("wms_product_brand")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "商品品牌")
public class WmsProductBrand implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;
    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createBy;
    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建日期")
    private Date createTime;
    /**
     * 更新人
     */
    @Schema(description = "更新人")
    private String updateBy;
    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新日期")
    private Date updateTime;
    /**
     * 所属部门
     */
    @Schema(description = "所属部门")
    private String sysOrgCode;
    /**
     * 品牌名称
     */
    @Excel(name = "品牌名称", width = 15)
    @Schema(description = "品牌名称")
    private String name;
    /**
     * 品牌logo
     */
    @Excel(name = "品牌logo", width = 15)
    @Schema(description = "品牌logo")
    private String logo;
    /**
     * 品牌logo完整访问地址
     * <p>
     * 非数据库字段，只用于前端展示，例如：
     * https://zyb-wms.oss-cn-beijing.aliyuncs.com/temp/image1_1777088739449.jpg
     */
    @TableField(exist = false)
    @Schema(description = "品牌logo完整访问地址")
    private String logoUrl;
    /**
     * 状态
     */
    @Excel(name = "状态", width = 15)
    @Schema(description = "状态")
    private String status;
}
