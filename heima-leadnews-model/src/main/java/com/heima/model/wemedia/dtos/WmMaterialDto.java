package com.heima.model.wemedia.dtos;

import com.heima.model.common.dtos.PageRequestDto;

public class WmMaterialDto extends PageRequestDto {
    private Short isCollection;   //1 收藏 0 未收藏

    public Short getIsCollection() {
        return isCollection;
    }

    public void setIsCollection(Short isCollection) {
        this.isCollection = isCollection;
    }
}
