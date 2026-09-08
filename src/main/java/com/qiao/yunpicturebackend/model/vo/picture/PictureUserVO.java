package com.qiao.yunpicturebackend.model.vo.picture;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.qiao.yunpicturebackend.model.entity.Picture;
import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUserVO extends PictureVO
        implements Serializable {
    private static final long serialVersionUID = 8718615507939257774L;

    public PictureUserVO(Picture picture) {
        BeanUtil.copyProperties(picture, this);
        // tags属性需要转成json数组
        this.setTags(JSONUtil.toList(JSONUtil.parseArray(picture.getTags()), String.class));
    }
}
