package com.leyou.item.client;

import com.leyou.auth.api.AuthApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "auth-service")
public interface AuthClient extends AuthApi {
}
