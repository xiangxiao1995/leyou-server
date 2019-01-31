package com.leyou.auth.web;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtProperties properties;

    /**
     * 用户登录
     * @param username
     * @param password
     * @param request
     * @param response
     * @return
     */
    @PostMapping("login")
    public ResponseEntity<Void> login(
            @RequestParam("username") String username, @RequestParam("password") String password,
            HttpServletRequest request, HttpServletResponse response) {
        // 登陆校验，返回token
        String token = authService.login(username, password);
        // 将token写入cookie
        CookieUtils.setCookie(request,response,properties.getCookieName(),token);
        // 返回
        return ResponseEntity.ok().build();
    }

    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(@CookieValue("LY_TOKEN") String token,
       HttpServletRequest request, HttpServletResponse response) {
        try {
            // 解析token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, properties.getPublicKey());

            // 更新cookie，重置token的过期时间
            String newToken = JwtUtils.generateToken(userInfo, properties.getPrivateKey(), properties.getExpire());
            CookieUtils.setCookie(request,response,properties.getCookieName(),newToken);

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }

    }
}
