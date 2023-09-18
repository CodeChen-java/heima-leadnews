package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.nntp.Article;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    @Override
    //表明当前方法异步
    @Async
    public void autoScanWmNews(Integer id) throws Exception {
        //查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if(wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl---文章不存在");
        }
        if(wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            //从内容中提取纯文本内容和图片
            Map<String, Object> textAndImages = handleTextAndImages(wmNews);
            //自管理的敏感词过滤
            boolean isSensitive = handleSensitiveScan((String) textAndImages.get("content"), wmNews);
            if(isSensitive == false) {
                return;
            }
            //审核文本内容
            boolean isTextScan = handleTextScan((String)textAndImages.get("content"), wmNews);
            if(isTextScan == false) {
                return;
            }
            boolean isImageScan = handleImageScan((List<String>)textAndImages.get("images"), wmNews);
            if(isImageScan == false) {
                return;
            }
            //审核成功 保存app端的相关文章数据
            ResponseResult responseResult = saveAppArticle(wmNews);
            if(!responseResult.getCode().equals(200)) {
                throw new RuntimeException("文章审核，保存app端相关文章数据失败");
            }
            //回填article_id
            wmNews.setArticleId((Long) responseResult.getData());
            updateNews(wmNews, (short) 9, "审核成功");

        }
    }

    @Autowired
    private WmSensitiveMapper sensitiveMapper;
    /**
     * 自管理的敏感词
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitiveScan(String content, WmNews wmNews) {
        boolean flag = true;
        //获取所有的敏感词
        List<WmSensitive> wmSensitives = sensitiveMapper.selectList(
                Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());
        //初始化敏感词库
        SensitiveWordUtil.initMap(sensitiveList);
        //查看文章中是否有敏感词
        Map<String , Integer> map = SensitiveWordUtil.matchWords(content);
        if(map.size() > 0) {
            updateNews(wmNews, (short)2, "当前文章内容存在违规");
            flag = false;
        }
        return flag;
    }


    @Resource
    private IArticleClient iArticleClient;

    @Autowired
    private WmUserMapper userMapper;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    private ResponseResult saveAppArticle(WmNews wmNews) {
        ArticleDto dto = new ArticleDto();
        BeanUtils.copyProperties(wmNews, dto);
        dto.setLayout(wmNews.getType());
        //频道 作者
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if(wmChannel != null) {
            dto.setChannelName(wmChannel.getName());
        }
        dto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = userMapper.selectById(wmNews.getUserId());
        if(wmUser != null) {
            dto.setAuthorName(wmUser.getName());
        }
        if(wmNews.getArticleId()!=null) {
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());
        ResponseResult responseResult = iArticleClient.saveArticle(dto);
        return responseResult;
    }

    /**
     * 审核图片
     * @param images
     * @param wmNews
     * @return
     */
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private GreenImageScan greenImageScan;
    @Autowired
    private Tess4jClient tess4jClient;
    private boolean handleImageScan(List<String> images, WmNews wmNews) throws Exception {
        boolean flag = true;
        if(images == null || images.size() == 0) {
            return flag;
        }
        //下载图片
        //去重
        images = images.stream().distinct().collect(Collectors.toList());
        List<byte[]> imageList = new ArrayList<>();
        for(String image: images) {
            byte[] bytes = fileStorageService.downLoadFile(image);
            //图片识别
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            BufferedImage bufferedImage = ImageIO.read(in);
            String result = tess4jClient.doOCR(bufferedImage);
            //文字过滤
            boolean isSensitive = handleSensitiveScan(result, wmNews);
            if(isSensitive == false) {
                return isSensitive;
            }
            imageList.add(bytes);
        }
        //审核图片
        try {
            Map map = greenImageScan.imageScan(imageList);
            if(map!=null) {
                if(map.get("suggestion").equals("block")) {
                    //审核失败
                    flag = false;
                    updateNews(wmNews, (short) 2, "当前文章图片违规");
                }
                if(map.get("suggestion").equals("review")) {
                    flag = false;
                    //审核失败 人工审核
                    updateNews(wmNews, (short) 3, "当前文章图片不确定，需要人工审核");
                }
            }
        }catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }
        return flag;
    }

    @Autowired
    private GreenTextScan greenTextScan;
    //审核纯文本内容
    private boolean handleTextScan(String content, WmNews wmNews) {
        boolean flag = true;
        if((wmNews.getTitle() + "-" + content).length() == 0) {
            return flag;
        }
        try {
            Map map = greenTextScan.greeTextScan(wmNews.getTitle() + "-" + content);
            if(map!=null) {
                if(map.get("suggestion").equals("block")) {
                    //审核失败
                    flag = false;
                    updateNews(wmNews, (short) 2, "当前文章内容违规");
                }
                if(map.get("suggestion").equals("review")) {
                    flag = false;
                    //审核失败 人工审核
                    updateNews(wmNews, (short) 3, "当前文章内容不确定，需要人工审核");
                }
            }
        }catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }
        return flag;
    }

    //修改文章内容
    private void updateNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 从自媒体文章的内容中提取文本和图片
     * 提取文章的封面图片
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {

        //存储纯文本内容
        StringBuilder textBuilder = new StringBuilder();
        //存储图片url
        List<String> images = new ArrayList<>();
        if(StringUtils.isNotBlank(wmNews.getContent())) {
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for(Map map: maps) {
                if(map.get("type").equals("text")) {
                    //文本内容
                    textBuilder.append(map.get("value"));
                }
                if(map.get("type").equals("image")) {
                    images.add((String) map.get("value"));
                }
            }
        }

        //提取封面图片
        if(StringUtils.isNotBlank(wmNews.getImages())) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("content", textBuilder.toString());
        resultMap.put("images", images);
        return resultMap;
    }
}
