package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping("spec")
public interface SpecificationApi {

    /**
     * 查询规格参数集合
     * @param gid  规格参数组id
     * @param cid  分类id
     * @param searching  是否为搜索字段
     * @return
     */
    @GetMapping("params")
    List<SpecParam> querySpecParamList(
            @RequestParam(name = "gid",required = false)Long gid,
            @RequestParam(name = "cid",required = false)Long cid,
            @RequestParam(name = "searching",required = false)Boolean searching
    );

    /**
     * 根据分类id查询规格参数组集合
     * @param cid
     * @return
     */
    @GetMapping("groupList/{cid}")
    List<SpecGroup> querySpecGroupAndParams(@PathVariable("cid") Long cid);
}
