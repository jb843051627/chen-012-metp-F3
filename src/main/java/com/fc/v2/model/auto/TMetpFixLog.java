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
 * 查处经过流水对象 t_metp_fix_log
 * 只增、不改、不删：谁递的、几点几分递的由系统落下，事后补记一概不认；
 * 退回作废旧话留底（退回一笔的 voidedSeqs 指向被作废的卷内序号），先人记录原封不动。
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_metp_fix_log")
@ApiModel(value = "TMetpFixLog", description = "查处经过流水")
public class TMetpFixLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属卷主键 */
    @TableField("card_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属卷主键")
    private Long cardId;

    /** 卷内序号（自1递增，卷内唯一，正倒列皆以此为准） */
    @TableField("seq")
    @ApiModelProperty(value = "卷内序号")
    private Integer seq;

    /** 本笔所属段 0受理 1调查 2告知 3处理决定 4复查 5结案 */
    @TableField("stage")
    @ApiModelProperty(value = "本笔所属段 0受理 1调查 2告知 3处理决定 4复查 5结案")
    private Integer stage;

    /** 过口动作 ACCEPT/INVESTIGATE/NOTIFY/DECIDE/REVIEW_SIGN/RETURN/CLOSE */
    @TableField("action")
    @ApiModelProperty(value = "过口动作")
    private String action;

    /** 本笔落下时的水平距离快照（米） */
    @TableField("horizontal_dist")
    @ApiModelProperty(value = "水平距离快照(米)")
    private BigDecimal horizontalDist;

    /** 本笔落下时的遮挡方位角快照（度） */
    @TableField("blocked_azimuth")
    @ApiModelProperty(value = "遮挡方位角快照(度)")
    private BigDecimal blockedAzimuth;

    /** 本笔落下时的高出平台快照（米） */
    @TableField("excess_height")
    @ApiModelProperty(value = "高出平台快照(米)")
    private BigDecimal excessHeight;

    /** 告知笔的拆除高度（米） */
    @TableField("demolish_height")
    @ApiModelProperty(value = "拆除高度(米)")
    private BigDecimal demolishHeight;

    /** 复查点头角色 OFFICER执法承办人/CHIEF台站长 */
    @TableField("sign_role")
    @ApiModelProperty(value = "复查点头角色")
    private String signRole;

    /** 复查点头所属轮次 */
    @TableField("sign_round")
    @ApiModelProperty(value = "复查点头所属轮次")
    private Integer signRound;

    /** 退回笔：退回前所在段 */
    @TableField("from_stage")
    @ApiModelProperty(value = "退回前所在段")
    private Integer fromStage;

    /** 退回笔：退到那一段 */
    @TableField("to_stage")
    @ApiModelProperty(value = "退到那一段")
    private Integer toStage;

    /** 退回笔：被作废旧话的卷内序号（逗号分隔，留底不抹） */
    @TableField("voided_seqs")
    @ApiModelProperty(value = "被作废旧话的卷内序号")
    private String voidedSeqs;

    /** 本笔记事 */
    @TableField("content")
    @ApiModelProperty(value = "本笔记事")
    private String content;

    /** 递交人（系统落下，不认补记） */
    @TableField("operator")
    @ApiModelProperty(value = "递交人")
    private String operator;

    /** 递交时刻（系统落下，不认补记） */
    @TableField("operate_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "递交时刻")
    private Date operateTime;

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

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
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

    public String getSignRole() {
        return signRole;
    }

    public void setSignRole(String signRole) {
        this.signRole = signRole;
    }

    public Integer getSignRound() {
        return signRound;
    }

    public void setSignRound(Integer signRound) {
        this.signRound = signRound;
    }

    public Integer getFromStage() {
        return fromStage;
    }

    public void setFromStage(Integer fromStage) {
        this.fromStage = fromStage;
    }

    public Integer getToStage() {
        return toStage;
    }

    public void setToStage(Integer toStage) {
        this.toStage = toStage;
    }

    public String getVoidedSeqs() {
        return voidedSeqs;
    }

    public void setVoidedSeqs(String voidedSeqs) {
        this.voidedSeqs = voidedSeqs;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
