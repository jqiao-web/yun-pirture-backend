package com.qiao.yunpicturebackend.model.dto.picture;

import com.qiao.yunpicturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureQueryRequest
        extends PageRequest
        implements Serializable {
    private static final long serialVersionUID = -8707025203257770855L;
    /**
     * id
     */
    private Long id;

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
     * 创建人用户id
     */
    private Long userId;

    /**
     * 搜索词（模糊匹配图片名称和图片简介）
     */
    private String searchText;

    /**
     * 审核状态
     * 0-待审核 1-通过 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核人用户id
     */
    private Long reviewerId;
}
