package com.qiao.yunpicturebackend.model.entity;

import lombok.Data;
import java.util.List;

@Data
public class PictureTagCategory {
    private List<String> tagList;
    private List<String> categoryList;
}
