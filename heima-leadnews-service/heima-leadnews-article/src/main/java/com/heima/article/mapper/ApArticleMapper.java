package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ApArticleMapper extends BaseMapper<ApArticle> {
    /**
     * 加载文章列表 1 加载更多  2加载最新
     * @param dto
     * @param type
     * @return
     */
    public List<ApArticle> loadArticleList(ArticleHomeDto dto, Short type);
}
