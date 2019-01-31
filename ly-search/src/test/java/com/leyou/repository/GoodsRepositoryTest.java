package com.leyou.repository;

import com.leyou.client.GoodsClient;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Spu;
import com.leyou.pojo.Goods;
import com.leyou.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
@RunWith(SpringRunner.class)
@SpringBootTest
public class GoodsRepositoryTest {

    @Autowired
    private GoodsRepository repository;
    @Autowired
    private ElasticsearchTemplate template;
    @Autowired
    private SearchService searchService;
    @Autowired
    private GoodsClient goodsClient;

    @Test
    public void testCreate() {
        template.createIndex(Goods.class);
        template.putMapping(Goods.class);
    }

    @Test
    public void serviceTest(){
        int page = 1;
        int rows = 100;
        int size = 0;

        do {
            // 查询spu
            PageResult<Spu> result = goodsClient.querySpuByPage(null, page, rows, true);
            List<Spu> spus = result.getItems();
            if (CollectionUtils.isEmpty(spus)) {
                break;
            }
            // 转成goods
            List<Goods> goods = spus.stream().map(searchService::bulidGoods).collect(Collectors.toList());
            repository.saveAll(goods);
            // 翻页
            page++;
            size = spus.size();
        } while (size == 100);

    }
}