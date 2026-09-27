package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TMetpFixCard;
import com.fc.v2.model.auto.TMetpFixLog;
import com.fc.v2.model.custom.FixClueItem;

/**
 * 探测环境违规整改事务卡 Service接口
 * 进门只此「受理」一扇；进一步退一步的章程都搁在本层；
 * 这一单办成什么局面，由查处经过一笔一笔推出来才算数；立单、存单两扇门焊严。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITMetpFixCardService {

    // ---------------- 存单门（只读） ----------------

    /** 按主键查询卷宗 */
    TMetpFixCard selectTMetpFixCardById(Long id);

    /** 流转台账列表查询 */
    List<TMetpFixCard> selectTMetpFixCardList(QueryWrapper<TMetpFixCard> queryWrapper);

    /**
     * 查处经过：asc=true 正着列一遍，asc=false 倒着列一遍。
     * 含被退回作废的留底旧话；每一笔都与当日办理时屏上所摆的一致。
     */
    List<TMetpFixLog> selectTrail(Long cardId, boolean asc);

    /** 卷面当前各段在册记录：被退回作废的旧话不摆，只摆重做后那一条 */
    List<TMetpFixLog> selectLiveLogs(Long cardId);

    // ---------------- 立单门（办理） ----------------

    /**
     * 受理（唯一进门）：每月初从违法线索清单转进来一批。
     * 同一座建筑物名下已有一卷没走到终点的，不再另开第二卷。
     *
     * @return 本次新开的卷数
     */
    int intake(List<FixClueItem> clues, String operator);

    /** 调查段递交：落下技术意见（水平距离/遮挡方位角/高出平台），一进告知段即冻结 */
    TMetpFixCard submitInvestigate(Long id, BigDecimal horizontalDist, BigDecimal blockedAzimuth,
                                   BigDecimal excessHeight, String content, String operator);

    /** 告知段递交：落下拆除高度；技术意见只许照着调查段的取，中途改不得 */
    TMetpFixCard submitNotify(Long id, BigDecimal demolishHeight, String content, String operator);

    /** 处理决定段递交 */
    TMetpFixCard submitDecide(Long id, String decisionText, String content, String operator);

    /**
     * 复查点头：role=OFFICER 县局执法承办人 / CHIEF 台站长，各点头一次，少一次不算复查过；
     * 同人不得充两角；拆除高度与调查记录对不上时签不得，须先退回。
     */
    TMetpFixCard reviewSign(Long id, String role, String operator);

    /**
     * 退回上一段：复查时看出告知段写的拆除高度与调查记录对不上，退回告知段重做。
     * 退回去之前那一段的旧话作废留底，页面只摆重做后那一条。
     */
    TMetpFixCard returnToNotify(Long id, String reason, String operator);

    /** 结案段递交：结案即死卷，不许再往上挂记事 */
    TMetpFixCard submitClose(Long id, String content, String operator);

    /** 抽掉整卷：结案死卷一概不许；在办卷仅许软删，查处经过留底不动 */
    boolean remove(Long id);
}
