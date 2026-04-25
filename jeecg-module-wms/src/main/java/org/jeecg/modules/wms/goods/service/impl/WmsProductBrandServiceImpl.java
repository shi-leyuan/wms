package org.jeecg.modules.wms.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.goods.entity.WmsProductBrand;
import org.jeecg.modules.wms.goods.mapper.WmsProductBrandMapper;
import org.jeecg.modules.wms.goods.service.IWmsProductBrandService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import org.jeecg.common.util.oConvertUtils;

import java.net.URI;

/**
 * @Description: 商品品牌
 * @Author: jeecg-boot
 * @Date: 2026-04-24
 * @Version: V1.0
 */
@Service
public class WmsProductBrandServiceImpl extends ServiceImpl<WmsProductBrandMapper, WmsProductBrand> implements IWmsProductBrandService {

    /**
     * 编辑
     *
     * @param wmsProductBrand
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(WmsProductBrand wmsProductBrand) {
//        1. 校验 id 不能为空
//        2. 根据 id 查询原品牌是否存在
//        3. 校验品牌名称不能为空
//        4. 校验品牌名称不能重复
//        5. 处理 logo，只保存相对路径
//        6. 如果前端没传 logo，不要误清空原 logo
//        7. 更新数据库
        if (wmsProductBrand == null) {
            throw new JeecgBootException("商品品牌信息不能为空");
        }
        String id = wmsProductBrand.getId();
        if (oConvertUtils.isEmpty(id)) {
            throw new JeecgBootException("商品品牌ID不能为空");
        }
        WmsProductBrand dbBrand = this.getById(id);
        if (dbBrand == null) {
            throw new JeecgBootException("商品品牌不存在");
        }
        String name = wmsProductBrand.getName();
        if (oConvertUtils.isEmpty(name)) {
            throw new JeecgBootException("品牌名称不能为空");
        }
        Long sameNameCount = this.count(
                new QueryWrapper<WmsProductBrand>()
                        .eq("name", name)
                        .ne("id", id)
        );
        if (sameNameCount != null && sameNameCount > 0) {
            throw new JeecgBootException("品牌名称已存在");
        }
        String logo = wmsProductBrand.getLogo();
        if(oConvertUtils.isNotEmpty(logo)){
            wmsProductBrand.setLogo(normalizeLogoPath(logo));
        }else{
            wmsProductBrand.setLogo(dbBrand.getLogo());
        }
        this.updateById(wmsProductBrand);
    }

    /**
     * 标准化 logo 路径。
     *
     * 目标：
     * 数据库永远只保存相对路径。
     *
     * 示例：
     * https://zyb-wms.oss-cn-beijing.aliyuncs.com/temp/a.jpg -> /temp/a.jpg
     * /temp/a.jpg -> /temp/a.jpg
     * temp/a.jpg -> /temp/a.jpg
     * /sys/common/static/temp/a.jpg -> /temp/a.jpg
     *
     * @param logo logo路径
     * @return 相对路径
     */
    private String normalizeLogoPath(String logo) {
        if (oConvertUtils.isEmpty(logo)) {
            return logo;
        }

        String path = logo.trim();

        // 如果前端传的是完整 URL，只取 path 部分
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                URI uri = new URI(path);
                path = uri.getPath();
            } catch (Exception e) {
                return logo;
            }
        }

        // 兼容 Jeecg 本地静态资源地址
        String staticPrefix = "/sys/common/static/";
        if (path.contains(staticPrefix)) {
            path = path.substring(path.indexOf(staticPrefix) + staticPrefix.length());
        }

        // 统一保证以 / 开头
        return path.startsWith("/") ? path : "/" + path;
    }
}
