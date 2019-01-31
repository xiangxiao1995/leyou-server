package com.leyou.item.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

/**
 * PO：persistent object，持久化对象，对象属性必须和表中字段完全一致，没有的属性用@Transient声明
 * VO：view object，视图对象，页面展示时使用
 * 实际开发中要区分开PO和VO
 */
@Data
@Table(name = "tb_spu")
public class Spu {
    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;
    private Long brandId;
    private Long cid1;
    private Long cid2;
    private Long cid3;
    private String title;
    private String subTitle;
    private Boolean saleable;
    //返回的JSON数据中不包括@JsonIgnore声明的属性
    @JsonIgnore
    private Boolean valid;
    private Date createTime;
    @JsonIgnore
    private Date lastUpdateTime;
    //数据库表中没有的字段，用@Transient声明
    @Transient
    private String cname;//分类名称
    @Transient
    private String bname;//品牌名称

    @Transient
    private List<Sku> skus;//从前台接收sku数据
    @Transient
    private SpuDetail spuDetail;//从前台接收spuDetail数据

}
