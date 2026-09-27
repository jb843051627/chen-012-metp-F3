package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TMetpCase;
import com.fc.v2.model.custom.CaseAcceptDto;
import com.fc.v2.model.custom.CaseBatchVo;
import com.fc.v2.model.custom.CaseDetailVo;
import com.fc.v2.model.custom.CaseSubmitDto;

import java.util.List;

/**
 * 探测环境违法建设查处案卷 Service（state-machine 形状：六段顺序流转，只开受理一扇门）。
 * 推进、退回、点头、结案的章程全部落在此处，接口回话说了算；不提供普通增删改入口。
 *
 * @author fuce
 * @date 2026-09-27
 */
public interface ITMetpCaseService {

    /** 台账查询 */
    List<TMetpCase> selectCaseList(QueryWrapper<TMetpCase> queryWrapper);

    /** 按主键取卷 */
    TMetpCase selectCaseById(Long id);

    /** 案卷详情：卷头 + 查处经过（正列、倒列、当前摆出的记载） */
    CaseDetailVo caseDetail(Long id);

    /** 受理：违法线索转入，开卷立号。同建筑物名下有未结案卷的不开第二卷。 */
    TMetpCase accept(CaseAcceptDto dto);

    /** 每月初批量转入 */
    CaseBatchVo acceptBatch(List<CaseAcceptDto> list);

    /** 递交当前段并推进下一段；只许在当前段递，同段第二遍不认，不许跳段。 */
    TMetpCase submit(CaseSubmitDto dto);

    /** 复查段点头：OFFICER 执法承办人、CHIEF 台站长各点一次，少一次不算复查过；同人第二遍不认。 */
    TMetpCase reviewSign(Long id, String signRole);

    /**
     * 复查时发现告知段拆除高度与调查记录对不上，退回告知段重做。
     * 旧告知话作废留底（voided=1 不抹），页面只摆重做后的一条。
     */
    TMetpCase returnToNotice(Long id);

    /** 告知段重做递交（仅退回后可办），递交后重回复查，复查点头另起一回。 */
    TMetpCase redoNotice(CaseSubmitDto dto);

    /** 结案即死卷：复查两头点齐方可结案；结案后不得再挂记事、不得抽卷、不得抹记载。 */
    TMetpCase close(Long id);
}
