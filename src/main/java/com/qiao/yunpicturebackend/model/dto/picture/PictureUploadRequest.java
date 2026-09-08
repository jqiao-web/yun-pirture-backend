package com.qiao.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = -4996015066058391709L;

    /**
     * 文件url
     */
    private String fileUrl;

    /**
     * id 不传时新建
     */
    private Long id;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 统一的图片简介
     */
    private String introduction;

    /**
     * 统一的图片分类
     */
    private String category;

    /**
     * 统一的图片标签
     */
    private List<String> tags;
}
