package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 探测环境违法建设查处案卷对象 t_metp_case
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_metp_case")
@ApiModel(value = "TMetpCase", description = "探测环境违法建设查处案卷")
public class TMetpCase implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 查处案卷号(一卷一号) */
    @TableField("case_no")
    @ApiModelProperty(value = "查处案卷号(一卷一号)")
    private String caseNo;

    /** 违法线索清单转入编号 */
    @TableField("clue_no")
    @ApiModelProperty(value = "违法线索清单转入编号")
    private String clueNo;

    /** 违法建筑物名称(去重锚点) */
    @TableField("building_name")
    @ApiModelProperty(value = "违法建筑物名称(去重锚点)")
    private String buildingName;

    /** 案卷当前所在段 1受理 2调查 3告知 4处理决定 5复查 6结案 */
    @TableField("stage")
    @ApiModelProperty(value = "案卷当前所在段 1受理 2调查 3告知 4处理决定 5复查 6结案")
    private Integer stage;

    /** 案卷情形 0在办 1已结案(死卷) */
    @TableField("status")
    @ApiModelProperty(value = "案卷情形 0在办 1已结案(死卷)")
    private Integer status;

    /** 与仪器平台水平距离(米) */
    @TableField("tech_distance")
    @ApiModelProperty(value = "与仪器平台水平距离(米)")
    private BigDecimal techDistance;

    /** 所挡方位角 */
    @TableField("tech_azimuth")
    @ApiModelProperty(value = "所挡方位角")
    private String techAzimuth;

    /** 高出仪器平台米数 */
    @TableField("tech_height")
    @ApiModelProperty(value = "高出仪器平台米数")
    private BigDecimal techHeight;

    /** 技术意见冻结标记 0未冻结 1进告知段后冻结 */
    @TableField("tech_frozen")
    @ApiModelProperty(value = "技术意见冻结标记 0未冻结 1进告知段后冻结")
    private Integer techFrozen;

    /** 告知段载明的应拆除高度(米) */
    @TableField("demolish_height")
    @ApiModelProperty(value = "告知段载明的应拆除高度(米)")
    private BigDecimal demolishHeight;

    /** 执法承办人复查点头 0未点 1已点 */
    @TableField("review_officer_sign")
    @ApiModelProperty(value = "执法承办人复查点头 0未点 1已点")
    private Integer reviewOfficerSign;

    /** 台站长复查点头 0未点 1已点 */
    @TableField("review_chief_sign")
    @ApiModelProperty(value = "台站长复查点头 0未点 1已点")
    private Integer reviewChiefSign;

    /** 受理建卷时刻 */
    @TableField("accept_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "受理建卷时刻")
    private Date acceptTime;

    /** 结案时刻(死卷落定) */
    @TableField("close_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "结案时刻(死卷落定)")
    private Date closeTime;

    /** 删除标记 恒为0：案卷不许抽删 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 恒为0：案卷不许抽删")
    private Integer delFlag;

    /** 建卷人(系统自落) */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "建卷人(系统自落)")
    private String createBy;

    /** 建卷时间(系统自落) */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "建卷时间(系统自落)")
    private Date createTime;

    /** 最近办理人(系统自落) */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "最近办理人(系统自落)")
    private String updateBy;

    /** 最近办理时间(系统自落) */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "最近办理时间(系统自落)")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 当前段名（非持久，台账展示用） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前段名")
    private String stageName;

    /** 上一段办理人（非持久，台账展示用） */
    @TableField(exist = false)
    @ApiModelProperty(value = "上一段办理人")
    private String prevOperator;

    /** 告知段是否处于复查退回后的重做态（非持久，按钮分辨入口用） */
    @TableField(exist = false)
    @ApiModelProperty(value = "告知段退回重做态")
    private Boolean noticeRedo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public void setCaseNo(String caseNo) {
        this.caseNo = caseNo;
    }

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

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public Integer getTechFrozen() {
        return techFrozen;
    }

    public void setTechFrozen(Integer techFrozen) {
        this.techFrozen = techFrozen;
    }

    public BigDecimal getDemolishHeight() {
        return demolishHeight;
    }

    public void setDemolishHeight(BigDecimal demolishHeight) {
        this.demolishHeight = demolishHeight;
    }

    public Integer getReviewOfficerSign() {
        return reviewOfficerSign;
    }

    public void setReviewOfficerSign(Integer reviewOfficerSign) {
        this.reviewOfficerSign = reviewOfficerSign;
    }

    public Integer getReviewChiefSign() {
        return reviewChiefSign;
    }

    public void setReviewChiefSign(Integer reviewChiefSign) {
        this.reviewChiefSign = reviewChiefSign;
    }

    public Date getAcceptTime() {
        return acceptTime;
    }

    public void setAcceptTime(Date acceptTime) {
        this.acceptTime = acceptTime;
    }

    public Date getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Date closeTime) {
        this.closeTime = closeTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getPrevOperator() {
        return prevOperator;
    }

    public void setPrevOperator(String prevOperator) {
        this.prevOperator = prevOperator;
    }

    public Boolean getNoticeRedo() {
        return noticeRedo;
    }

    public void setNoticeRedo(Boolean noticeRedo) {
        this.noticeRedo = noticeRedo;
    }
}
