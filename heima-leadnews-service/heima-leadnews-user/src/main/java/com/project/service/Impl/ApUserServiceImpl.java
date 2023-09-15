package com.project.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.common.AppJwtUtil;
import com.project.mapper.ApUserMapper;
import com.project.service.ApUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;

@Service
@Transactional
@Slf4j
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService {
    @Override
    public ResponseResult login(LoginDto loginDto) {
        //1 正常登录 用户名和密码
        if(StringUtils.isNotBlank(loginDto.getPhone()) && StringUtils.isNotBlank(loginDto.getPassword())) {
            //1.1 根据手机号查询信息
            ApUser apUser = getOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, loginDto.getPhone()));
            if(apUser == null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户信息不存在");
            }
            //1.2 比对密码
            String salt = apUser.getSalt();
            String password = loginDto.getPassword();
            String pswd = DigestUtils.md5DigestAsHex((password + salt).getBytes());
            if(!pswd.equals(apUser.getPassword())) {
                return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
            }
            //1.3 返回信息 jwt
            String token = AppJwtUtil.getToken(apUser.getId().longValue());
            Map<String, Object> map = new HashMap<String, Object>();
            apUser.setSalt("");
            apUser.setPassword("");
            map.put("token", token);
            map.put("user", apUser);
            return ResponseResult.okResult(map);
        }else {
            //2 游客登录
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("token", AppJwtUtil.getToken(0L));
            return ResponseResult.okResult(map);
        }
    }
}
