package com.qiao.yunpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiao.yunpicturebackend.annotation.AuthCheck;
import com.qiao.yunpicturebackend.common.BaseResponse;
import com.qiao.yunpicturebackend.common.DeleteRequest;
import com.qiao.yunpicturebackend.common.ResultUtils;
import com.qiao.yunpicturebackend.constant.UserConstant;
import com.qiao.yunpicturebackend.exception.ErrorCode;
import com.qiao.yunpicturebackend.exception.ThrowUtils;
import com.qiao.yunpicturebackend.manager.cache.CaffeineCacheManager;
import com.qiao.yunpicturebackend.manager.cache.RedisCacheManager;
import com.qiao.yunpicturebackend.model.dto.picture.*;
import com.qiao.yunpicturebackend.model.entity.Picture;
import com.qiao.yunpicturebackend.model.entity.PictureTagCategory;
import com.qiao.yunpicturebackend.model.entity.User;
import com.qiao.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.qiao.yunpicturebackend.model.vo.picture.PictureAdminVO;
import com.qiao.yunpicturebackend.model.vo.picture.PictureUserVO;
import com.qiao.yunpicturebackend.service.PictureService;
import com.qiao.yunpicturebackend.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;

@RestController
@RequestMapping("/picture")
@Slf4j
@RequiredArgsConstructor
public class PictureController {
    private final PictureService pictureService;
    private final UserService userService;
    private final RedisCacheManager redisCacheManager;
    private final CaffeineCacheManager caffeineCacheManager;

    /**
     * 上传图片
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @ApiOperation(value = "上传图片——登录权限")
    @PostMapping("/upload")
    public BaseResponse<PictureUserVO> uploadPicture(@RequestPart MultipartFile multipartFile,
                                                     PictureUploadRequest pictureUploadRequest,
                                                     HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureUserVO pictureUserVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureUserVO);
    }

    @ApiOperation(value = "通过URL上传图片——登录权限")
    @PostMapping("/upload/url")
    public BaseResponse<PictureUserVO> updatePictureByUrl(PictureUploadRequest pictureUploadRequest,
                                                    HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureUserVO pictureUserVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureUserVO);
    }

    @ApiOperation(value = "批量上传图片——管理员权限")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload/batch")
    public BaseResponse<List<PictureUserVO>> uploadPictureBatch(PictureUploadBatchRequest pictureUploadBatchRequest,
                                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<PictureUserVO> pictureUserVOList = pictureService.uploadPictureBatch(pictureUploadBatchRequest, loginUser);
        return ResultUtils.success(pictureUserVOList);
    }

    /**
     * 更新图片
     * @param pictureUpdateRequest
     * @return
     */
    @ApiOperation(value = "更新图片——管理员权限")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        // 核验图片是否存在
        Long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // DTO转换Entity
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        // 处理tags字段：序列化原始 List，避免 BeanUtil 拷贝时把 List 转成无引号的字符串
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 转换成实体类后核验字段
        pictureService.checkValidPicture(picture);
        // 填充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 更新图片
        boolean updateResult = pictureService.updateById(picture);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "图片更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片（本人上传图片和管理员可编辑）
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @ApiOperation(value = "编辑图片——管理员或本人权限")
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(pictureEditRequest == null || pictureEditRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        // 核验图片是否存在
        Long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 校验编辑权限，仅本人和管理员可编辑
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(
                !loginUser.getId().equals(oldPicture.getUserId())
                        && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NOT_AUTH_ERROR,
                "无权限编辑该图片"
        );
        // DTO转换Entity
        Picture picture = new Picture();
        picture.setEditTime(new Date());
        BeanUtil.copyProperties(pictureEditRequest, picture);
        // 处理tags字段：序列化原始 List，避免 BeanUtil 拷贝时把 List 转成无引号的字符串
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 转换成实体类后核验字段
        pictureService.checkValidPicture(picture);
        // 填充编辑参数
        pictureService.fillReviewParams(picture, loginUser);
        // 更新图片
        boolean editResult = pictureService.updateById(picture);
        ThrowUtils.throwIf(!editResult, ErrorCode.OPERATION_ERROR, "图片更新失败");
        return ResultUtils.success(true);
    }


    /**
     * 删除图片，仅本人和管理员可删除
     * @param deleteRequest
     * @param request
     * @return
     */
    @ApiOperation(value = "删除图片——管理员或本人权限")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        // 核验图片是否存在
        Long id = deleteRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 校验删除权限，仅本人和管理员可删除
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(
                !loginUser.getId().equals(oldPicture.getUserId())
                        && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.NOT_AUTH_ERROR,
                "无权限删除该图片"
        );
        boolean deleteResult = pictureService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!deleteResult, ErrorCode.OPERATION_ERROR, "图片删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片
     * @param id
     * @return
     */
    @ApiOperation(value = "根据id获取图片——管理员权限")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/get")
    public BaseResponse<Picture> getPictureById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        Picture picture = pictureService.getById(id);
        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取图片VO——管理员
     * @param id
     * @return
     */
    @GetMapping("/get/vo/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "根据id获取图片VO——管理员权限")
    public BaseResponse<PictureAdminVO> getPictureAdminVOById(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return ResultUtils.success(pictureService.getPictureVO(picture, PictureAdminVO.class));
    }

    /**
     * 根据id获取图片VO
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    @ApiOperation(value = "根据id获取图片VO——登录权限")
    public BaseResponse<PictureUserVO> getPictureUserVOById(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return ResultUtils.success(pictureService.getPictureVO(picture, PictureUserVO.class));
    }

    /**
     * 获取图片分页列表
     * 缓存查询优化 key规则：项目名:方法名:md5加密查询条件
     * @param pictureQueryRequest
     * @return
     */
    @ApiOperation(value = "获取图片分页列表——管理员权限")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public BaseResponse<Page<PictureAdminVO>> getPicturePage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 构建key
        String queryStr = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryStr.getBytes());
        String key = ":getPicturePage:" + hashKey;
        // 查询缓存
        // 从Caffeine缓存中获取
        BaseResponse baseResponse = caffeineCacheManager.get(key, BaseResponse.class);
        if (baseResponse != null) {
            // Caffeine缓存命中
            return baseResponse;
        }
        // Caffeine缓存未命中，查询二级缓存Redis
        baseResponse = redisCacheManager.get(key, BaseResponse.class);
        if (baseResponse != null) {
            // Redis缓存命中
            return baseResponse;
        }
        // 二级缓存未命中，查询数据库
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()), queryWrapper);
        baseResponse =  ResultUtils.success(pictureService.getPictureVOPage(picturePage, PictureAdminVO.class));
        // 添加缓存数据
        caffeineCacheManager.put(key, baseResponse);
        redisCacheManager.put(key, baseResponse);
        return baseResponse;
    }

    /**
     * 获取已审核图片VO分页列表
     * @param pictureQueryRequest
     * @return
     */
    @ApiOperation(value = "获取图片VO分页列表——登录")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureUserVO>> getPictureVOPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 查询已审核的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        pictureQueryRequest.setReviewerId(null);
        // 构建key
        String queryStr = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryStr.getBytes());
        String key = ":getPictureVOPage:" + hashKey;
        // 查询缓存
        // 从Caffeine缓存中获取
        BaseResponse baseResponse = caffeineCacheManager.get(key, BaseResponse.class);
        if (baseResponse != null) {
            // Caffeine缓存命中
            return baseResponse;
        }
        // Caffeine缓存未命中，查询二级缓存Redis
        baseResponse = redisCacheManager.get(key, BaseResponse.class);
        if (baseResponse != null) {
            // Redis缓存命中
            return baseResponse;
        }
        // 二级缓存未命中，查询数据库
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()), queryWrapper);
        baseResponse = ResultUtils.success(pictureService.getPictureVOPage(picturePage, PictureUserVO.class));
        // 写入二级缓存
        caffeineCacheManager.put(key, baseResponse);
        redisCacheManager.put(key, baseResponse);
        return baseResponse;
    }

    @ApiOperation(value = "获取图片标签类别列表——登录")
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @ApiOperation(value = "图片审核——管理员权限")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(
                pictureReviewRequest == null || pictureReviewRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.reviewPicture(pictureReviewRequest, loginUser);
        return ResultUtils.success(result);
    }
}
