package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.contants.WemediaConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtils;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsService;

import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl  extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {


    @Override
    public ResponseResult findList(WmNewsPageReqDto wmNewsPageReqDto) {
        //检查参数
        wmNewsPageReqDto.checkParam();
        //分页条件查询
        IPage page = new Page(wmNewsPageReqDto.getPage(), wmNewsPageReqDto.getSize());
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //状态精确查询
        if(wmNewsPageReqDto.getStatus() != null) {
            lambdaQueryWrapper.eq(WmNews::getStatus, wmNewsPageReqDto.getStatus());
        }
        //频道精确查询
        if(wmNewsPageReqDto.getChannelId() != null) {
            lambdaQueryWrapper.eq(WmNews::getChannelId, wmNewsPageReqDto.getChannelId());
        }
        //时间范围查询
        if(wmNewsPageReqDto.getBeginPubDate() !=null && wmNewsPageReqDto.getEndPubDate()!=null) {
            lambdaQueryWrapper.between(WmNews::getPublishTime, wmNewsPageReqDto.getBeginPubDate(), wmNewsPageReqDto.getEndPubDate());
        }
        //关键字模糊查询
        if(StringUtils.isNotBlank(wmNewsPageReqDto.getKeyword())) {
            lambdaQueryWrapper.like(WmNews::getTitle, wmNewsPageReqDto.getKeyword());
        }
        //查询当前登录人的文章
        lambdaQueryWrapper.eq(WmNews::getUserId, WmThreadLocalUtils.getUser().getId());
        //按照发布时间倒序
        lambdaQueryWrapper.orderByDesc(WmNews::getPublishTime);
        page = page(page, lambdaQueryWrapper);
        ResponseResult responseResult = new PageResponseResult(wmNewsPageReqDto.getPage(), wmNewsPageReqDto.getSize(), (int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Override
    public ResponseResult submitNews(WmNewsDto wmNewsDto) throws InvocationTargetException, IllegalAccessException {
        if(wmNewsDto == null || wmNewsDto.getContent()==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //保存或者修改文章
        WmNews wmNews = new WmNews();
        //属性拷贝
        BeanUtils.copyProperties(wmNewsDto, wmNews);
        //封面图片 list--->String
        if(wmNewsDto.getImages()!=null && wmNewsDto.getImages().size() > 0) {
            String imageStr = StringUtils.join(wmNewsDto.getImages(), ",");
            wmNews.setImages(imageStr);
        }
        //如果当前封面类型为自动 -1
        if(wmNewsDto.getType().equals(-1)) {
            wmNews.setType(null);
        }
        saveOrUpdateWmNews(wmNews);

        //判断是否为草稿  如果为草稿则结束当前方法
        if(wmNewsDto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }

        //不是草稿 保存文章内容图片与素材的关系
        //获取文章内容的图片信息
        List<String> materials = ectractUrlInfo(wmNewsDto.getContent());
        saveRelativeInfoForContent(materials, wmNews.getId());

        //保存文章封面图片与素材的关系 如果当前布局是自动 需要匹配封面图片
        saveRelativeInfoForCover(wmNewsDto, wmNews, materials);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    /**
     * 保存或修改文章
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        //补全属性
        wmNews.setUserId(WmThreadLocalUtils.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setPublishTime(new Date());
        wmNews.setEnable((short)1);   //默认上架

        if(wmNews.getId() == null) {
            //保存
            save(wmNews);
        }else {
            //修改
            //删除文章图片和素材的关系
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }

    //提取文章内容中图片信息的方法
    private List<String> ectractUrlInfo(String content) {
        List<String> materials = new ArrayList<>();
        List<Map> maps = JSON.parseArray(content, Map.class);
        for(Map map: maps) {
            if(map.get("type").equals("image")) {
                String imgUrl = (String)map.get("value");
                materials.add(imgUrl);
            }
        }
        return materials;
    }


    /**
     * 处理文章内容图片与素材的关系
     */
    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        saveRelativeInfo(materials, newsId, (short) 0);   //0表示内容引用
    }

    /**
     * 保存文章图片与素材的关系
     */
    @Autowired
    private WmMaterialMapper wmMaterialMapper;
    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {
        if(materials !=null && !materials.isEmpty()) {
            //通过图片的url查询素材的id
            List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(
                    Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials)
            );
            //判断素材是否有效
            if(dbMaterials == null || dbMaterials.size() == 0) {
                //手动抛出异常 提示调用者素材失效 已经进行数据回滚
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERECE_FAIL);
            }
            if(materials.size() != dbMaterials.size()) {
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERECE_FAIL);
            }
            List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
            //批量保存
            wmNewsMaterialMapper.saveRelations(idList, newsId, type);
        }

    }

    /**
     * 保存文章封面和素材的关系
     * 如果当前封面类型是自动 则设置封面类型的数据
     * 匹配规则：
     * 如果内容图片大于等于1 小于3  单图 type 1
     * 如果内容图片大于等于3 多图 type 3
     * 如果内容没有图片 无图  type 0
     */
    private void saveRelativeInfoForCover(WmNewsDto wmNewsDto,WmNews wmNews,List<String> materials) {
        List<String> images = wmNewsDto.getImages();
        //如果当前封面类型是自动 则设置封面类型的数据
        if(wmNewsDto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            //多图
            if(materials.size() >= 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            }else if(materials.size() >= 1&& materials.size() < 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            }else {
                //无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }

            //修改文章
            if(images!=null && images.size() > 0) {
                wmNews.setImages(StringUtils.join(images, ","));
            }
            updateById(wmNews);
        }
        //保存文章封面和素材的关系
        if(images !=null && images.size() > 0) {
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }
    }
}
