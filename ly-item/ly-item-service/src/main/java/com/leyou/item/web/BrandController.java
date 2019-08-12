package com.leyou.item.web;

import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.BrandProductDto;
import com.leyou.item.pojo.Brand;
import com.leyou.item.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    /**
     * 分页查询所有品牌
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @param key
     * @return
     */
    @GetMapping("page")
    public ResponseEntity<PageResult<Brand>> queryBrandByPage(
            @RequestParam(name = "page",defaultValue = "1") Integer page,
            @RequestParam(name = "rows",defaultValue = "5") Integer rows,
            @RequestParam(name = "sortBy",required = false) String  sortBy,
            @RequestParam(name = "desc",defaultValue = "false") Boolean desc,
            @RequestParam(name = "key",required = false) String key
    ){
        return ResponseEntity.ok(brandService.queryBrandByPage(page, rows, sortBy, desc, key));
    }

    /**
     * 新增商品分类
     * @param brand
     * @param cids
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> saveBrand(Brand brand, @RequestParam("categories")ArrayList<Long> cids){
        brandService.saveBrand(brand, cids);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 更新品牌信息
     * @param brand
     * @param cids
     * @return
     */
    @PutMapping
    public ResponseEntity<Void> updateBrand(Brand brand,@RequestParam("categories")ArrayList<Long> cids){
        brandService.updateBrand(brand, cids);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除品牌信息
     * @param bid
     * @return
     */
    @DeleteMapping("{bid}")
    public ResponseEntity<Void> deleteBrandByBid(@PathVariable("bid")Long bid){
        brandService.deleteBrandByBid(bid);
        return ResponseEntity.ok().build();
    }

    /**
     * 根据cid查询品牌
     * @param cid
     * @return
     */
    @GetMapping("/cid/{cid}")
    public ResponseEntity<List<Brand>> queryBrandByCid(@PathVariable("cid") Long cid) {
        return ResponseEntity.ok(brandService.queryBrandByCid(cid));
    }

    /**
     * 根据品牌id查询品牌
     * @param id
     * @return
     */
    @GetMapping("{id}")
    public ResponseEntity<Brand> queryBrandById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(brandService.queryBrandById(id));
    }

    /**
     * 根据品牌id集合查询品牌集合
     * @param ids id集合
     * @return List<Brand>
     */
    @GetMapping("list")
    public ResponseEntity<List<Brand>> queryBrandByIds(@RequestParam("ids") List<Long> ids) {
        return ResponseEntity.ok(brandService.queryBrandByIds(ids));
    }

    /**
     * 获取所有品牌以及品牌下辖的商品数
     * @return List<BrandProductDto>
     */
    @GetMapping("/chart/brand")
    public ResponseEntity<List<BrandProductDto>> queryBrandProduct() {
        return ResponseEntity.ok(brandService.queryBrandProduct());
    }

    @GetMapping("/chart/order")
    public ResponseEntity<List<BrandProductDto>> queryOrderNumByDay() {
        return ResponseEntity.ok(brandService.queryOrderNumByDay());
    }
}
