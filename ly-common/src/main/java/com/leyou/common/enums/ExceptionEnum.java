package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum  ExceptionEnum {

    PRICE_CANNOT_BE_NULL(400,"价格不能为空"),
    CATEGORY_NOT_FOUND(404,"商品分类未找到"),
    BRAND_CREATED_ERROR(500,"品牌新增失败"),
    BRAND_DELETED_ERROR(500,"品牌删除失败"),
    BRAND_UPDATED_ERROR(500,"品牌更新失败"),
    BRAND_NOT_FOUND(404,"品牌未找到"),
    SPEC_GROUP_CREATED_ERROR(500,"规格参数组新增失败"),
    SPEC_GROUP_DELETED_ERROR(500,"规格参数组删除失败"),
    SPEC_GROUP_UPDATED_ERROR(500,"规格参数组更新失败"),
    SPEC_GROUP_NOT_FOUND(404,"规格参数组不存在"),
    SPEC_PARAM_CREATED_ERROR(500,"规格参数新增失败"),
    SPEC_PARAM_DELETED_ERROR(500,"规格参数删除失败"),
    SPEC_PARAM_UPDATED_ERROR(500,"规格参数更新失败"),
    SPEC_PARAM_NOT_FOUND(404,"规格参数不存在"),
    GOODS_CREATED_ERROR(500,"商品新增失败"),
    GOODS_DETAIL_NOT_FOUND(404,"商品详情未找到"),
    GOODS_DETAIL_UPDATED_ERROR(500,"商品详情更新失败"),
    GOODS_DETAIL_DELETED_ERROR(500,"商品详情删除失败"),
    GOODS_SKU_NOT_FOUND(404,"商品SKU未找到"),
    GOODS_SKU_DELETED_ERROR(500,"商品SKU删除失败"),
    GOODS_STOCK_NOT_FOUND(404,"商品库存未找到"),
    GOODS_STOCK_DELETED_ERROR(500,"商品库存删除失败"),
    GOODS_SPUID_CANNOT_BE_NULL(400,"SPU的id不能为空"),
    GOODS_SPU_UPDATED_ERROR(500,"SPU更新失败"),
    GOODS_SPU_DELETED_ERROR(500,"SPU删除失败"),
    GOODS_SPU_NOT_FOUND(404,"SPU未找到"),
    INVALID_UPLOAD_TYPE(400,"无效的文件类型"),
    UPLOAD_FILE_ERROR(500,"上传文件失败"),
    INVALID_USER_DATA_TYPE(400, "用户数据类型无效"),
    INVALID_VERIFY_CODE(400,"验证码错误"),
    INVALID_USERNAME_PASSWORD(400,"用户名或密码错误"),
    UNAUTHORIZED(403,"用户未授权"),
    CART_NOT_FOUND(404,"购物车为空"),
    ORDER_CREATED_ERROR(500,"订单新增失败"),
    STOCK_NOT_ENOUGH(500,"库存不足"),
    ORDER_NOT_FOUND(404,"订单未找到"),
    ORDER_DETAIL_NOT_FOUND(404,"订单详情未找到"),
    ORDER_STATUS_NOT_FOUND(404,"订单状态未找到"),
    WX_PAY_ORDER_FAIL(500,"微信下单失败"),
    PAY_ORDER_PARAM_ERROR(400,"订单参数有误"),
    UPDATE_ORDER_STATUS_ERROR(500,"更新订单状态失败"),
    QUERY_ORDER_STATUS_ERROR(500,"查询订单状态失败")
    ;
    private int statusCode;
    private String message;
}
