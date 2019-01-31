package com.leyou.order.service;

import com.github.wxpay.sdk.WXPayUtil;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.GoodsClient;
import com.leyou.common.dto.CartDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.interceptor.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderStatusMapper statusMapper;
    @Autowired
    private OrderDetailMapper detailMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private PayHelper payHelper;

    public Long createOrder(OrderDTO orderDTO) {
        // 1 新建订单
        Order order = new Order();
        // 1.1 创建订单id和基本信息
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        order.setCreateTime(new Date());
        order.setPaymentType(orderDTO.getPaymentType());

        // 1.2 买家信息
        UserInfo userInfo = UserInterceptor.getUserInfo();
        order.setUserId(userInfo.getId());
        order.setBuyerNick(userInfo.getUsername() + "user");
        order.setBuyerRate(false);

        // 1.3 计算金额
        // 将List<Cart>转成map格式，key是skuId，value是num
        Map<Long, Integer> skuMap = orderDTO.getCarts().stream().collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
        // 查询所有的sku
        Set<Long> keySet = skuMap.keySet();
        List<Sku> skus = goodsClient.querySkuListByIds(new ArrayList<>(keySet));
        Long totalPrice = 0L;
        List<OrderDetail> details = new ArrayList<>();
        for (Sku sku : skus) {
            totalPrice += sku.getPrice() * skuMap.get(sku.getId());
            // 创建每个sku的订单详情
            OrderDetail detail = new OrderDetail();
            detail.setSkuId(sku.getId());
            detail.setImage(StringUtils.substringBefore(sku.getImages(),","));
            detail.setNum(skuMap.get(sku.getId()));
            detail.setOrderId(orderId);
            detail.setOwnSpec(sku.getOwnSpec());
            detail.setPrice(sku.getPrice());
            detail.setTitle(sku.getTitle());
            details.add(detail);
        }
        order.setTotalPay(totalPrice);
        // 实际金额等于总金额 + 邮费 - 优惠金额
        order.setActualPay(totalPrice + order.getPostFee() - 0);
        // 2 新增订单详情
        int count = detailMapper.insertList(details);
        if (count != details.size()) {
            log.error("[订单服务] 订单新增失败，订单号：{}",orderId);
            throw new LyException(ExceptionEnum.ORDER_CREATED_ERROR);
        }
        //TODO 1.4 收货人信息
        count = orderMapper.insertSelective(order);
        if (count != 1) {
            log.error("[订单服务] 订单新增失败，订单号：{}",orderId);
            throw new LyException(ExceptionEnum.ORDER_CREATED_ERROR);
        }

        // 3 订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.UNPAID.value());
        order.setOrderStatus(orderStatus);
        count = statusMapper.insertSelective(orderStatus);
        if (count != 1) {
            log.error("[订单服务] 订单新增失败，订单号：{}",orderId);
            throw new LyException(ExceptionEnum.ORDER_CREATED_ERROR);
        }
        // 4 减库存 | 两种方案
        // (1) 同步加载，通过FeignClient远程直接调用item-service微服务
        goodsClient.decreaseStock(orderDTO.getCarts());
        // (2) 异步加载，通过发送消息到RabbitMQ，让MQ通知tem-service微服务，但会有分布式事务的问题
        return orderId;
    }

    public Order queryOrderNById(Long orderId) {
        // 查订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        // 查订单详情
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orderId);
        List<OrderDetail> details = detailMapper.select(detail);
        if (CollectionUtils.isEmpty(details)) {
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        order.setOrderDetails(details);
        // 查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        if (orderStatus == null) {
            throw new LyException(ExceptionEnum.ORDER_STATUS_NOT_FOUND);
        }
        order.setOrderStatus(orderStatus);
        return order;
    }

    public String createUrl(Long orderId) {
        // 查询订单
        Order order = queryOrderNById(orderId);

        // 判断订单状态
        Integer status = order.getOrderStatus().getStatus();
        if (status != OrderStatusEnum.UNPAID.value()) {
            log.error("[订单服务] 订单已支付，无需再行支付，订单号：{}",orderId);
            throw new LyException(ExceptionEnum.WX_PAY_ORDER_FAIL);
        }

        // 创建支付链接
        List<OrderDetail> details = order.getOrderDetails();
        String desc = details.get(0).getTitle();
        return payHelper.createPayUrl(orderId, order.getActualPay(), desc);
    }

    /**
     * 微信通知回调
     * @return xml格式
     */
    public void handleNotify(Map<String, String> result) {
        Long orderId = Long.valueOf(result.get("out_trade_no"));

        // 数据校验
        payHelper.isSuccess(orderId,result);

        // 签名校验
        payHelper.isValidSign(result);

        // 金额校验
        Long totalFee = Long.valueOf(result.get("total_fee"));
        Order order = orderMapper.selectByPrimaryKey(orderId);
        Long actualPay = order.getActualPay();
        if (!totalFee.equals(actualPay)) {
            throw new LyException(ExceptionEnum.PAY_ORDER_PARAM_ERROR);
        }

        // 修改订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.UNDELIVERED.value());
        orderStatus.setPaymentTime(new Date());
        int count = statusMapper.updateByPrimaryKeySelective(orderStatus);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_ORDER_STATUS_ERROR);
        }
    }

    /**
     * 查询订单状态
     * @param orderId
     * @return 订单状态码
     */
    public Integer queryOrderStateById(Long orderId) {
        // 查订单状态表
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        if (orderStatus == null) {
            throw new LyException(ExceptionEnum.ORDER_STATUS_NOT_FOUND);
        }
        Integer statusCode = orderStatus.getStatus();
        // 如果已支付，则返回1
        if (OrderStatusEnum.UNDELIVERED.value().equals(statusCode)) {
            return 1;
        }
        // 如果不是已支付状态，则不能确定当前支付状态，需要去向微信主动查询订单状态
        return payHelper.queryOrder(orderId);
    }
}
