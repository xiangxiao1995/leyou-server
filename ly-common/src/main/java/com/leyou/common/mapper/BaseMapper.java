package com.leyou.common.mapper;

import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.additional.insert.InsertListMapper;
import tk.mybatis.mapper.annotation.RegisterMapper;
import tk.mybatis.mapper.common.Mapper;

/**
 * 自定义通用mapper，继承通用mapper，InsertListMapper<T>, IdListMapper<T,Long>
 * additional.insert.InsertListMapper不支持主键策略，插入前需要设置好主键的值
 * @param <T>
 */
@RegisterMapper //自动注册 Mapper 接口标记
public interface BaseMapper<T> extends Mapper<T>, InsertListMapper<T>, IdListMapper<T,Long> {
}
