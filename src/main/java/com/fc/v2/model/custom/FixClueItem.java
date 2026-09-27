package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 违法线索清单转入项。每月初转进来一批；
 * 同一座建筑物名下已有一卷没走到终点的，不再另开第二卷。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ApiModel(value = "FixClueItem", description = "违法线索清单转入项")
public class FixClueItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 违建建筑物名称（同一座建筑物判重） */
    @ApiModelProperty(value = "违建建筑物名称", required = true)
    private String buildingName;

    /** 来源线索编号 */
    @ApiModelProperty(value = "来源线索编号")
    private String clueNo;

    /** 线索记事 */
    @ApiModelProperty(value = "线索记事")
    private String content;

    public FixClueItem() {
    }

    public FixClueItem(String buildingName, String clueNo, String content) {
        this.buildingName = buildingName;
        this.clueNo = clueNo;
        this.content = content;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getClueNo() {
        return clueNo;
    }

    public void setClueNo(String clueNo) {
        this.clueNo = clueNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
