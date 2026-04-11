package com.sak.service.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictSaveVO {

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型长度不能超过100位")
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签长度不能超过100位")
    private String dictLabel;

    @NotBlank(message = "字典键值不能为空")
    @Size(max = 100, message = "字典键值长度不能超过100位")
    private String dictValue;

    @Size(max = 30, message = "标签类型长度不能超过30位")
    private String tagType;

    @Size(max = 30, message = "标签颜色长度不能超过30位")
    private String tagColor;

    private Integer orderNum;

    @Pattern(regexp = "0|1", message = "状态不合法")
    private String status;

    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;
}
