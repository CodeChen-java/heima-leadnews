package com.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;

public interface ApUserService extends IService<ApUser> {
    /**
     * App端登录功能
     * @param loginDto
     * @return
     */
    public ResponseResult login(LoginDto loginDto);
}
