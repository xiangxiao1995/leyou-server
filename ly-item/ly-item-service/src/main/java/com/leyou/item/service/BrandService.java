package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    /**
     * 分页查询品牌
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @param key
     * @return
     */
    public PageResult<Brand> queryBrandByPage(Integer page, Integer rows, String sortBy, Boolean desc, String key) {
        //分页
        PageHelper.startPage(page, rows);
        Example example = new Example(Brand.class);
        //过滤
        if(StringUtils.isNotBlank(key)){
            example.createCriteria().orLike("name","%"+ key +"%").orEqualTo("letter",key.toUpperCase());
        }
        //排序
        if(StringUtils.isNotBlank(sortBy)){
            //setOrderByClause(orderByClause)的sql语句是order by id ASC， id ASC需要手动拼接
            String orderByClause = sortBy + (desc ? " DESC":" ASC");
            example.setOrderByClause(orderByClause);
        }
        //查询
        List<Brand> list = brandMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        PageInfo<Brand> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), list);
    }

    /**
     * 新增品牌
     * @param brand
     * @param cids
     */
    public void saveBrand(Brand brand, ArrayList<Long> cids) {
        int count = brandMapper.insert(brand);
        if(count != 1){
            throw new LyException(ExceptionEnum.BRAND_CREATED_ERROR);
        }
        for (Long cid : cids) {
            count = brandMapper.saveBrandCategory(cid, brand.getId());
            if(count != 1){
                throw new LyException(ExceptionEnum.BRAND_CREATED_ERROR);
            }
        }
    }

    /**
     * 更新品牌
     * @param brand
     * @param cids
     */
    public void updateBrand(Brand brand, ArrayList<Long> cids) {
        //更新品牌信息
        int count = brandMapper.updateByPrimaryKey(brand);
        if(count != 1){
            throw new LyException(ExceptionEnum.BRAND_UPDATED_ERROR);
        }
        //删除之前的品牌商品中间表信息
        brandMapper.deleteBrandCategory(brand.getId());
        //插入新的品牌商品中间表信息
        for (Long cid : cids) {
            count = brandMapper.saveBrandCategory(cid, brand.getId());
            if(count != 1){
                throw new LyException(ExceptionEnum.BRAND_CREATED_ERROR);
            }
        }
    }

    /**
     * 删除品牌
     * @param bid
     */
    public void deleteBrandByBid(Long bid) {
        //删除中间表信息
        brandMapper.deleteBrandCategory(bid);
        //删除品牌信息
        int count = brandMapper.deleteByPrimaryKey(bid);
        if(count != 1){
            throw new LyException(ExceptionEnum.BRAND_DELETED_ERROR);
        }
    }

    /**
     * 根据品牌id查询品牌
     * @param id
     * @return
     */
    public Brand queryBrandById(Long id) {
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if (brand == null) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brand;
    }

    /**
     * 根据分类id查询品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandByCid(Long cid) {
        List<Brand> list = brandMapper.queryBrandByCid(cid);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return list;
    }

    public List<Brand> queryBrandByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
