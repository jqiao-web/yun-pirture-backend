package com.qiao.yunpicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qiao.yunpicturebackend.config.CosClientConfig;
import com.qiao.yunpicturebackend.manager.CosManager;
import com.qiao.yunpicturebackend.manager.dto.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.io.File;

@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    protected CosClientConfig cosClientConfig;
    @Resource
    protected CosManager cosManager;

    /**
     * 上传图片
     * @param inputSource 图片源
     * @param filePathPrefix 文件路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String filePathPrefix) {
        // 1、校验图片
        checkPicture(inputSource);
        // 2、重新生成随机文件名拼接前缀
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        String uploadFileName = StrUtil.format("{}_{}.{}",
                DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadFilePath = StrUtil.format("{}/{}", filePathPrefix, uploadFileName);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadFilePath, null);
            // 处理图片
            processFile(inputSource, file);
            // 上传图片到Cos
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 转换返回结果
            return buildResult(originalFilename, uploadFilePath, imageInfo, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // 清理临时文件
            deleteTempFile(file);
        }

    }

    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            boolean deleteResult = file.delete();
            if (!deleteResult) {
                log.info("上传文件时，删除临时文件：{} 异常", file.getAbsoluteFile());
            }
        }
    }

    /**
     * 返回处理结果
     * @param originalFilename
     * @param uploadFilePath
     * @param imageInfo
     * @param file
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, String uploadFilePath, ImageInfo imageInfo, File file){
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadFilePath);
        // 图片名称取原始图片名称
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        // 计算宽高比，保留一位小数
        double picScale = Math.round((double) picWidth / picHeight * 10) / 10.0;
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }

    /**
     * 处理文件，临时转存到本地
     * @param inputSource
     * @param file
     */
    protected abstract void processFile(Object inputSource, File file);

    /**
     * 核验照片
     * @param inputSource 文件源，可接收 MultipartFile 或 url
     */
    public abstract void checkPicture(Object inputSource);

    /**
     * 获取原文件名
      * @param inputSource 文件源，可接收 MultipartFile 或 url
     * @return
     */
    public abstract String getOriginalFilename(Object inputSource);
}
