package com.szml.movieticket.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.pojo.entity.User;
import com.szml.movieticket.pojo.vo.LoginVO;

/**
 * 认证服务接口。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
public interface AuthService extends IService<User> {

    /**
     * 手机号 + 密码登录。
     *
     * @param phone    手机号
     * @param password 明文密码
     * @return JWT 双令牌 + 用户信息
     */
    LoginVO login(String phone, String password);
}
