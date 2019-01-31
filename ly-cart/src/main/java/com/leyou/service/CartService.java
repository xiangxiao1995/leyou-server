package com.leyou.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.interceptor.UserInterceptor;
import com.leyou.pojo.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:uid:";
    /**
     * 将商品放入购物车
     * @param cart 商品
     * @return
     */
    public void addCart(Cart cart) {
        // 获取用户
        UserInfo userInfo = UserInterceptor.getUserInfo();
        String key = KEY_PREFIX + userInfo.getId().toString();
        String hashKey = cart.getSkuId().toString();

        // 判断购物车中是否已有该件商品
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        if (operations.hasKey(hashKey)) {
            // 有则修改数量
            Cart cacheCart = JsonUtils.parse(operations.get(hashKey).toString(), Cart.class);
            cacheCart.setNum(cacheCart.getNum() + cart.getNum());
            // 重新写入redis
            operations.put(hashKey,JsonUtils.serialize(cacheCart));
        }else {
            // 无则直接放入
            operations.put(hashKey,JsonUtils.serialize(cart));
        }
    }

    /**
     * 查询redis中对应用户的购物车
     * @return
     */
    public List<Cart> queryCartList() {
        // 获取用户
        UserInfo userInfo = UserInterceptor.getUserInfo();
        String key = KEY_PREFIX + userInfo.getId().toString();

        if(!redisTemplate.hasKey(key)){
            throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        List<Cart> carts = operations.values().stream().map(o -> JsonUtils.parse(o.toString(), Cart.class)).collect(Collectors.toList());
        return carts;
    }

    /**
     * 更新购物车中商品的数量
     * @param skuId
     * @param num
     * @return
     */
    public void updateCartNum(Long skuId, Integer num) {
        // 获取用户
        UserInfo userInfo = UserInterceptor.getUserInfo();
        String key = KEY_PREFIX + userInfo.getId().toString();
        String hashKey = skuId.toString();

        // 获取操作
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        if (!operations.hasKey(hashKey)) {
            throw new LyException(ExceptionEnum.CART_NOT_FOUND);
        }
        // 获取原来的数据
        Cart cart = JsonUtils.parse(operations.get(hashKey).toString(), Cart.class);
        cart.setNum(num);

        // 写入到redis中
        operations.put(hashKey,JsonUtils.serialize(cart));
    }

    /**
     * 删除购物车中的商品
     * @param skuId
     * @return
     */
    public void deleteCart(Long skuId) {
        // 获取用户
        UserInfo userInfo = UserInterceptor.getUserInfo();
        String key = KEY_PREFIX + userInfo.getId().toString();
        String hashKey = skuId.toString();
        // 删除
        redisTemplate.opsForHash().delete(key,hashKey);
    }
}
