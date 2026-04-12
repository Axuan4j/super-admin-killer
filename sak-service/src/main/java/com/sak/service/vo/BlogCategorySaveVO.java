package com.sak.service.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlogCategorySaveVO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100位")
    private String categoryName;

    @Size(max = 100, message = "分类别名长度不能超过100位")
    private String slug;

    @Size(max = 500, message = "分类描述长度不能超过500位")
    private String description;

    @Size(max = 255, message = "封面图片长度不能超过255位")
    private String coverImage;

    private Integer orderNum;

    @Pattern(regexp = "0|1", message = "状态不合法")
    private String status;

    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;
}
