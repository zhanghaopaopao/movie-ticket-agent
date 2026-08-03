package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.entity.User;
import com.szml.movieticket.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证服务接口。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
public interface AuthService extends IService<User> {

    /**
     * C端手机号 + 密码登录。
     */
    LoginVO login(String phone, String password);

    /**
     * B端管理员手机号 + 密码登录，非管理员角色直接拒绝。
     */
    LoginVO adminLogin(String phone, String password);

    /**
     * 发送邮箱验证码。
     *
     * @param email   接收邮箱
     * @param purpose 用途：0=注册 1=找回密码
     */
    void sendEmailCode(String email, Integer purpose);

    /**
     * 用户注册。
     *
     * @param phone    手机号
     * @param email    邮箱
     * @param password 密码
     * @param code     邮箱验证码
     */
    void register(String phone, String email, String password, String code);

    /**
     * 找回密码。
     *
     * @param email       邮箱
     * @param code        验证码
     * @param newPassword 新密码
     */
    void resetPassword(String email, String code, String newPassword);

    /**
     * 退出登录，删除 Redis 会话令牌。
     *
     * @param request request
     */
    void logout(HttpServletRequest request);
}