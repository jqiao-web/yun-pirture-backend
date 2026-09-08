package com.qiao.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qiao.yunpicturebackend.exception.BusinessException;
import com.qiao.yunpicturebackend.exception.ErrorCode;
import com.qiao.yunpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate{

    @Override
    protected void processFile(Object inputSource, File file) {
        String fileUrl = (String) inputSource;
        // 下载文件
        HttpUtil.downloadFile(fileUrl, file);
    }

    @Override
    public void checkPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
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
        } catch (Exception e) {
            log.info("文件URL访问异常，fileUrl={}", fileUrl, e);
        } finally {
            response.close();
        }
    }

    @Override
    public String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }
}
