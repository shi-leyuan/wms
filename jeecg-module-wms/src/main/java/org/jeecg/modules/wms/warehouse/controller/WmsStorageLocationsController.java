package org.jeecg.modules.wms.warehouse.controller;

import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.query.QueryRuleEnum;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.warehouse.entity.WmsStorageLocations;
import org.jeecg.modules.wms.warehouse.entity.WmsStorageZones;
import org.jeecg.modules.wms.warehouse.entity.WmsWarehouses;
import org.jeecg.modules.wms.warehouse.service.IWmsStorageLocationsService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.modules.wms.warehouse.service.IWmsStorageZonesService;
import org.jeecg.modules.wms.warehouse.service.IWmsWarehousesService;
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
 * @Description: 储位表
 * @Author: jeecg-boot
 * @Date: 2026-04-23
 * @Version: V1.0
 */
@Tag(name = "储位表")
@RestController
@RequestMapping("/warehouse/wmsStorageLocations")
@Slf4j
public class WmsStorageLocationsController extends JeecgController<WmsStorageLocations, IWmsStorageLocationsService> {
    @Autowired
    private IWmsStorageLocationsService wmsStorageLocationsService;
    @Autowired
    private IWmsWarehousesService iWmsWarehousesService;
    @Autowired
    private IWmsStorageZonesService iWmsStorageZonesService;

    /**
     * 分页列表查询
     *
     * @param wmsStorageLocations
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    //@AutoLog(value = "储位表-分页列表查询")
    @Operation(summary = "储位表-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsStorageLocations>> queryPageList(WmsStorageLocations wmsStorageLocations,
                                                            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                            HttpServletRequest req) {
        QueryWrapper<WmsStorageLocations> queryWrapper = QueryGenerator.initQueryWrapper(wmsStorageLocations, req.getParameterMap());
        Page<WmsStorageLocations> page = new Page<>(pageNo, pageSize);
        IPage<WmsStorageLocations> pageList = wmsStorageLocationsService.page(page, queryWrapper);

        if (pageList.getRecords().isEmpty()) {
            return Result.OK(pageList);
        }

        // 提取仓库ID和库区ID
        List<String> warehouseIds = pageList.getRecords().stream()
                .map(WmsStorageLocations::getWarehouseId)
                .filter(id -> id != null && !"".equals(id.trim()))
                .distinct()
                .collect(Collectors.toList());

        List<String> zoneIds = pageList.getRecords().stream()
                .map(WmsStorageLocations::getZoneId)
                .filter(id -> id != null && !"".equals(id.trim()))
                .distinct()
                .collect(Collectors.toList());

        // 查询仓库和库区
        List<WmsWarehouses> warehouses = warehouseIds.isEmpty()
                ? new ArrayList<>()
                : iWmsWarehousesService.listByIds(warehouseIds);

        List<WmsStorageZones> zones = zoneIds.isEmpty()
                ? new ArrayList<>()
                : iWmsStorageZonesService.listByIds(zoneIds);

        // 回填名称
        pageList.getRecords().forEach(item -> {
            String warehouseId = item.getWarehouseId();
            WmsWarehouses warehouse = warehouses.stream()
                    .filter(w -> w.getId().equals(warehouseId))
                    .findFirst()
                    .orElse(null);
            if (warehouse != null) {
                item.setWarehouseName(warehouse.getWarehouseName());
            }

            String zoneId = item.getZoneId();
            WmsStorageZones zone = zones.stream()
                    .filter(z -> z.getId().equals(zoneId))
                    .findFirst()
                    .orElse(null);
            if (zone != null) {
                item.setZoneName(zone.getZoneName());
            }
        });

        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param wmsStorageLocations
     * @return
     */
    @AutoLog(value = "储位表-添加")
    @Operation(summary = "储位表-添加")
    @RequiresPermissions("warehouse:wms_storage_locations:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsStorageLocations wmsStorageLocations) {
        wmsStorageLocationsService.save(wmsStorageLocations);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param wmsStorageLocations
     * @return
     */
    @AutoLog(value = "储位表-编辑")
    @Operation(summary = "储位表-编辑")
    @RequiresPermissions("warehouse:wms_storage_locations:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsStorageLocations wmsStorageLocations) {
        wmsStorageLocationsService.updateById(wmsStorageLocations);
        return Result.OK("编辑成功!");
    }

    /**
     * 启用
     *
     * @param wmsStorageLocations
     * @return
     */
    @AutoLog(value = "储位表-启用")
    @Operation(summary = "储位表-启用")
    @RequestMapping(value = "/enable", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> enable(@RequestBody WmsStorageLocations wmsStorageLocations) {
        wmsStorageLocationsService.enable(wmsStorageLocations);
        return Result.OK("启用成功!");
    }

    /**
     * 禁用
     *
     * @param wmsStorageLocations
     * @return
     */
    @AutoLog(value = "储位表-禁用")
    @Operation(summary = "储位表-禁用")
    @RequestMapping(value = "/disable", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> disable(@RequestBody WmsStorageLocations wmsStorageLocations) {
        wmsStorageLocationsService.disable(wmsStorageLocations);
        return Result.OK("禁用成功!");
    }


    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "储位表-通过id删除")
    @Operation(summary = "储位表-通过id删除")
    @RequiresPermissions("warehouse:wms_storage_locations:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        wmsStorageLocationsService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "储位表-批量删除")
    @Operation(summary = "储位表-批量删除")
    @RequiresPermissions("warehouse:wms_storage_locations:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.wmsStorageLocationsService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "储位表-通过id查询")
    @Operation(summary = "储位表-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsStorageLocations> queryById(@RequestParam(name = "id", required = true) String id) {
        WmsStorageLocations wmsStorageLocations = wmsStorageLocationsService.getById(id);
        if (wmsStorageLocations == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsStorageLocations);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param wmsStorageLocations
     */
    @RequiresPermissions("warehouse:wms_storage_locations:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsStorageLocations wmsStorageLocations) {
        return super.exportXls(request, wmsStorageLocations, WmsStorageLocations.class, "储位表");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequiresPermissions("warehouse:wms_storage_locations:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsStorageLocations.class);
    }

}
