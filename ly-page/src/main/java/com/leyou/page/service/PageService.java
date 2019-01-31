package com.leyou.page.service;

import com.leyou.item.pojo.*;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class PageService {

    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 加载页面需要的数据
     * @param spuId
     * @return
     */
    public Map<String, Object> loadModel(Long spuId) {
        Map<String, Object> model = new HashMap<>();
        // 查询spu
        Spu spu = goodsClient.querySpuBySpuId(spuId);
        // 查询skus
        List<Sku> skus = spu.getSkus();
        // 查询detail
        SpuDetail detail = spu.getSpuDetail();
        // 查询brand
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        // 查询categories
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        // 查询specs
        List<SpecGroup> specs = specificationClient.querySpecGroupAndParams(spu.getCid3());

        model.put("spu", spu);
        model.put("skus", skus);
        model.put("detail", detail);
        model.put("brand", brand);
        model.put("categories", categories);
        model.put("specs", specs);
        return model;
    }

    /**
     * 使用Thymeleaf模板引擎技术，创建静态化页面，提高页面加载速度
     * @param spuId
     */
    public void createHtml(Long spuId) {
        // 创建上下文，加载数据
        Context context = new Context();
        context.setVariables(loadModel(spuId));
        File dest = new File("D:\\安装包\\upload", spuId + ".html");
        // 判断dest是否存在，存在则删除
        if (dest.exists()) {
            dest.delete();
        }
        // 创建输出流
        try(PrintWriter writer = new PrintWriter(dest,"UTF-8")) {
            templateEngine.process("item", context, writer);
        } catch (Exception e) {
            log.error("[静态化服务] 静态化页面出错",e);
        }
    }

    /**
     * 监听到MQ的消息后，删除对应的商品详情页
     * @param spuId
     */
    public void deleteHtml(Long spuId) {
        File dest = new File("D:\\安装包\\upload", spuId + ".html");
        if (dest.exists()) {
            dest.delete();
        }
    }
}
