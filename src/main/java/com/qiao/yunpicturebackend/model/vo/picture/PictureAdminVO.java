package com.qiao.yunpicturebackend.model.vo.picture;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.qiao.yunpicturebackend.model.entity.Picture;
import com.qiao.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PictureAdminVO extends PictureVO
        implements Serializable {
    private static final long serialVersionUID = 6550403274845636405L;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 审核状态：0-待审核；1-通过；2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核状态：0-待审核；1-通过；2-拒绝
     */
    private String reviewStatus_Name;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    public PictureAdminVO(Picture picture) {
        BeanUtil.copyProperties(picture, this);
        // tags属性需要转成json数组
        this.setTags(JSONUtil.toList(JSONUtil.parseArray(picture.getTags()), String.class));
        // 翻译审核状态字段
        String text = PictureReviewStatusEnum.getEnumByValue(picture.getReviewStatus()).getText();
        this.setReviewStatus_Name(text);
    }
}
