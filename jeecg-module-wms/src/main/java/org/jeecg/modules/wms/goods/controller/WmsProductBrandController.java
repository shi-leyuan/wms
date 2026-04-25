package org.jeecg.modules.wms.goods.controller;

import java.net.URI;
import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.goods.entity.WmsProductBrand;
import org.jeecg.modules.wms.goods.service.IWmsProductBrandService;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.ModelAndView;

/**
 * @Description: 商品品牌
 * @Author: jeecg-boot
 * @Date: 2026-04-24
 * @Version: V1.0
 */
@Tag(name = "商品品牌")
@RestController
@RequestMapping("/goods/wmsProductBrand")
@Slf4j
public class WmsProductBrandController extends JeecgController<WmsProductBrand, IWmsProductBrandService> {

    @Autowired
    private IWmsProductBrandService wmsProductBrandService;

    /**
     * 文件访问域名。
     *
     * 数据库 logo 字段只存相对路径，例如：
     * /temp/image1_1777088739449.jpg
     *
     * 接口返回时动态拼接成：
     * https://zyb-wms.oss-cn-beijing.aliyuncs.com/temp/image1_1777088739449.jpg
     */
    @Value("${jeecg.file-view-domain}")
    private String fileDomain;

    /**
     * 分页列表查询
     *
     * @param wmsProductBrand
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @Operation(summary = "商品品牌-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsProductBrand>> queryPageList(WmsProductBrand wmsProductBrand,
                                                        @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                        HttpServletRequest req) {
        QueryWrapper<WmsProductBrand> queryWrapper = QueryGenerator.initQueryWrapper(wmsProductBrand, req.getParameterMap());
        Page<WmsProductBrand> page = new Page<>(pageNo, pageSize);
        IPage<WmsProductBrand> pageList = wmsProductBrandService.page(page, queryWrapper);

        pageList.getRecords().forEach(item -> {
            item.setLogoUrl(buildFileAccessUrl(item.getLogo()));
        });

        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param wmsProductBrand
     * @return
     */
    @AutoLog(value = "商品品牌-添加")
    @Operation(summary = "商品品牌-添加")
    @RequiresPermissions("goods:wms_product_brand:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsProductBrand wmsProductBrand) {
        wmsProductBrand.setLogo(normalizeLogoPath(wmsProductBrand.getLogo()));

        wmsProductBrandService.save(wmsProductBrand);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param wmsProductBrand
     * @return
     */
    @AutoLog(value = "商品品牌-编辑")
    @Operation(summary = "商品品牌-编辑")
    @RequiresPermissions("goods:wms_product_brand:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsProductBrand wmsProductBrand) {
        wmsProductBrand.setLogo(normalizeLogoPath(wmsProductBrand.getLogo()));

//        wmsProductBrandService.updateById(wmsProductBrand);
        wmsProductBrandService.edit(wmsProductBrand);
        return Result.OK("编辑成功!");

    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "商品品牌-通过id删除")
    @Operation(summary = "商品品牌-通过id删除")
    @RequiresPermissions("goods:wms_product_brand:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        wmsProductBrandService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "商品品牌-批量删除")
    @Operation(summary = "商品品牌-批量删除")
    @RequiresPermissions("goods:wms_product_brand:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        wmsProductBrandService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @Operation(summary = "商品品牌-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsProductBrand> queryById(@RequestParam(name = "id", required = true) String id) {
        WmsProductBrand wmsProductBrand = wmsProductBrandService.getById(id);
        if (wmsProductBrand == null) {
            return Result.error("未找到对应数据");
        }

        wmsProductBrand.setLogoUrl(buildFileAccessUrl(wmsProductBrand.getLogo()));

        return Result.OK(wmsProductBrand);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param wmsProductBrand
     */
    @RequiresPermissions("goods:wms_product_brand:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsProductBrand wmsProductBrand) {
        return super.exportXls(request, wmsProductBrand, WmsProductBrand.class, "商品品牌");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequiresPermissions("goods:wms_product_brand:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsProductBrand.class);
    }

    /**
     * 给分页列表中的每条品牌数据填充 logoUrl。
     *
     * logo 是数据库字段，只存相对路径；
     * logoUrl 是接口展示字段，返回给前端用于图片预览。
     *
     * @param pageList
     */
    private void fillLogoUrl(IPage<WmsProductBrand> pageList) {
        if (pageList == null || pageList.getRecords() == null || pageList.getRecords().isEmpty()) {
            return;
        }

        pageList.getRecords().forEach(item -> {
            item.setLogoUrl(buildFileAccessUrl(item.getLogo()));
        });
    }

    /**
     * 保存前标准化 logo。
     *
     * 目标：
     * 数据库 logo 字段永远只存相对路径。
     *
     * 示例：
     * https://zyb-wms.oss-cn-beijing.aliyuncs.com/temp/a.jpg -> /temp/a.jpg
     * /temp/a.jpg -> /temp/a.jpg
     * temp/a.jpg -> /temp/a.jpg
     *
     * @param logo
     * @return
     */
    private String normalizeLogoPath(String logo) {
        if (oConvertUtils.isEmpty(logo)) {
            return logo;
        }

        String path = logo.trim();

        // 如果前端传的是完整 URL，则只取 URL 的 path 部分
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                URI uri = new URI(path);
                path = uri.getPath();
            } catch (Exception e) {
                log.warn("品牌logo地址解析失败，logo={}", logo, e);
                return logo;
            }
        }

        // 兼容 Jeecg 本地静态资源路径：
        // /sys/common/static/temp/a.jpg -> /temp/a.jpg
        String staticPrefix = "/sys/common/static/";
        if (path.contains(staticPrefix)) {
            path = path.substring(path.indexOf(staticPrefix) + staticPrefix.length());
        }

        // 统一保证以 / 开头
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * 返回前构建完整文件访问地址。
     *
     * 目标：
     * 根据数据库 logo 相对路径动态拼接完整访问地址。
     *
     * 示例：
     * /temp/a.jpg -> https://zyb-wms.oss-cn-beijing.aliyuncs.com/temp/a.jpg
     *
     * @param logo
     * @return
     */
    private String buildFileAccessUrl(String logo) {
        if (oConvertUtils.isEmpty(logo)) {
            return "";
        }

        String path = logo.trim();

        // 兼容历史数据：如果数据库里已经是完整地址，则直接返回
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        String domain = fileDomain == null ? "" : fileDomain.trim();

        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        path = path.startsWith("/") ? path : "/" + path;

        return domain + path;
    }
}