package com.qiao.yunpicturebackend.common;

import lombok.Data;

@Data
public class PageRequest {
    /**
     * 当前页
     */
    private int current = 1;
    /**
     * 每页大小
     */
    private int pageSize = 10;
    /**
     * 排序字段
     */
    private String sortField;
    /**
     * 排序方式，默认降序
     */
    private String sortOrder = "desc";

}
