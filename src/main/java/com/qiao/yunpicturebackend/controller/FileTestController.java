package com.qiao.yunpicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.qiao.yunpicturebackend.annotation.AuthCheck;
import com.qiao.yunpicturebackend.common.BaseResponse;
import com.qiao.yunpicturebackend.common.ResultUtils;
import com.qiao.yunpicturebackend.constant.UserConstant;
import com.qiao.yunpicturebackend.exception.BusinessException;
import com.qiao.yunpicturebackend.exception.ErrorCode;
import com.qiao.yunpicturebackend.manager.CosManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

//@RestController
@Slf4j
@RequiredArgsConstructor
public class FileTestController {
    private final CosManager cosManager;
    private final View error;

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> upload(@RequestPart("file") MultipartFile multipartFile) {
        String filename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", filename);
        // 把 MultipartFile 转成 File
        // 1、创建临时文件
        File file = null;
        try {
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filePath, file);
            return ResultUtils.success(filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (file != null) {
                boolean deleteResult = file.delete();
                if (!deleteResult) {
                    log.info("文件删除失败: {}", file.getAbsolutePath());
                }
            }
        }
    }

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void download(String filePath, HttpServletResponse response) {
        COSObjectInputStream cosObjectInputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filePath);
            cosObjectInputStream = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosObjectInputStream);
            // 设置响应头
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filePath);
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "文件下载失败");
        } finally {
            if (cosObjectInputStream != null) {
                try {
                    cosObjectInputStream.close();
                } catch (IOException e) {
                    log.error("输入流关闭失败", e);
                }
            }
        }
    }
}
