package com.fc.v2.model.auto;

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
 * 查处经过(只追加留底账)对象 t_metp_case_record
 * 每一笔只许 INSERT，不许 UPDATE/DELETE；退回重做时旧笔 voided=1 原样留底。
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_metp_case_record")
@ApiModel(value = "TMetpCaseRecord", description = "查处经过")
public class TMetpCaseRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属案卷主键 */
    @TableField("case_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属案卷主键")
    private Long caseId;

    /** 案卷号 */
    @TableField("case_no")
    @ApiModelProperty(value = "案卷号")
    private String caseNo;

    /** 卷内次序(逐笔递增，查处经过按此正列/倒列) */
    @TableField("seq")
    @ApiModelProperty(value = "卷内次序")
    private Integer seq;

    /** 本笔所在段 1..6 */
    @TableField("stage")
    @ApiModelProperty(value = "本笔所在段 1..6")
    private Integer stage;

    /** 动作 SUBMIT递交 SIGN点头 RETURN退回重做 CLOSE结案 */
    @TableField("action")
    @ApiModelProperty(value = "动作 SUBMIT递交 SIGN点头 RETURN退回重做 CLOSE结案")
    private String action;

    /** 该段当前生效记载 1页面摆出 0已被重做替换 */
    @TableField("current_flag")
    @ApiModelProperty(value = "该段当前生效记载 1页面摆出 0已被重做替换")
    private Integer currentFlag;

    /** 作废留底标记 0有效 1退回重做前的旧话(留底不抹) */
    @TableField("voided")
    @ApiModelProperty(value = "作废留底标记 0有效 1退回重做前的旧话(留底不抹)")
    private Integer voided;

    /** 办理人(系统自落，不许补记) */
    @TableField("operator")
    @ApiModelProperty(value = "办理人(系统自落，不许补记)")
    private String operator;

    /** 办理时刻(系统自落，精确到毫秒定先后) */
    @TableField("operate_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    @ApiModelProperty(value = "办理时刻(系统自落)")
    private Date operateTime;

    /** 点头角色 OFFICER执法承办人 CHIEF台站长 */
    @TableField("sign_role")
    @ApiModelProperty(value = "点头角色 OFFICER执法承办人 CHIEF台站长")
    private String signRole;

    /** 本笔落定时的水平距离(米) */
    @TableField("tech_distance")
    @ApiModelProperty(value = "本笔落定时的水平距离(米)")
    private BigDecimal techDistance;

    /** 本笔落定时的所挡方位角 */
    @TableField("tech_azimuth")
    @ApiModelProperty(value = "本笔落定时的所挡方位角")
    private String techAzimuth;

    /** 本笔落定时的高出平台米数 */
    @TableField("tech_height")
    @ApiModelProperty(value = "本笔落定时的高出平台米数")
    private BigDecimal techHeight;

    /** 本笔落定时的应拆除高度(米) */
    @TableField("demolish_height")
    @ApiModelProperty(value = "本笔落定时的应拆除高度(米)")
    private BigDecimal demolishHeight;

    /** 该段记载内容 */
    @TableField("content")
    @ApiModelProperty(value = "该段记载内容")
    private String content;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getCaseNo() {
        return caseNo;
    }

    public void setCaseNo(String caseNo) {
        this.caseNo = caseNo;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getCurrentFlag() {
        return currentFlag;
    }

    public void setCurrentFlag(Integer currentFlag) {
        this.currentFlag = currentFlag;
    }

    public Integer getVoided() {
        return voided;
    }

    public void setVoided(Integer voided) {
        this.voided = voided;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(Date operateTime) {
        this.operateTime = operateTime;
    }

    public String getSignRole() {
        return signRole;
    }

    public void setSignRole(String signRole) {
        this.signRole = signRole;
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
