package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private BrandService brandService;
    @Autowired
    private SpuDetailMapper spuDetailMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 分页查询spu
     * @param key
     * @param page
     * @param rows
     * @param saleable
     * @return
     */
    public PageResult<Spu> querySpuByPage(String key, Integer page, Integer rows, Boolean saleable) {
        //分页
        PageHelper.startPage(page, rows);
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //搜索过滤
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        //上下架过滤
        if (saleable != null) {
            criteria.andEqualTo("saleable",saleable ? 1 : 0);
        }
        //默认按更新时间排序
        example.setOrderByClause("last_update_time DESC");
        //查询
        List<Spu> list = spuMapper.selectByExample(example);

        //查询分类名称和品牌名称
        loadCategoryAndBrandName(list);

        PageInfo<Spu> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(),list);
    }

    /**
     * 保存商品
     * @param spu
     */
    public void saveGoods(Spu spu) {
        //新增spu
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        spu.setSaleable(true);
        spu.setValid(true);
        int count = spuMapper.insert(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_CREATED_ERROR);
        }
        //新增spuDetail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        count = spuDetailMapper.insert(spuDetail);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_CREATED_ERROR);
        }
        saveSkuAndStock(spu);

        //发送消息
        amqpTemplate.convertAndSend("item.insert",spu.getId());
    }

    private void saveSkuAndStock(Spu spu) {
        //新增sku
        int count;
        List<Stock> stockList = new ArrayList<>();
        List<Sku> skus = spu.getSkus();
        for (Sku sku : skus) {
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            sku.setSpuId(spu.getId());
            count = skuMapper.insert(sku);
            if (count != 1) {
                throw new LyException(ExceptionEnum.GOODS_CREATED_ERROR);
            }
            //新增库存
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockList.add(stock);
        }
        //批量新增stock
        stockMapper.insertList(stockList);
    }

    /**
     *根据spu获取三级分类名称和品牌名称
     * @param spus
     */
    public void loadCategoryAndBrandName(List<Spu> spus) {
        for (Spu spu : spus) {
            //查询分类名称
            List<String> cnameList = categoryService.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3())).stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(cnameList,"/"));
            //查询品牌名称
            Brand brand = brandService.queryBrandById(spu.getBrandId());
            spu.setBname(brand.getName());
        }
    }


    /**
     * 根据spuId查询spuDetail
     * @param spuId
     * @return
     */
    public SpuDetail querySpuDetailBySpuId(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (spuDetail == null) {
            throw new LyException(ExceptionEnum.GOODS_DETAIL_NOT_FOUND);
        }
        return spuDetail;
    }

    /**
     * 根据spuId查询sku集合
     * @param spuId
     * @return
     */
    public List<Sku> querySkuBySpuId(Long spuId) {
        //查询sku集合
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        //批量查询库存
        List<Long> skuIds = skuList.stream().map(Sku::getId).collect(Collectors.toList());
        List<Stock> stockList = stockMapper.selectByIdList(skuIds);
        if (CollectionUtils.isEmpty(stockList)) {
            throw new LyException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }
        //将查到的库存集合转成map格式，map的key是skuId，value是库存量stock
        Map<Long, Integer> stockMap = stockList.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        //将库存存入相应的sku中
        skuList.forEach(s -> s.setStock(stockMap.get(s.getId())));
        return skuList;
    }

    /**
     * 更新商品信息
     * @param spu
     */
    public void updateGoods(Spu spu) {
        Long spuId = spu.getId();
        //删除StockAndSku
        deleteStockAndSku(spuId);
        int count;
        //更新spu
        spu.setLastUpdateTime(new Date());
        spu.setValid(true);
        count = spuMapper.updateByPrimaryKey(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_SPU_UPDATED_ERROR);
        }
        //更新spuDetail
        count = spuDetailMapper.updateByPrimaryKey(spu.getSpuDetail());
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_DETAIL_UPDATED_ERROR);
        }
        //新增stock和sku
        saveSkuAndStock(spu);

        //发送消息
        amqpTemplate.convertAndSend("item.update",spu.getId());
    }

    private void deleteStockAndSku(Long spuId) {
        //判断spuId是否为空
        if (spuId == null) {
            throw new LyException(ExceptionEnum.GOODS_SPUID_CANNOT_BE_NULL);
        }
        //查询sku集合
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(sku);
        List<Long> skuIds = skuList.stream().map(Sku::getId).collect(Collectors.toList());
        //批量删除stock
        int count = stockMapper.deleteByIdList(skuIds);
        if (count != skuList.size()) {
            throw new LyException(ExceptionEnum.GOODS_STOCK_DELETED_ERROR);
        }
        //批量删除sku
        count = skuMapper.deleteByIdList(skuIds);
        if (count != skuList.size()) {
            throw new LyException(ExceptionEnum.GOODS_SKU_DELETED_ERROR);
        }
    }

    /**
     * 商品上下架
     * @param spuId
     * @param saleable
     */
    public void loadAndWithdrawGoods(Long spuId, Boolean saleable) {
        //判断spuId是否为空
        if (spuId == null) {
            throw new LyException(ExceptionEnum.GOODS_SPUID_CANNOT_BE_NULL);
        }
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        //更新spu的上下架
        spu.setSaleable(saleable);
        spu.setLastUpdateTime(new Date());
        int count = spuMapper.updateByPrimaryKey(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_SPU_UPDATED_ERROR);
        }
    }

    /**
     * 删除商品
     * @param spuId
     */
    public void deleteGoods(Long spuId) {
        //删除stock和sku
       deleteStockAndSku(spuId);

        //删除spuDetail
        int count = spuDetailMapper.deleteByPrimaryKey(spuId);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_DETAIL_DELETED_ERROR);
        }

        //删除spu
        count = spuMapper.deleteByPrimaryKey(spuId);
        if (count != 1) {
            throw new LyException(ExceptionEnum.GOODS_SPU_DELETED_ERROR);
        }

        //发送消息
        amqpTemplate.convertAndSend("item.delete",spuId);
    }

    /**
     * 根据spuId查询spu
     * @param spuId
     * @return
     */
    public Spu querySpuBySpuId(Long spuId) {
        // 查询spu
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        if (spu == null) {
            throw new LyException(ExceptionEnum.GOODS_SPU_NOT_FOUND);
        }

        // 查询skus
        spu.setSkus(querySkuBySpuId(spuId));

        // 查询spuDetail
        spu.setSpuDetail(querySpuDetailBySpuId(spuId));

        return spu;
    }

    /**
     * 根据skuId查询sku
     * @param skuId
     * @return spuId
     */
    public Long querySpuIdBySkuId(Long skuId) {
        Sku sku = skuMapper.selectByPrimaryKey(skuId);
        if (sku == null) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        return sku.getSpuId();
    }

    /**
     * 根据skuId集合查询sku集合
     * @param ids
     * @return
     */
    public List<Sku> querySkuListByIds(List<Long> ids) {
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skus)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        return skus;
    }

    /**
     * 减库存
     * @param cartDTOList skuId和num的集合
     * @return
     */
    public void decreaseStock(List<CartDTO> cartDTOList) {

        for (CartDTO cartDTO : cartDTOList) {
            int count = skuMapper.decreaseStock(cartDTO.getSkuId(), cartDTO.getNum());
            if (count != 1) {
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
