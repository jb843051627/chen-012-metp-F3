package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 案卷当前段递交入参。
 * 调查段给技术意见三件；告知段给应拆除高度；处理决定段给决定内容。
 * 技术意见一进告知段即冻结，此后各段递交时即便带值也只照录冻结值。
 *
 * @author fuce
 * @date 2026-09-27
 */
@ApiModel(value = "CaseSubmitDto", description = "案卷当前段递交入参")
public class CaseSubmitDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 案卷主键 */
    @ApiModelProperty(value = "案卷主键")
    private Long id;

    /** 与仪器平台水平距离(米)——调查段 */
    @ApiModelProperty(value = "与仪器平台水平距离(米)")
    private BigDecimal techDistance;

    /** 所挡方位角——调查段 */
    @ApiModelProperty(value = "所挡方位角")
    private String techAzimuth;

    /** 高出仪器平台米数——调查段 */
    @ApiModelProperty(value = "高出仪器平台米数")
    private BigDecimal techHeight;

    /** 告知段载明的应拆除高度(米)——告知段 */
    @ApiModelProperty(value = "告知段载明的应拆除高度(米)")
    private BigDecimal demolishHeight;

    /** 本段记载内容（处理决定等） */
    @ApiModelProperty(value = "本段记载内容")
    private String content;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getTechDistance() {
        return techDistance;
    }

    public void setTechDistance(BigDecimal techDistance) {
        this.techDistance = techDistance;
    }

    public String getTechAzimuth() {
        return techAzimuth;
    }

    public void setTechAzimuth(String techAzimuth) {
        this.techAzimuth = techAzimuth;
    }

    public BigDecimal getTechHeight() {
        return techHeight;
    }

    public void setTechHeight(BigDecimal techHeight) {
        this.techHeight = techHeight;
    }

    public BigDecimal getDemolishHeight() {
        return demolishHeight;
    }

    public void setDemolishHeight(BigDecimal demolishHeight) {
        this.demolishHeight = demolishHeight;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
