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
 * 探测环境违规整改事务卡对象 t_metp_fix_card
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_metp_fix_card")
@ApiModel(value = "TMetpFixCard", description = "探测环境违规整改事务卡")
public class TMetpFixCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 探测环境违规整改卡号 */
    @TableField("biz_no")
    @ApiModelProperty(value = "探测环境违规整改卡号")
    private String bizNo;

    /** 违建建筑物名称（同一座建筑物判重） */
    @TableField("building_name")
    @ApiModelProperty(value = "违建建筑物名称")
    private String buildingName;

    /** 来源线索编号（月初清单转入/省台重查另起一号） */
    @TableField("clue_no")
    @ApiModelProperty(value = "来源线索编号")
    private String clueNo;

    /** 当前停在的段 1调查 2告知 3处理决定 4复查 5结案（受理即建卷，受理段已办完） */
    @TableField("stage")
    @ApiModelProperty(value = "当前停在的段 1调查 2告知 3处理决定 4复查 5结案")
    private Integer stage;

    /** 卡的落定 0在办 1已解除 2已归档（结案即死卷） */
    @TableField("status")
    @ApiModelProperty(value = "卡的落定 0在办 1已解除 2已归档")
    private Integer status;

    /** 与仪器平台的水平距离（米），调查段落下，进告知段即冻结 */
    @TableField("horizontal_dist")
    @ApiModelProperty(value = "与仪器平台的水平距离(米)")
    private BigDecimal horizontalDist;

    /** 遮挡方位角（度），调查段落下，进告知段即冻结 */
    @TableField("blocked_azimuth")
    @ApiModelProperty(value = "遮挡方位角(度)")
    private BigDecimal blockedAzimuth;

    /** 高出仪器平台（米），调查段落下，进告知段即冻结 */
    @TableField("excess_height")
    @ApiModelProperty(value = "高出仪器平台(米)")
    private BigDecimal excessHeight;

    /** 告知书拆除高度（米），复查须与调查记录对得上 */
    @TableField("demolish_height")
    @ApiModelProperty(value = "告知书拆除高度(米)")
    private BigDecimal demolishHeight;

    /** 处理决定正文 */
    @TableField("decision_text")
    @ApiModelProperty(value = "处理决定正文")
    private String decisionText;

    /** 复查轮次（退回一次进一轮） */
    @TableField("sign_round")
    @ApiModelProperty(value = "复查轮次")
    private Integer signRound;

    /** 本轮复查-县局执法承办人点头人 */
    @TableField("officer_sign_by")
    @ApiModelProperty(value = "县局执法承办人点头人")
    private String officerSignBy;

    /** 本轮复查-县局执法承办人点头时刻 */
    @TableField("officer_sign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "县局执法承办人点头时刻")
    private Date officerSignTime;

    /** 本轮复查-台站长点头人 */
    @TableField("chief_sign_by")
    @ApiModelProperty(value = "台站长点头人")
    private String chiefSignBy;

    /** 本轮复查-台站长点头时刻 */
    @TableField("chief_sign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "台站长点头时刻")
    private Date chiefSignTime;

    /** 在办判重键=建筑物名称；结案置空（唯一索引保证同一座建筑物在办仅一卷） */
    @TableField("open_key")
    @ApiModelProperty(value = "在办判重键")
    private String openKey;

    /** 查处记事 */
    @TableField("content")
    @ApiModelProperty(value = "查处记事")
    private String content;

    /** 最近一次过口动作 */
    @TableField("last_action")
    @ApiModelProperty(value = "最近一次过口动作")
    private String lastAction;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
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

    public BigDecimal getHorizontalDist() {
        return horizontalDist;
    }

    public void setHorizontalDist(BigDecimal horizontalDist) {
        this.horizontalDist = horizontalDist;
    }

    public BigDecimal getBlockedAzimuth() {
        return blockedAzimuth;
    }

    public void setBlockedAzimuth(BigDecimal blockedAzimuth) {
        this.blockedAzimuth = blockedAzimuth;
    }

    public BigDecimal getExcessHeight() {
        return excessHeight;
    }

    public void setExcessHeight(BigDecimal excessHeight) {
        this.excessHeight = excessHeight;
    }

    public BigDecimal getDemolishHeight() {
        return demolishHeight;
    }

    public void setDemolishHeight(BigDecimal demolishHeight) {
        this.demolishHeight = demolishHeight;
    }

    public String getDecisionText() {
        return decisionText;
    }

    public void setDecisionText(String decisionText) {
        this.decisionText = decisionText;
    }

    public Integer getSignRound() {
        return signRound;
    }

    public void setSignRound(Integer signRound) {
        this.signRound = signRound;
    }

    public String getOfficerSignBy() {
        return officerSignBy;
    }

    public void setOfficerSignBy(String officerSignBy) {
        this.officerSignBy = officerSignBy;
    }

    public Date getOfficerSignTime() {
        return officerSignTime;
    }

    public void setOfficerSignTime(Date officerSignTime) {
        this.officerSignTime = officerSignTime;
    }

    public String getChiefSignBy() {
        return chiefSignBy;
    }

    public void setChiefSignBy(String chiefSignBy) {
        this.chiefSignBy = chiefSignBy;
    }

    public Date getChiefSignTime() {
        return chiefSignTime;
    }

    public void setChiefSignTime(Date chiefSignTime) {
        this.chiefSignTime = chiefSignTime;
    }

    public String getOpenKey() {
        return openKey;
    }

    public void setOpenKey(String openKey) {
        this.openKey = openKey;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
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
}
