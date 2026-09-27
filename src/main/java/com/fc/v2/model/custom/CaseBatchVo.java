package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 每月初线索批量转入结果：开了几卷、因同一建筑物名下有在办卷而另开被拒的有哪些。
 *
 * @author fuce
 * @date 2026-09-27
 */
@ApiModel(value = "CaseBatchVo", description = "线索批量受理结果")
public class CaseBatchVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本批线索条数 */
    @ApiModelProperty(value = "本批线索条数")
    private int total;

    /** 新开卷数 */
    @ApiModelProperty(value = "新开卷数")
    private int opened;

    /** 另开被拒（名下已有未结案卷）的建筑物名称 */
    @ApiModelProperty(value = "另开被拒的建筑物")
    private List<String> skipped = new ArrayList<>();

    /** 新开卷号 */
    @ApiModelProperty(value = "新开卷号")
    private List<String> caseNos = new ArrayList<>();

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getOpened() {
        return opened;
    }

    public void setOpened(int opened) {
        this.opened = opened;
    }

    public List<String> getSkipped() {
        return skipped;
    }

    public void setSkipped(List<String> skipped) {
        this.skipped = skipped;
    }

    public List<String> getCaseNos() {
        return caseNos;
    }

    public void setCaseNos(List<String> caseNos) {
        this.caseNos = caseNos;
    }
}
