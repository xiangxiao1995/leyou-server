package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;
    @Autowired
    private SpecParamMapper specParamMapper;

    /**
     * 根据cid查询所有的SpecGroup
     * @param cid
     * @return
     */
    public List<SpecGroup> querySpecGroupByCid(Long cid) {
        //根据cid查询所有的SpecGroup
        SpecGroup group = new SpecGroup();
        group.setCid(cid);
        List<SpecGroup> list = specGroupMapper.select(group);
        //判断是否为空
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        //返回结果
        return list;
    }

    /**
     * 查询规格参数集合
     * @param gid  规格参数组id
     * @param cid  分类id
     * @param searching  是否为搜索字段
     * @return
     */
    public List<SpecParam> querySpecParamList(Long gid,Long cid,Boolean searching) {
        //根据gid查询所有的SpecGroup
        SpecParam param = new SpecParam();
        param.setGroupId(gid);
        param.setCid(cid);
        param.setSearching(searching);
        //根据param中的非空字段查询
        List<SpecParam> list = specParamMapper.select(param);
        //判断是否为空
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        //返回结果
        return list;
    }

    /**
     * 更新SpecGroup
     * @param specGroup
     */
    public void updateSpecGroup(SpecGroup specGroup) {
        //更新SpecGroup
        int count = specGroupMapper.updateByPrimaryKey(specGroup);
        if (count != 1) {
            throw new LyException(ExceptionEnum.SPEC_GROUP_UPDATED_ERROR);
        }
    }

    /**
     * 新增SpecGroup
     * @param specGroup
     */
    public void saveSpecGroup(SpecGroup specGroup) {
        int count = specGroupMapper.insert(specGroup);
        if (count != 1) {
            throw new LyException(ExceptionEnum.SPEC_GROUP_CREATED_ERROR);
        }
    }

    /**
     * 根据id删除SpecGroup
     * @param id
     */
    public void deleteSpecGroup(Long id) {
        int count = specGroupMapper.deleteByPrimaryKey(id);
        if (count != 1) {
            throw new LyException(ExceptionEnum.SPEC_GROUP_DELETED_ERROR);
        }
    }

    /**
     * 更新SpecParam
     * @param specParam
     */
    public void updateSpecParam(SpecParam specParam) {
        int count = specParamMapper.updateByPrimaryKey(specParam);
        if (count != 1) {
            throw new LyException(ExceptionEnum.SPEC_PARAM_UPDATED_ERROR);
        }
    }

    /**
     * 新增SpecParam
     * @param specParam
     */
    public void saveSpecParam(SpecParam specParam) {
        int count = specParamMapper.insert(specParam);
        if (count != 1) {
            throw new LyException(ExceptionEnum.SPEC_PARAM_CREATED_ERROR);
        }
    }

    /**
     * 根据id删除SpecParam
     * @param id
     */
    public void deleteSpecParam(Long id) {
        int count = specParamMapper.deleteByPrimaryKey(id);
        if (count != 1) {
            throw new LyException(ExceptionEnum.SPEC_PARAM_DELETED_ERROR);
        }
    }

    /**
     * 根据分类id查询规格参数组以及规格参数
     * @param cid
     * @return
     */
    public List<SpecGroup> querySpecGroupAndParams(Long cid) {
        // 查询规格参数组
        List<SpecGroup> specGroupList = querySpecGroupByCid(cid);

        // 查询规格参数
        List<SpecParam> paramList = querySpecParamList(null, cid, null);

        // 将SpecGroup的id作为key，对应的规格参数集合作为value
        Map<Long, List<SpecParam>> map = new HashMap<>();
        for (SpecParam param : paramList) {
            // 判断map中是否已经有该param对应的groupId
            if (!map.containsKey(param.getGroupId())) {
                // 如果没有，则新创建一个List<SpecParam>，放入到map中
                map.put(param.getGroupId(), new ArrayList<>());
            }
            // 将param放入
            map.get(param.getGroupId()).add(param);
        }

        // 将List<SpecParam>存入对应的specGroup
        for (SpecGroup specGroup : specGroupList) {
            specGroup.setParams(map.get(specGroup.getId()));
        }

        return specGroupList;
    }
}
