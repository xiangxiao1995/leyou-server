package com.leyou.item.iterceptor;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.item.client.AuthClient;
import com.leyou.item.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Slf4j
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class ItemInterceptor implements HandlerInterceptor {

    @Value("${spring.application.name}")
    private String serviceId;
    @Autowired
    private AuthClient authClient;
    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否来自leyou后台，是则直接放行
        String header = request.getHeader("Referer");
        if ("http://manage.leyou.com/".equals(header)) {
            return true;
        }
        //获取cookie或者header中的token
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("LY_TOKEN")) {
                    return true;
                }
            }
        }
        String authorization = request.getHeader("Authorization");
        //先判断是否有Authorization头信息
        if (authorization == null){
            log.error("该服务未授权！");
            response.sendError(403,"该服务未授权！");
            return false;
        }
        //利用发放的公钥验证调用者的token是否正确(不需要再次调用authService进行验证)
        UserInfo userInfo = JwtUtils.getInfoFromToken(authorization, jwtProperties.getPublicKey());
        //UserInfo userInfo = authClient.verify(authorization);
        if (null == userInfo) {
            log.error("该服务无权限！");
            response.sendError(403,"该服务无权限！");
            return false;
        }
        //判断是否有权限
        if (!authClient.hasServicePermission(serviceId, userInfo.getUsername())) return false;
        return true;
    }
}
