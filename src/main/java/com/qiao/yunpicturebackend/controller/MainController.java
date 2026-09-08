package com.qiao.yunpicturebackend.controller;

import com.qiao.yunpicturebackend.common.BaseResponse;
import com.qiao.yunpicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {
    /**
     * 获取健康状态
     * @return
     */
    @GetMapping("/health")
    public BaseResponse<String> getHealth() {
        return ResultUtils.success("ok");
    }
}
