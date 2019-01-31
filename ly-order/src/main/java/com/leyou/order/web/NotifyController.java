package com.leyou.order.web;

import com.leyou.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("notify")
public class NotifyController {

    @Autowired
    private OrderService orderService;

    /**
     * 微信通知回调
     * @return xml格式
     */
    @PostMapping(value = "pay",produces = "application/xml")
    public Map<String,String> handleNotify(@RequestBody Map<String,String> result) {
        // 处理回调
        orderService.handleNotify(result);
        // 返回成功信息
        log.info("[支付回调] 订单支付成功，订单号：{}",result.get("out_trade_no"));
        Map<String, String> msg = new HashMap<>();
        msg.put("return_code", "SUCCESS");
        msg.put("return_msg", "OK");
        return msg;
    }
}
