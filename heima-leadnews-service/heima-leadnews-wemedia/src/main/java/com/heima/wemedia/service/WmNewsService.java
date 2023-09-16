package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.InvocationTargetException;

public interface WmNewsService extends IService<WmNews> {

    /**
     * 条件查询文章列表
     * @param wmNewsPageReqDto
     * @return
     */
    public ResponseResult findList(WmNewsPageReqDto wmNewsPageReqDto);


    /**
     * 发布文章的方法
     * @param wmNewsDto
     * @return
     */
    public ResponseResult submitNews(WmNewsDto wmNewsDto) throws InvocationTargetException, IllegalAccessException;


}
