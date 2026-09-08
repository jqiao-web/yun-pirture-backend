package com.qiao.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiao.yunpicturebackend.constant.UserConstant;
import com.qiao.yunpicturebackend.exception.BusinessException;
import com.qiao.yunpicturebackend.exception.ErrorCode;
import com.qiao.yunpicturebackend.exception.ThrowUtils;
import com.qiao.yunpicturebackend.manager.dto.UploadPictureResult;
import com.qiao.yunpicturebackend.manager.upload.FilePictureUpload;
import com.qiao.yunpicturebackend.manager.upload.PictureUploadTemplate;
import com.qiao.yunpicturebackend.manager.upload.UrlPictureUpload;
import com.qiao.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.qiao.yunpicturebackend.model.dto.picture.PictureReviewRequest;
import com.qiao.yunpicturebackend.model.dto.picture.PictureUploadBatchRequest;
import com.qiao.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.qiao.yunpicturebackend.model.entity.Picture;
import com.qiao.yunpicturebackend.model.entity.User;
import com.qiao.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.qiao.yunpicturebackend.model.vo.picture.PictureUserVO;
import com.qiao.yunpicturebackend.model.vo.picture.PictureVO;
import com.qiao.yunpicturebackend.model.vo.user.UserVO;
import com.qiao.yunpicturebackend.service.PictureService;
import com.qiao.yunpicturebackend.mapper.PictureMapper;
import com.qiao.yunpicturebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author qiaoj
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-08-29 15:04:08
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    private final UserService userService;
    private final FilePictureUpload filePictureUpload;
    private final UrlPictureUpload urlPictureUpload;

    /**
     * 上传图片
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureUserVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 1、校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        if (pictureId != null) {
            // 校验管理员权限或本人权限
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.SERVER_ERROR, "图片不存在");
            if (!StrUtil.equals(loginUser.getUserRole(), UserConstant.ADMIN_ROLE)
                    && !oldPicture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权限更新他人图片");
            }
        }
        // 2、上传图片
        String uploadPathPrefix = "public/" + loginUser.getId();
        PictureUploadTemplate uploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            uploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult =
                uploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        Picture picture = new Picture();
        BeanUtil.copyProperties(uploadPictureResult, picture);
        picture.setUserId(loginUser.getId());
        if (pictureId != null) {
            // 更新
            picture.setEditTime(new Date());
            picture.setId(pictureId);
        }
        // 填充请求中携带的其他参数
        String picName = pictureUploadRequest.getPicName();
        String category = pictureUploadRequest.getCategory();
        String tags = JSONUtil.toJsonStr(pictureUploadRequest.getTags());
        String introduction = pictureUploadRequest.getIntroduction();
        picture.setPicName(picName);
        picture.setCategory(category);
        picture.setTags(tags);
        picture.setIntroduction(introduction);
        // 填充审核参数
        fillReviewParams(picture, loginUser);
        // 执行更新或插入
        boolean saved = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!saved, ErrorCode.SERVER_ERROR, "图片上传失败");
        // 重新查询，获取图片完整数据
        Picture newPicture = this.getById(picture.getId());
        return getPictureVO(newPicture, PictureUserVO.class);
    }

    /**
     * 批量上传图片
     * @param pictureUploadBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<PictureUserVO> uploadPictureBatch(PictureUploadBatchRequest pictureUploadBatchRequest, User loginUser) {
        Integer count = pictureUploadBatchRequest.getCount();
        String searchText = pictureUploadBatchRequest.getSearchText();
        String fetchUrl = StrUtil.format("https://cn.bing.com/images/async?q={}&mmasync=1", searchText);
        try {
            Document document = Jsoup.connect(fetchUrl).get();
            Element dgControl = document.getElementsByClass("dgControl").first();
            if (ObjUtil.isEmpty(dgControl)) {
                log.info("图片获取失败，类名为dgControl的标签不存在");
                throw new BusinessException(ErrorCode.SERVER_ERROR, "图片获取失败");
            }
            Elements images = dgControl.select("img.mimg");
            Integer uploadCount = 0;
            List<PictureUserVO> pictureUserVOList = new ArrayList<>();
            String namePrefix = pictureUploadBatchRequest.getNamePrefix();
            for (Element img: images) {
                String imgSrc = img.attr("src");
                if (StrUtil.isBlank(imgSrc)) {
                    // 图片地址为空跳过
                    continue;
                }
                // 处理图片地址
                int questionMarkIndex = imgSrc.indexOf("?");
                if (questionMarkIndex > 0) {
                    // 截取问号之前的部分
                    imgSrc = imgSrc.substring(0, questionMarkIndex);
                }
                try {
                    // 获取其他图片信息
                    PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
                    pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
                    pictureUploadRequest.setIntroduction(pictureUploadBatchRequest.getIntroduction());
                    pictureUploadRequest.setCategory(pictureUploadBatchRequest.getCategory());
                    pictureUploadRequest.setTags(pictureUploadBatchRequest.getTags());
                    PictureUserVO pictureUserVO = this.uploadPicture(imgSrc, pictureUploadRequest, loginUser);
                    pictureUserVOList.add(pictureUserVO);
                    uploadCount++;
                } catch (Exception e) {
                    // 图片上传失败，不抛异常
                    log.info("图片上传失败，图片地址：{}，已跳过", imgSrc);
                }
                if (uploadCount.equals(count)) {
                    break;
                }
            }
            return pictureUserVOList;
        } catch (IOException e) {
            log.info("图片获取失败", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "图片获取失败");
        }
    }

    /**
     * 校验图片字段
     * @param picture
     * @return
     */
    @Override
    public void checkValidPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片为空");
        Long id = picture.getId();
        String introduction = picture.getIntroduction();
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "图片ID为空");
        Picture dataPicture = this.getById(id);
        ThrowUtils.throwIf(dataPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "图片介绍过长");
        }
    }

    /**
     * 根据图片实体获取图片vo
      * @param picture 图片实体
      * @param clazz 转换的vo类
     * @return
     * @param <T>
     */
    @Override
    public <T extends PictureVO> T getPictureVO(Picture picture, Class<T> clazz) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片为空");
        try {
            // 通过反射找到对应的构造方法
            T pictureVO = clazz.getConstructor(Picture.class).newInstance(picture);
            // 解析user字段
            Long userId = picture.getUserId();
            if (userId != null) {
                User user = userService.getById(userId);
                UserVO userVO = userService.getUserVO(user);
                pictureVO.setUser(userVO);
            }
            return pictureVO;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "图片转换失败");
        }
    }

    /**
     * 根据图片分页获取图片vo列表——批量解析user字段
     * @param picturePage
     * @return
     */
    @Override
    public <T extends PictureVO> Page<T> getPictureVOPage(Page<Picture> picturePage, Class<T> clazz) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<T> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 转成PictureVo集合 可能是PictureUserVo 或者 PictureAdminVo
        List<T> pictureVOList = pictureList.stream().map(picture -> {
            try {
                // 通过反射找到对应的构造方法
                return clazz.getConstructor(Picture.class).newInstance(picture);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SERVER_ERROR, "图片转换失败");
            }
        }).collect(Collectors.toList());
        // 获取所有用户id 的 Set集合
        Set<Long> userIdSet = pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());
        // 查询用户表将结果以下边的格式输出
        // key: userId, value: user
        Map<Long, User> userMap = userService.listByIds(userIdSet).stream().collect(Collectors.toMap(User::getId, user -> user));
        // 填充user
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = userMap.get(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        });
        // 设置分页数据
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 根据查询参数获取mytbatisplus查询条件
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureQueryRequest.getId();
        String picName = pictureQueryRequest.getPicName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        Long userId = pictureQueryRequest.getUserId();
        String searchText = pictureQueryRequest.getSearchText();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(searchText)) {
            // 模糊搜索picName或introduction
            queryWrapper
                    .and(w -> w.like("picName", searchText)
                            .or()
                            .like("introduction", searchText));
        }
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.like(StrUtil.isNotBlank(picName), "picName", picName);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        // 补充审核参数
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);

        // 搜索tags
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("asc"), sortField);
        // 默认按创建时间降序
        queryWrapper.orderBy(StrUtil.isBlank(pictureQueryRequest.getSortField()), false, "createTime");
        return queryWrapper;
    }

    /**
     * 审核图片
     * @param pictureReviewRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean reviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1、校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        ThrowUtils.throwIf(
                reviewStatus != PictureReviewStatusEnum.REVIEWING.getValue(),
                ErrorCode.PARAMS_ERROR, "审核状态参数错误");
        // 2、核验图片是否存在，审核状态是否是待审核
        Long pictureId = pictureReviewRequest.getId();
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(oldPicture.getReviewStatus() != PictureReviewStatusEnum.REVIEWING.getValue(),
                ErrorCode.PARAMS_ERROR, "请勿重复审核");
        // 3、更新图片审核状态
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        return this.updateById(updatePicture);
    }

    /**
     * 填充图片审核参数——管理员自动过审
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        String userRole = loginUser.getUserRole();
        if (StrUtil.equals(userRole, UserConstant.ADMIN_ROLE)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }
}




