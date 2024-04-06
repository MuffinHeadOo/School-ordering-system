package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    //微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    public WeChatProperties weChatProperties;
    @Autowired
    public UserMapper userMapper;
    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获得当前微信用户的openid(httpclient),方法写在下面了
        String openid = getOpenid(userLoginDTO.getCode());

        //判断openid是否为空，如果为空则表示登录失败，抛出业务异常
        if(openid == null)
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);

        //判断当前微信用户是否为新用户（拿着openid到自己的用户表里去查）
        User user = userMapper.getByOpenid(openid);

        //如果是新用户，则自动完成注册
        if(user == null){
            user = User.builder()
                   .openid(openid)
                   .createTime(LocalDateTime.now())
                   .build();
            userMapper.insert(user);
        }
        //返回这个对象
        return user;
    }


    /**
     * 简化，调用微信接口服务，获取当前用户openid，封装为一个方法
     * @param code
     * @return
     */
    private String getOpenid(String code){
        //调用微信接口服务，获得当前微信用户的openid(httpclient)
        Map<String, String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        //使用fastJson解析返回的唯一标识
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }

}
