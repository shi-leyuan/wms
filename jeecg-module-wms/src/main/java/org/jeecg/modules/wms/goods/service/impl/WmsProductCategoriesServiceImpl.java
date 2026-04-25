package org.jeecg.modules.wms.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.wms.goods.entity.WmsProductCategories;
import org.jeecg.modules.wms.goods.mapper.WmsProductCategoriesMapper;
import org.jeecg.modules.wms.goods.service.IWmsProductCategoriesService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import javax.swing.plaf.basic.BasicSeparatorUI;


import org.jeecg.modules.wms.config.WarehouseDictEnum;

/**
 * @Description: 商品类别
 * @Author: jeecg-boot
 * @Date: 2026-04-24
 * @Version: V1.0
 */
@Service
public class WmsProductCategoriesServiceImpl extends ServiceImpl<WmsProductCategoriesMapper, WmsProductCategories> implements IWmsProductCategoriesService {

//	@Override
//	public void addWmsProductCategories(WmsProductCategories wmsProductCategories) {
//	   //新增时设置hasChild为0
//	    wmsProductCategories.setHasChild(IWmsProductCategoriesService.NOCHILD);
//		if(oConvertUtils.isEmpty(wmsProductCategories.getParentId())){
//			wmsProductCategories.setParentId(IWmsProductCategoriesService.ROOT_PID_VALUE);
//		}else{
//			//如果当前节点父ID不为空 则设置父节点的hasChildren 为1
//			WmsProductCategories parent = baseMapper.selectById(wmsProductCategories.getParentId());
//			if(parent!=null && !"1".equals(parent.getHasChild())){
//				parent.setHasChild("1");
//				baseMapper.updateById(parent);
//			}
//		}
//		baseMapper.insert(wmsProductCategories);
//	}


    /**
     * 新增商品类别
     *
     * @param wmsProductCategories
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWmsProductCategories(WmsProductCategories wmsProductCategories) {
        if (wmsProductCategories == null) {
            throw new JeecgBootException("商品类别信息不能为空");
        }
        if (oConvertUtils.isEmpty(wmsProductCategories.getCategoryName())) {
            throw new JeecgBootException("类别名称不能为空");
        }
        String parentId = wmsProductCategories.getParentId();
        if (oConvertUtils.isEmpty(parentId)) {
            parentId = IWmsProductCategoriesService.ROOT_PID_VALUE;
            wmsProductCategories.setParentId(parentId);
        }
        // 校验同级类别名称不能重复
        Long sameNameCount = baseMapper.selectCount(
                new QueryWrapper<WmsProductCategories>().eq("parent_id", parentId).eq("category_name", wmsProductCategories.getCategoryName())
        );
        if (sameNameCount != null && sameNameCount > 0) {
            throw new JeecgBootException("同级分类下已存在相同类别名称");
        }
        // 生成类别编码
        String categoryCode = generateCategoryCode(parentId);
        wmsProductCategories.setCategoryCode(categoryCode);
        // 新增节点默认没有子节点
        wmsProductCategories.setHasChild(IWmsProductCategoriesService.NOCHILD);
        // 如果状态为空，默认创建
        if (oConvertUtils.isEmpty(wmsProductCategories.getStatus())) {
            wmsProductCategories.setStatus("CREATED");
        }
        // 如果不是根节点，更新父节点 hasChild
        if (!IWmsProductCategoriesService.ROOT_PID_VALUE.equals(parentId)) {
            WmsProductCategories parent = baseMapper.selectById(parentId);
            if (parent == null) {
                throw new JeecgBootException("父级类别不存在");
            }
            if (!IWmsProductCategoriesService.HASCHILD.equals(parent.getHasChild())) {
                parent.setHasChild(IWmsProductCategoriesService.HASCHILD);
                baseMapper.updateById(parent);
            }
        }
        baseMapper.insert(wmsProductCategories);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWmsProductCategories(WmsProductCategories wmsProductCategories) {
        if (wmsProductCategories == null || oConvertUtils.isEmpty(wmsProductCategories.getId())) {
            throw new JeecgBootException("商品类别ID不能为空");
        }

        WmsProductCategories entity = this.getById(wmsProductCategories.getId());
        if (entity == null) {
            throw new JeecgBootException("未找到对应商品类别");
        }

        if (oConvertUtils.isEmpty(wmsProductCategories.getCategoryName())) {
            throw new JeecgBootException("类别名称不能为空");
        }

        // 不允许通过编辑修改父节点
        wmsProductCategories.setParentId(entity.getParentId());

        // 不允许通过编辑修改类别编码
        wmsProductCategories.setCategoryCode(entity.getCategoryCode());

        // 不允许通过编辑修改是否有子节点
        wmsProductCategories.setHasChild(entity.getHasChild());

        // 校验同级类别名称不能重复
        Long sameNameCount = baseMapper.selectCount(
                new QueryWrapper<WmsProductCategories>()
                        .eq("parent_id", entity.getParentId())
                        .eq("category_name", wmsProductCategories.getCategoryName())
                        .ne("id", wmsProductCategories.getId())
        );

        if (sameNameCount != null && sameNameCount > 0) {
            throw new JeecgBootException("同级分类下已存在相同类别名称");
        }

        baseMapper.updateById(wmsProductCategories);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWmsProductCategories(String id) throws JeecgBootException {
        //查询选中节点下所有子节点一并删除
        id = this.queryTreeChildIds(id);
        if (id.indexOf(",") > 0) {
            StringBuffer sb = new StringBuffer();
            String[] idArr = id.split(",");
            for (String idVal : idArr) {
                if (idVal != null) {
                    WmsProductCategories wmsProductCategories = this.getById(idVal);
                    String pidVal = wmsProductCategories.getParentId();
                    //查询此节点上一级是否还有其他子节点
                    List<WmsProductCategories> dataList = baseMapper.selectList(new QueryWrapper<WmsProductCategories>().eq("parent_id", pidVal).notIn("id", Arrays.asList(idArr)));
                    boolean flag = (dataList == null || dataList.size() == 0) && !Arrays.asList(idArr).contains(pidVal) && !sb.toString().contains(pidVal);
                    if (flag) {
                        //如果当前节点原本有子节点 现在木有了，更新状态
                        sb.append(pidVal).append(",");
                    }
                }
            }
            //批量删除节点
            baseMapper.deleteBatchIds(Arrays.asList(idArr));
            //修改已无子节点的标识
            String[] pidArr = sb.toString().split(",");
            for (String pid : pidArr) {
                this.updateOldParentNode(pid);
            }
        } else {
            WmsProductCategories wmsProductCategories = this.getById(id);
            if (wmsProductCategories == null) {
                throw new JeecgBootException("未找到对应实体");
            }
            updateOldParentNode(wmsProductCategories.getParentId());
            baseMapper.deleteById(id);
        }
    }

    @Override
    public List<WmsProductCategories> queryTreeListNoPage(QueryWrapper<WmsProductCategories> queryWrapper) {
        List<WmsProductCategories> dataList = baseMapper.selectList(queryWrapper);
        List<WmsProductCategories> mapList = new ArrayList<>();
        for (WmsProductCategories data : dataList) {
            String pidVal = data.getParentId();
            //递归查询子节点的根节点
            if (pidVal != null && !IWmsProductCategoriesService.NOCHILD.equals(pidVal)) {
                WmsProductCategories rootVal = this.getTreeRoot(pidVal);
                if (rootVal != null && !mapList.contains(rootVal)) {
                    mapList.add(rootVal);
                }
            } else {
                if (!mapList.contains(data)) {
                    mapList.add(data);
                }
            }
        }
        fillParentName(mapList);
        return mapList;
    }

    @Override
    public List<SelectTreeModel> queryListByCode(String parentCode) {
        String pid = ROOT_PID_VALUE;
        if (oConvertUtils.isNotEmpty(parentCode)) {
            LambdaQueryWrapper<WmsProductCategories> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WmsProductCategories::getParentId, parentCode);
            List<WmsProductCategories> list = baseMapper.selectList(queryWrapper);
            if (list == null || list.size() == 0) {
                throw new JeecgBootException("该编码【" + parentCode + "】不存在，请核实!");
            }
            if (list.size() > 1) {
                throw new JeecgBootException("该编码【" + parentCode + "】存在多个，请核实!");
            }
            pid = list.get(0).getId();
        }
        return baseMapper.queryListByPid(pid, null);
    }

    @Override
    public List<SelectTreeModel> queryListByPid(String pid) {
        if (oConvertUtils.isEmpty(pid)) {
            pid = ROOT_PID_VALUE;
        }
        return baseMapper.queryListByPid(pid, null);
    }

    /**
     * 根据所传pid查询旧的父级节点的子节点并修改相应状态值
     *
     * @param pid
     */
    private void updateOldParentNode(String pid) {
        if (!IWmsProductCategoriesService.ROOT_PID_VALUE.equals(pid)) {
            Long count = baseMapper.selectCount(new QueryWrapper<WmsProductCategories>().eq("parent_id", pid));
            if (count == null || count <= 1) {
                baseMapper.updateTreeNodeStatus(pid, IWmsProductCategoriesService.NOCHILD);
            }
        }
    }

    /**
     * 递归查询节点的根节点
     *
     * @param pidVal
     * @return
     */
    private WmsProductCategories getTreeRoot(String pidVal) {
        WmsProductCategories data = baseMapper.selectById(pidVal);
        if (data != null && !IWmsProductCategoriesService.ROOT_PID_VALUE.equals(data.getParentId())) {
            return this.getTreeRoot(data.getParentId());
        } else {
            return data;
        }
    }

    /**
     * 根据id查询所有子节点id
     *
     * @param ids
     * @return
     */
    private String queryTreeChildIds(String ids) {
        //获取id数组
        String[] idArr = ids.split(",");
        StringBuffer sb = new StringBuffer();
        for (String pidVal : idArr) {
            if (pidVal != null) {
                if (!sb.toString().contains(pidVal)) {
                    if (sb.toString().length() > 0) {
                        sb.append(",");
                    }
                    sb.append(pidVal);
                    this.getTreeChildIds(pidVal, sb);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 递归查询所有子节点
     *
     * @param pidVal
     * @param sb
     * @return
     */
    private StringBuffer getTreeChildIds(String pidVal, StringBuffer sb) {
        List<WmsProductCategories> dataList = baseMapper.selectList(new QueryWrapper<WmsProductCategories>().eq("parent_id", pidVal));
        if (dataList != null && dataList.size() > 0) {
            for (WmsProductCategories tree : dataList) {
                if (!sb.toString().contains(tree.getId())) {
                    sb.append(",").append(tree.getId());
                }
                this.getTreeChildIds(tree.getId(), sb);
            }
        }
        return sb;
    }

    /**
     * 生成商品类别编码
     * <p>
     * 编码规则：
     * 一级类别：01、02、03
     * 二级类别：0101、0102
     * 三级类别：010101、010102
     *
     * @param parentId 父节点ID
     * @return 新类别编码
     */
    private String generateCategoryCode(String parentId) {
        String prefix = "";
        // 根节点
        if (!IWmsProductCategoriesService.ROOT_PID_VALUE.equals(parentId)) {
            WmsProductCategories parent = baseMapper.selectById(parentId);
            if (parent == null) {
                throw new JeecgBootException("父级类别不存在");
            }
            if (oConvertUtils.isEmpty(parent.getCategoryCode())) {
                throw new JeecgBootException("父级类别编码为空，无法生成子级编码");
            }
            prefix = parent.getCategoryCode();
        }
        List<WmsProductCategories> children = baseMapper.selectList(
                new QueryWrapper<WmsProductCategories>().eq("parent_id", parentId)
        );
        int maxNo = 0;
        int expectedLength = prefix.length() + 2;
        if (children != null && !children.isEmpty()) {
            for (WmsProductCategories child : children) {
                String code = child.getCategoryCode();

                if (oConvertUtils.isEmpty(code)) {
                    continue;
                }

                // 只处理当前层级的编码
                if (code.length() != expectedLength) {
                    continue;
                }

                if (!code.startsWith(prefix)) {
                    continue;
                }

                String suffix = code.substring(prefix.length());

                try {
                    int no = Integer.parseInt(suffix);
                    if (no > maxNo) {
                        maxNo = no;
                    }
                } catch (NumberFormatException ignored) {
                    // 如果历史脏数据不是数字编码，直接跳过
                }
            }
        }
        int nextNo = maxNo + 1;

        if (nextNo > 99) {
            throw new JeecgBootException("当前层级类别数量已超过99个，无法继续生成两位编码");
        }

        return prefix + String.format("%02d", nextNo);
    }

    /**
     * 回填父节点名称
     *
     * @param records 商品类别列表
     */
    public void fillParentName(List<WmsProductCategories> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<String> parentIds = records.stream()
                .map(WmsProductCategories::getParentId)
                .filter(parentId -> parentId != null && !"".equals(parentId.trim()))
                .filter(parentId -> !IWmsProductCategoriesService.ROOT_PID_VALUE.equals(parentId))
                .distinct()
                .collect(Collectors.toList());

        if (parentIds.isEmpty()) {
            records.forEach(item -> item.setParentName("根节点"));
            return;
        }

        List<WmsProductCategories> parentList = this.listByIds(parentIds);

        Map<String, String> parentNameMap = parentList.stream()
                .collect(Collectors.toMap(
                        WmsProductCategories::getId,
                        WmsProductCategories::getCategoryName,
                        (oldValue, newValue) -> oldValue
                ));

        records.forEach(item -> {
            String parentId = item.getParentId();

            if (parentId == null || "".equals(parentId.trim())
                    || IWmsProductCategoriesService.ROOT_PID_VALUE.equals(parentId)) {
                item.setParentName("根节点");
            } else {
                item.setParentName(parentNameMap.getOrDefault(parentId, ""));
            }
        });
    }

    /**
     * 启用
     *
     * @param id
     */
    @Transactional(rollbackFor = Exception.class)
    public void enable(String id) {
        if (oConvertUtils.isEmpty(id)) {
            throw new JeecgBootException("商品类别ID不能为空");
        }
        WmsProductCategories category = this.getById(id);
        if (category == null) {
            throw new JeecgBootException("商品类别不存在");
        }
        String status = category.getStatus();
        if (!WarehouseDictEnum.STATUS_CREATED.getCode().equals(status) && !WarehouseDictEnum.STATUS_INACTIVE.getCode().equals(status)) {
            throw new JeecgBootException("只有‘创建’或‘禁用’状态的商品类别才可以启用");
        }
        String parentId = category.getParentId();
        // 子级类别启用前，父级必须是 ACTIVE
        // 只有非根节点才需要校验父级状态
        if (oConvertUtils.isNotEmpty(parentId) && !IWmsProductCategoriesService.ROOT_PID_VALUE.equals(parentId)) {
            WmsProductCategories parent = this.getById(parentId);
            if (parent == null) {
                throw new JeecgBootException("父级商品类别不存在，无法启用");
            }
            if (!WarehouseDictEnum.STATUS_ACTIVE.getCode().equals(parent.getStatus())) {
                throw new JeecgBootException("父级商品类别未启用，不能启用当前子级类别");
            }
        }
        category.setStatus("ACTIVE");
        this.updateById(category);
    }

    /**
     * 禁用
     * 需要做级联
     * 启用子级类别前，父级必须是 ACTIVE
     * 禁用父级时，自动级联禁用所有子级
     *
     * @param id
     */
    @Transactional(rollbackFor = Exception.class)
    public void disable(String id) {
        if (oConvertUtils.isEmpty(id)) {
            throw new JeecgBootException("商品类别ID不能为空");
        }
        WmsProductCategories category = this.getById(id);
        if (category == null) {
            throw new JeecgBootException("商品类别不存在");
        }
        String status = category.getStatus();
        if (!status.equals(WarehouseDictEnum.STATUS_ACTIVE.getCode())) {
            throw new JeecgBootException("只有‘启用’状态的商品类别才可以禁用");
        }
        // 查询当前节点以及所有子节点 id
        String ids = queryTreeChildIds(id);
        List<String> idList = Arrays.asList(ids.split(","));
        List<WmsProductCategories> categoryList = this.listByIds(idList);
        if (categoryList == null || categoryList.isEmpty()) {
            throw new JeecgBootException("商品类别不存在");
        }
        for (WmsProductCategories item : categoryList) {
            item.setStatus("INACTIVE");
        }
        this.updateBatchById(categoryList);
    }

}
