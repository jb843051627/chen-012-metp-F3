package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 违法线索转入、受理建卷入参。
 * 每月初从违法线索清单转进来一批新件，进门只此「受理」一扇。
 *
 * @author fuce
 * @date 2026-09-27
 */
@ApiModel(value = "CaseAcceptDto", description = "线索转受理入参")
public class CaseAcceptDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 违法线索清单转入编号 */
    @ApiModelProperty(value = "违法线索清单转入编号")
    private String clueNo;

    /** 违法建筑物名称（同一建筑物名下有未结案卷的不再另开） */
    @ApiModelProperty(value = "违法建筑物名称")
    private String buildingName;

    /** 受理记载内容 */
    @ApiModelProperty(value = "受理记载内容")
    private String content;

    public String getClueNo() {
        return clueNo;
    }

    public void setClueNo(String clueNo) {
        this.clueNo = clueNo;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
