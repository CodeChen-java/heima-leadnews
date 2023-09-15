package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.utils.thread.WmThreadLocalUtils;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;


@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    public ResponseResult uploadPicture(MultipartFile multipartFile){
        //检查参数
        if(multipartFile == null || multipartFile.getSize()==0 ) return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        //上传图片到MINIO
        String fileName = UUID.randomUUID().toString().replace("-", "");
        String originalFileName = multipartFile.getOriginalFilename();
        String postFix = originalFileName.substring(originalFileName.lastIndexOf("."));
        String fileId = null;
        try {
            fileId = fileStorageService.uploadImgFile("", fileName + postFix, multipartFile.getInputStream());
            log.info("上传图片到MINIO中, fileId:{}", fileId);
        }catch (IOException exception) {
            exception.printStackTrace();
            log.error("上传文件失败！");
        }
        //保存到数据库
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtils.getUser().getId());
        wmMaterial.setUrl(fileId);
        wmMaterial.setIsCollection((short)0);
        wmMaterial.setType((short)0);
        wmMaterial.setCreatedTime(new Date());
        save(wmMaterial);

        return ResponseResult.okResult(wmMaterial);
    }

}
