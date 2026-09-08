package com.qiao.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qiao.yunpicturebackend.exception.BusinessException;
import com.qiao.yunpicturebackend.exception.ErrorCode;
import com.qiao.yunpicturebackend.exception.ThrowUtils;
import com.qiao.yunpicturebackend.manager.dto.UploadPictureResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void processFile(Object inputSource, File file) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        try {
            multipartFile.transferTo(file);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "文件转换失败");
        }
    }

    @Override
    public void checkPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        // 1、校验文件是否为空
        ThrowUtils.throwIf(multipartFile.isEmpty(), new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "上传文件不能为空"));
        // 2、校验文件大小
        long size = multipartFile.getSize();
        ThrowUtils.throwIf(size > cosClientConfig.getMaxSize(), new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "文件大小超出限制"));
        // 3、校验文件类型
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!cosClientConfig.getAllowType().contains(suffix),
                new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "上传文件类型错误"));

    }

    @Override
    public String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }
}
