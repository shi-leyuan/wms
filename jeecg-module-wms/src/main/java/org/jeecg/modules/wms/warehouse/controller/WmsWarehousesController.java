package org.jeecg.modules.wms.warehouse.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.query.QueryRuleEnum;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.warehouse.entity.WmsWarehouses;
import org.jeecg.modules.wms.warehouse.service.IWmsWarehousesService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;

/**
 * @Description: 仓库表
 * @Author: jeecg-boot
 * @Date: 2026-04-23
 * @Version: V1.0
 */
@Tag(name = "仓库表")
@RestController
@RequestMapping("/warehouse/wmsWarehouses")
@Slf4j
public class WmsWarehousesController extends JeecgController<WmsWarehouses, IWmsWarehousesService> {
    @Autowired
    private IWmsWarehousesService wmsWarehousesService;

    /**
     * 分页列表查询
     *
     * @param wmsWarehouses
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    //@AutoLog(value = "仓库表-分页列表查询")
    @Operation(summary = "仓库表-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsWarehouses>> queryPageList(WmsWarehouses wmsWarehouses,
                                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                      HttpServletRequest req) {
        QueryWrapper<WmsWarehouses> queryWrapper = QueryGenerator.initQueryWrapper(wmsWarehouses, req.getParameterMap());
        Page<WmsWarehouses> page = new Page<WmsWarehouses>(pageNo, pageSize);
        IPage<WmsWarehouses> pageList = wmsWarehousesService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param wmsWarehouses
     * @return
     */
    @AutoLog(value = "仓库表-添加")
    @Operation(summary = "仓库表-添加")
    @RequiresPermissions("warehouse:wms_warehouses:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsWarehouses wmsWarehouses) {
//		wmsWarehousesService.save(wmsWarehouses);
        wmsWarehousesService.add(wmsWarehouses);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param wmsWarehouses
     * @return
     */
    @AutoLog(value = "仓库表-编辑")
    @Operation(summary = "仓库表-编辑")
    @RequiresPermissions("warehouse:wms_warehouses:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsWarehouses wmsWarehouses) {
//		wmsWarehousesService.updateById(wmsWarehouses);
        wmsWarehousesService.edit(wmsWarehouses);
        return Result.OK("编辑成功!");
    }


    /**
     * 仓库启用
     *
     * @param id
     * @return
     */
    @AutoLog(value = "仓库表-启用")
    @Operation(summary = "仓库表-启用")
    @RequestMapping(value = "/enable", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> enable(@RequestParam(name = "id", required = true) String id) {
        wmsWarehousesService.enable(id);
        return Result.OK("启用成功!");
    }

    /**
     * 仓库禁用
     *
     * @param id
     * @return
     */
    @AutoLog(value = "仓库表-禁用")
    @Operation(summary = "仓库表-禁用")
    @RequestMapping(value = "/disable", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> disable(@RequestParam(name = "id", required = true) String id) {
        wmsWarehousesService.disable(id);
        return Result.OK("禁用成功!");
    }


    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "仓库表-通过id删除")
    @Operation(summary = "仓库表-通过id删除")
    @RequiresPermissions("warehouse:wms_warehouses:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        wmsWarehousesService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "仓库表-批量删除")
    @Operation(summary = "仓库表-批量删除")
    @RequiresPermissions("warehouse:wms_warehouses:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.wmsWarehousesService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "仓库表-通过id查询")
    @Operation(summary = "仓库表-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsWarehouses> queryById(@RequestParam(name = "id", required = true) String id) {
        WmsWarehouses wmsWarehouses = wmsWarehousesService.getById(id);
        if (wmsWarehouses == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsWarehouses);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param wmsWarehouses
     */
    @RequiresPermissions("warehouse:wms_warehouses:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsWarehouses wmsWarehouses) {
        return super.exportXls(request, wmsWarehouses, WmsWarehouses.class, "仓库表");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequiresPermissions("warehouse:wms_warehouses:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsWarehouses.class);
    }

    /**
     * 查询启用状态的仓库列表
     *
     * @return
     */
    @GetMapping(value = "/activeList")
    public Result<List<WmsWarehouses>> activeList() {
        List<WmsWarehouses> list = wmsWarehousesService.list(
                new LambdaQueryWrapper<WmsWarehouses>()
                        .eq(WmsWarehouses::getStatus, WarehouseDictEnum.STATUS_ACTIVE.getCode())
                        .orderByAsc(WmsWarehouses::getWarehouseCode)
        );
        return Result.OK(list);
    }
}
