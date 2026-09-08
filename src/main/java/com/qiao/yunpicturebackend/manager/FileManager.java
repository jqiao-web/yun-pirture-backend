package com.qiao.yunpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qiao.yunpicturebackend.config.CosClientConfig;
import com.qiao.yunpicturebackend.exception.BusinessException;
import com.qiao.yunpicturebackend.exception.ErrorCode;
import com.qiao.yunpicturebackend.exception.ThrowUtils;
import com.qiao.yunpicturebackend.manager.dto.UploadPictureResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 通用Cos文件管理器 —— 与业务弱耦合
 * 作用：
 * 1、图片校验
 * 2、上传到桶的key如何确定
 * 3、解析图片参数
 * @Deprecated 已废弃，改用 uploadManager
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Deprecated
public class FileManager {
    private final CosClientConfig cosClientConfig;
    private final CosManager cosManager;

    /**
     * 上传图片
     * @param multipartFile 上传文件
     * @param uploadPathPrefix 上传路径前缀
     * @return UploadPictureResult 上传图片结果，包括业务需要的各项参数
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 1、校验文件
        checkValidPicture(multipartFile);
        // 2、生成随机文件名拼接前缀
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFileName = String.format("%s_%s.%s",
                DateUtil.format(new Date(), "yyyy-MM-dd"),
                uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);
        // 3、上传文件
        File file = null;
        try {
            // 把 MultipartFile 转成 File
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 4、解析图片返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
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
        } catch (IOException e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "文件上传失败");
        } finally {
            if (file != null) {
                boolean deleteResult = file.delete();
                if (!deleteResult) {
                    log.info("文件删除失败: {}", file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 通过URL上传图片
     * @param fileUrl 图片URL
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        // 1、校验文件是否合法，通过URL校验
        checkValidPicture(fileUrl);
        // 2、生成随机文件名拼接前缀
        // 3、上传文件
        return null;
    }

    /**
     * 通过HEAD请求校验文件是否合法
     * @param fileUrl
     */
    private void checkValidPicture(String fileUrl) {
        ThrowUtils.throwIf(fileUrl == null, ErrorCode.PARAMS_ERROR, "文件URL不能为空");
        try {
            // 1、校验URL格式
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件URL格式错误");
        }
        // 2、校验URL协议
        ThrowUtils.throwIf(
                !fileUrl.startsWith("https://") && !fileUrl.startsWith("http://"),
                ErrorCode.PARAMS_ERROR, "文件URL必须以http://或https://开头"
        );
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if(response.getStatus() != HttpStatus.HTTP_OK) {
                // 未正常返回
                return;
            }
            // 3、校验图片类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                List<String> ALLOW_CONTENTTYPE = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENTTYPE.contains(contentType),
                        ErrorCode.PARAMS_ERROR, "文件URL类型仅支持jpg、png、webp");
            }
            // 4、校验图片大小
            long contentLength = response.contentLength();
            ThrowUtils.throwIf(contentLength > cosClientConfig.getMaxSize(),
                    ErrorCode.PARAMS_ERROR, "文件URL大小超出限制");
        } finally {
            response.close();
        }
    }

    /**
     * 校验文件是否合法
     * @param multipartFile
     */
    public void checkValidPicture(MultipartFile multipartFile) {
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
}

