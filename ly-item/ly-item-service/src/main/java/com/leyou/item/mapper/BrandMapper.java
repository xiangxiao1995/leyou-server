package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import com.leyou.item.dto.BrandProductDto;
import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BrandMapper extends BaseMapper<Brand> {

    @Insert("insert into tb_category_brand values(#{cid},#{bid})")
    int saveBrandCategory(@Param("cid") Long cid, @Param("bid") Long bid);

    @Delete("delete from tb_category_brand where brand_id = #{bid}")
    int deleteBrandCategory(Long bid);

    @Select("select b.id,b.`name` from tb_brand b INNER JOIN tb_category_brand cb on b.id = cb.brand_id WHERE cb.category_id = #{cid}")
    List<Brand> queryBrandByCid(@Param("cid") Long cid);

    @Select("SELECT COUNT(*) AS value, b.`name` AS name FROM tb_brand b, tb_spu s WHERE b.id = s.brand_id GROUP BY b.id")
    List<BrandProductDto> queryBrandProduct();

    @Select("SELECT DATE_FORMAT(create_time,'%Y-%m-%d') AS name,COUNT(order_id) AS value FROM tb_order GROUP BY DATE_FORMAT(create_time,'%Y-%m-%d')")
    List<BrandProductDto> queryOrderNumByDay();
}
