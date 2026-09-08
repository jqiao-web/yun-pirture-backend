package com.qiao.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PictureReviewRequest implements Serializable {
    private static final long serialVersionUID = -2082515192346993341L;
    /**
     * id
     */
    private Long id;

    /**
     * 审核状态：0-待审核；1-通过；2-拒绝
     */
    private Integer reviewStatus;

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
}
