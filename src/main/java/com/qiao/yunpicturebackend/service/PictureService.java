package com.qiao.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiao.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.qiao.yunpicturebackend.model.dto.picture.PictureReviewRequest;
import com.qiao.yunpicturebackend.model.dto.picture.PictureUploadBatchRequest;
import com.qiao.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.qiao.yunpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qiao.yunpicturebackend.model.entity.User;
import com.qiao.yunpicturebackend.model.vo.picture.PictureUserVO;
import com.qiao.yunpicturebackend.model.vo.picture.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author qiaoj
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-08-29 15:04:08
*/
public interface PictureService extends IService<Picture> {
    PictureUserVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    List<PictureUserVO> uploadPictureBatch(PictureUploadBatchRequest pictureUploadBatchRequest, User loginUser);

    void checkValidPicture(Picture picture);

    <T extends PictureVO> T getPictureVO(Picture picture, Class<T> clazz);

    <T extends PictureVO> Page<T> getPictureVOPage(Page<Picture> picturePage, Class<T> clazz);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    boolean reviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);
}
