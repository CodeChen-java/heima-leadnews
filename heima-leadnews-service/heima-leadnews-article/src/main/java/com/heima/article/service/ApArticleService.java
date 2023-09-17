package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.web.bind.annotation.RequestBody;

public interface ApArticleService extends IService<ApArticle> {
    /**
     *
     * @param dto
     * @param type
     * @return
     */
    public ResponseResult load(ArticleHomeDto dto, Short type);

    public ResponseResult saveArticle(ArticleDto articleDto);
}
