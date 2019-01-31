package com.leyou.item.web;

import com.leyou.common.dto.CartDTO;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    /**
     * 分页查询商品
     * @param key
     * @param page
     * @param rows
     * @param saleable
     * @return
     */
    @GetMapping("/spu/page")
    public ResponseEntity<PageResult<Spu>> querySpuByPage(
            @RequestParam(name = "key",required = false) String key,
            @RequestParam(name = "page",defaultValue = "1") Integer page,
            @RequestParam(name = "rows",defaultValue = "5") Integer rows,
            @RequestParam(name = "saleable",required = false) Boolean saleable
    ) {
        return ResponseEntity.ok(goodsService.querySpuByPage(key, page, rows, saleable));
    }

    /**
     * 保存商品
     * @param spu
     * @return
     */
    @PostMapping("goods")
    public ResponseEntity<Void> saveGoods(@RequestBody Spu spu) {
        goodsService.saveGoods(spu);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据spuId查询spuDetail
     * @param spuId
     * @return
     */
    @GetMapping("/spu/detail/{spuId}")
    public ResponseEntity<SpuDetail> querySpuDetailBySpuId(@PathVariable("spuId") Long spuId) {
        return ResponseEntity.ok(goodsService.querySpuDetailBySpuId(spuId));
    }

    /**
     * 根据spuId查询sku集合
     * @param spuId
     * @return
     */
    @GetMapping("/sku/list")
    public ResponseEntity<List<Sku>> querySkuBySpuId(@RequestParam("id") Long spuId) {
        return ResponseEntity.ok(goodsService.querySkuBySpuId(spuId));
    }

    /**
     * 更新商品信息(不包括上下架)
     * @param spu
     * @return
     */
    @PutMapping("goods")
    public ResponseEntity<Void> updateGoods(@RequestBody Spu spu) {
        goodsService.updateGoods(spu);
        return ResponseEntity.ok().build();
    }

    /**
     * 商品上下架
     * @param spuId
     * @return
     */
    @PutMapping("/goods/{spuId}/{saleable}")
    public ResponseEntity<Void> loadAndWithdrawGoods(@PathVariable("spuId") Long spuId,@PathVariable("saleable")Boolean saleable) {
        goodsService.loadAndWithdrawGoods(spuId, saleable);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除商品
     * @param spuId
     * @return
     */
    @DeleteMapping("/goods/{spuId}")
    public ResponseEntity<Void> deleteGoods(@PathVariable("spuId")Long spuId) {
        goodsService.deleteGoods(spuId);
        return ResponseEntity.ok().build();
    }

    /**
     * 根据spuId查询spu
     * @param spuId
     * @return
     */
    @GetMapping("/spu/{id}")
    public ResponseEntity<Spu> querySpuBySpuId(@PathVariable("id")Long spuId) {
        return ResponseEntity.ok(goodsService.querySpuBySpuId(spuId));
    }

    /**
     * 根据skuId查询sku
     * @param skuId
     * @return spuId
     */
    @GetMapping("sku/{skuId}")
    public ResponseEntity<Long> querySpuIdBySkuId(@PathVariable("skuId")Long skuId) {
        return ResponseEntity.ok(goodsService.querySpuIdBySkuId(skuId));
    }

    /**
     * 根据skuId集合查询sku集合
     * @param ids
     * @return
     */
    @GetMapping("sku/ids")
    public ResponseEntity<List<Sku>> querySkuListByIds(@RequestParam("ids") List<Long> ids) {
        return ResponseEntity.ok(goodsService.querySkuListByIds(ids));
    }

    /**
     * 减库存
     * @param cartDTOList skuId和num的集合
     * @return
     */
    @PostMapping("stock/decrease")
    public ResponseEntity<Void> decreaseStock(@RequestBody List<CartDTO> cartDTOList) {
        goodsService.decreaseStock(cartDTOList);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
