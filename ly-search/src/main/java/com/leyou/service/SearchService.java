package com.leyou.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.client.BrandClient;
import com.leyou.client.CategoryClient;
import com.leyou.client.GoodsClient;
import com.leyou.client.SpecificationClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.pojo.Goods;
import com.leyou.pojo.SearchRequest;
import com.leyou.pojo.SearchResult;
import com.leyou.repository.GoodsRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ElasticsearchTemplate template;

    public Goods bulidGoods(Spu spu) {
        Long spuId = spu.getId();
        // 构建goods
        Goods goods = new Goods();

        // 查询所有需要被搜索的信息，包含标题，分类，品牌等
        // (1)查询分类名称
        List<Category> categoryList = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if(CollectionUtils.isEmpty(categoryList)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        List<String> categoryNames = categoryList.stream().map(Category::getName).collect(Collectors.toList());
        // (2)查询品牌名称
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        if (brand == null) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        String all = spu.getTitle() + " " + StringUtils.join(categoryNames," ") + " " + brand.getName();


        // 查询sku
        List<Sku> skuList = goodsClient.querySkuBySpuId(spuId);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        // 查询当前spu的所有sku信息的json结构，用于页面展示(只展示部分字段)
        List<Map<String, Object>> skus = new ArrayList<>();
        List<Long> priceList = new ArrayList<>();
        for (Sku sku : skuList) {
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id",sku.getId());
            skuMap.put("title",sku.getTitle());
            skuMap.put("price",sku.getPrice());
            skuMap.put("image",StringUtils.substringBefore(sku.getImages(),","));
            skus.add(skuMap);
            // 添加sku价格
            priceList.add(sku.getPrice());
        }

        // 查询可搜索的规格参数，key是参数名，值是参数值
        // 查询参数名
        List<SpecParam> params = specificationClient.querySpecParamList(null, spu.getCid3(), true);
        if (CollectionUtils.isEmpty(params)) {
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        // 查询参数值
        SpuDetail spuDetail = goodsClient.querySpuDetailBySpuId(spuId);
        if (spuDetail == null) {
            throw new LyException(ExceptionEnum.GOODS_DETAIL_NOT_FOUND);
        }
        // 将通用规格参数值转成map
        Map<Long, String> genericSpec = JsonUtils.parseMap(spuDetail.getGenericSpec(), Long.class, String.class);
        // 将特殊规格参数值转成map
        Map<Long,List<String>> specialSpec = JsonUtils.nativeRead(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
        });
        // key参数名与value参数值一一对应
        Map<String, Object> specs = new HashMap<>();
        for (SpecParam param : params) {
            String key = param.getName();
            Object value = "";
            if (param.getGeneric()) {
                // 通用规格参数
                value = genericSpec.get(param.getId());
                if (param.getNumeric()) {
                    // 如果是数值类型，将数值所在的段存入索引库
                    value = chooseSegment(value.toString(),param);
                }
            }else {
                // 特殊规格参数
                value = specialSpec.get(param.getId());
            }
            specs.put(key, value);
        }

        // 将转换spu转成goods
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setId(spuId);
        goods.setSubTitle(spu.getSubTitle());
        goods.setAll(all); // 查询所有需要被搜索的信息，包含标题，分类，品牌等
        goods.setPrice(priceList);// 查询当前spu的所有sku价格
        goods.setSkus(JsonUtils.serialize(skus));// 查询当前spu的所有sku信息的json结构，用于页面展示
        goods.setSpecs(specs);// 查询可搜索的规格参数，key是参数名，值是参数值
        // 返回goods
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    /**
     * 查询搜索结果
     * @param request
     * @return
     */
    public PageResult<Goods> search(SearchRequest request) {
        // PageRequest.of(page,size)的page从0开始
        Integer page = request.getPage() - 1;
        Integer size = request.getSize();
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 0 结果过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));
        // 1 分页，PageRequest.of(page,size)的page从0开始
        queryBuilder.withPageable(PageRequest.of(page,size));
        // 2 排序
        if (StringUtils.isNoneBlank(request.getSortBy())) {
            queryBuilder.withSort(SortBuilders.fieldSort(request.getSortBy()).order(request.getDescending() ? SortOrder.DESC : SortOrder.ASC));
        }
        // 3 聚合品牌和分类
        // 3.1 聚合品牌
        String brandAggName = "brand_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        // 3.2 聚合分类
        String categoryAggName = "category_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        // 4 使用布尔查询添加过滤条件、查询条件
        QueryBuilder basicQuery = bulidBasicQuery(request);
        queryBuilder.withQuery(basicQuery);
        // 5 查询
        AggregatedPage<Goods> goods = template.queryForPage(queryBuilder.build(), Goods.class);
        // 6 解析聚合结果
        Aggregations aggregations = goods.getAggregations();
        List<Brand> brands = parseBrandAggs(aggregations.get(brandAggName));
        List<Category> categories = parseCategoryAggs(aggregations.get(categoryAggName));
        // 7 在基础查询条件之上，聚合规格参数
        List<Map<String,Object>> spec = null;
        // 7.1 判断分类聚合结果是否为1个，如果是1个，则聚合规格参数，否则不聚合
        if (categories != null && categories.size() == 1) {
            // 如果是1个，则聚合规格参数
            spec = new ArrayList<>();
            getSpecificationAggs(categories.get(0).getId(),spec,basicQuery);
        }
        // 返回
        return new SearchResult(goods.getTotalElements(), (long) goods.getTotalPages(),goods.getContent(),categories,brands,spec);
    }

    private QueryBuilder bulidBasicQuery(SearchRequest request) {
        // 添加过滤条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        Map<String, String> filter = request.getFilter();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!"cid3".equals(key) && !"brandId".equals(key)) {
                key = "specs."+ key + ".keyword";
            }
            boolQuery.filter(QueryBuilders.termQuery(key,value));
        }
        // 添加查询条件
        boolQuery.must(QueryBuilders.termQuery("all", request.getKey()));
        return boolQuery;
    }

    /**
     * 聚合规格参数
     * @param cid
     * @param spec
     * @param basicQuery
     */
    private void getSpecificationAggs(Long cid, List<Map<String, Object>> spec, QueryBuilder basicQuery) {
        // 1 根据分类id查询规格参数
        List<SpecParam> params = specificationClient.querySpecParamList(null, cid, true);
        // 2 创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 3 添加基础查询条件
        queryBuilder.withQuery(basicQuery);
        // 4 聚合所有可搜索的规格参数
        for (SpecParam param : params) {
            // 将规格参数名称作为聚合的名称
            String name = param.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs."+ name + ".keyword"));
        }
        // 5 查询
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        // 6 解析聚合结果
        Aggregations aggregations = result.getAggregations();
        for (SpecParam param : params) {
            Map<String, Object> map = new HashMap<>();
            // 获取聚合
            StringTerms terms = aggregations.get(param.getName());
            // 获得桶
            List<String> options = terms.getBuckets().stream().map(StringTerms.Bucket::getKeyAsString).collect(Collectors.toList());
            // 将结果存入spec
            map.put("k", param.getName());
            map.put("options", options);
            spec.add(map);
        }


    }

    private List<Category> parseCategoryAggs(LongTerms terms) {
        List<Long> ids = terms.getBuckets().stream().map(bucket -> bucket.getKeyAsNumber().longValue()).collect(Collectors.toList());
        return categoryClient.queryCategoryByIds(ids);
    }

    private List<Brand> parseBrandAggs(LongTerms terms) {
        List<Long> ids = terms.getBuckets().stream().map(bucket -> bucket.getKeyAsNumber().longValue()).collect(Collectors.toList());
        return brandClient.queryBrandByIds(ids);
    }

    /**
     * 操作索引库，完成新增或者更新操作
     * @param spuId
     */
    public void createOrUpdateIndex(Long spuId) {
        // 查询spu
        Spu spu = goodsClient.querySpuBySpuId(spuId);
        // 构建goods
        Goods goods = bulidGoods(spu);
        // 操作索引库，完成新增或者更新操作
        goodsRepository.save(goods);
    }

    /**
     * 操作索引库，完成删除操作
     * @param spuId
     */
    public void deleteIndex(Long spuId) {
        goodsRepository.deleteById(spuId);
    }
}
