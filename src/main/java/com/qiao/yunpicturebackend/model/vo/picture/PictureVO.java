package com.qiao.yunpicturebackend.model.vo.picture;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.qiao.yunpicturebackend.model.entity.Picture;
import com.qiao.yunpicturebackend.model.vo.user.UserVO;
import lombok.Data;

import java.util.List;

@Data
public class PictureVO {
    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 创建用户信息
     */
    private UserVO user;


    /**
     * 将Picture对象转换为PictureVO对象
     * @param picture
     * @return
     */
    public static PictureVO entityToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);
        // tags属性需要转成json数组
        pictureVO.setTags(JSONUtil.toList(JSONUtil.parseArray(picture.getTags()), String.class));
        return pictureVO;
    }

}
