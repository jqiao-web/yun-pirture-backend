package com.qiao.yunpicturebackend.model.dto.picture;

import java.util.*;
import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadBatchRequest implements Serializable {
    private static final long serialVersionUID = 2704770121109838151L;
    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 搜索条数
     */
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

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
