package com.leyou.order.web;

import com.leyou.order.dto.OrderDTO;
import com.leyou.order.pojo.Order;
import com.leyou.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     * @param orderDTO
     * @return 订单id
     */
    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.createOrder(orderDTO));
    }

    /**
     * 根据订单id查询订单
     * @param orderId 订单id
     * @return 订单
     */
    @GetMapping("{orderId}")
    public ResponseEntity<Order> queryOrderNById(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(orderService.queryOrderNById(orderId));
    }

    /**
     * 创建支付链接
     * @param orderId 订单id
     * @return 支付链接
     */
    @GetMapping("/url/{orderId}")
    public ResponseEntity<String> createUrl(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(orderService.createUrl(orderId));
    }

    /**
     * 查询订单状态
     * @param orderId
     * @return 订单状态码
     */
    @GetMapping("/state/{orderId}")
    public ResponseEntity<Integer> queryOrderStateById(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(orderService.queryOrderStateById(orderId));
    }
}
