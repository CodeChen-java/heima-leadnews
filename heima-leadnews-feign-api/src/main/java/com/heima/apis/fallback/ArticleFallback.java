package com.heima.apis.fallback;

import com.heima.apis.IArticleClient;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import org.springframework.stereotype.Component;

@Component
public class ArticleFallback implements IArticleClient {
    @Override
    public ResponseResult saveArticle(ArticleDto articleDto) {
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "获取数据失败");
    }
}
