package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMetpCase;
import com.fc.v2.model.auto.TMetpCaseRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 案卷详情：卷头 + 查处经过。
 *
 * @author fuce
 * @date 2026-09-27
 */
@ApiModel(value = "CaseDetailVo", description = "案卷详情与查处经过")
public class CaseDetailVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 案卷卷头 */
    @ApiModelProperty(value = "案卷卷头")
    private TMetpCase caseInfo;

    /** 查处经过（按卷内次序正列） */
    @ApiModelProperty(value = "查处经过（正列）")
    private List<TMetpCaseRecord> ledgerAsc;

    /** 查处经过（倒列，倒着看每一笔与当日屏上所摆一致） */
    @ApiModelProperty(value = "查处经过（倒列）")
    private List<TMetpCaseRecord> ledgerDesc;

    /** 页面摆出的当前有效记载（current_flag=1，按段） */
    @ApiModelProperty(value = "当前有效记载")
    private List<TMetpCaseRecord> currentRecords;

    public TMetpCase getCaseInfo() {
        return caseInfo;
    }

    public void setCaseInfo(TMetpCase caseInfo) {
        this.caseInfo = caseInfo;
    }

    public List<TMetpCaseRecord> getLedgerAsc() {
        return ledgerAsc;
    }

    public void setLedgerAsc(List<TMetpCaseRecord> ledgerAsc) {
        this.ledgerAsc = ledgerAsc;
    }

    public List<TMetpCaseRecord> getLedgerDesc() {
        return ledgerDesc;
    }

    public void setLedgerDesc(List<TMetpCaseRecord> ledgerDesc) {
        this.ledgerDesc = ledgerDesc;
    }

    public List<TMetpCaseRecord> getCurrentRecords() {
        return currentRecords;
    }

    public void setCurrentRecords(List<TMetpCaseRecord> currentRecords) {
        this.currentRecords = currentRecords;
    }
}
