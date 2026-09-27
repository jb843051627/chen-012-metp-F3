package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.exception.FixFlowException;
import com.fc.v2.mapper.auto.TMetpFixCardMapper;
import com.fc.v2.mapper.auto.TMetpFixLogMapper;
import com.fc.v2.model.auto.TMetpFixCard;
import com.fc.v2.model.auto.TMetpFixLog;
import com.fc.v2.model.custom.FixAction;
import com.fc.v2.model.custom.FixClueItem;
import com.fc.v2.model.custom.FixStage;
import com.fc.v2.service.ITMetpFixCardService;
import com.fc.v2.util.SnowflakeIdWorker;

import cn.hutool.core.util.StrUtil;

/**
 * 探测环境违规整改事务卡 Service业务层处理
 * 一段一段往前挪，不许跳着办；同一段递到第二遍，只有头一次算数；
 * 谁递的、几点几分递的，由系统自己落下，事后补记一概不认；
 * 结案即死卷；退回作废旧话留底，页面只摆重做后那一条。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMetpFixCardServiceImpl implements ITMetpFixCardService {

    /** 在办 */
    private static final int STATUS_OPEN = 0;
    /** 已归档（结案即死卷） */
    private static final int STATUS_CLOSED = 2;

    /** 复查点头角色：县局执法承办人 */
    public static final String ROLE_OFFICER = "OFFICER";
    /** 复查点头角色：台站长 */
    public static final String ROLE_CHIEF = "CHIEF";

    private static final SnowflakeIdWorker BIZ_NO_WORKER = new SnowflakeIdWorker(1, 1);

    @javax.annotation.Resource
    private TMetpFixCardMapper metpFixCardMapper;

    @javax.annotation.Resource
    private TMetpFixLogMapper metpFixLogMapper;

    // ---------------- 存单门（只读） ----------------

    @Override
    public TMetpFixCard selectTMetpFixCardById(Long id) {
        return this.metpFixCardMapper.selectById(id);
    }

    @Override
    public List<TMetpFixCard> selectTMetpFixCardList(QueryWrapper<TMetpFixCard> queryWrapper) {
        return this.metpFixCardMapper.selectList(queryWrapper);
    }

    @Override
    public List<TMetpFixLog> selectTrail(Long cardId, boolean asc) {
        return this.metpFixLogMapper.selectTrail(cardId, asc ? "ASC" : "DESC");
    }

    @Override
    public List<TMetpFixLog> selectLiveLogs(Long cardId) {
        List<TMetpFixLog> all = selectTrail(cardId, true);
        Set<Integer> voided = new HashSet<Integer>();
        for (TMetpFixLog log : all) {
            if (FixAction.RETURN.name().equals(log.getAction()) && StrUtil.isNotBlank(log.getVoidedSeqs())) {
                for (String s : log.getVoidedSeqs().split(",")) {
                    if (StrUtil.isNotBlank(s)) {
                        voided.add(Integer.valueOf(s.trim()));
                    }
                }
            }
        }
        List<TMetpFixLog> live = new ArrayList<TMetpFixLog>();
        for (TMetpFixLog log : all) {
            if (!voided.contains(log.getSeq())) {
                live.add(log);
            }
        }
        return live;
    }

    // ---------------- 立单门（办理） ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int intake(List<FixClueItem> clues, String operator) {
        requireOperator(operator);
        if (clues == null || clues.isEmpty()) {
            return 0;
        }
        int opened = 0;
        for (FixClueItem clue : clues) {
            if (clue == null || StrUtil.isBlank(clue.getBuildingName())) {
                continue;
            }
            String building = clue.getBuildingName().trim();
            // 同一座建筑物名下已有一卷没走到终点的，不再另开第二卷
            Integer openCnt = this.metpFixCardMapper.selectCount(new QueryWrapper<TMetpFixCard>()
                    .eq("open_key", building).eq("del_flag", 0));
            if (openCnt != null && openCnt.intValue() > 0) {
                continue;
            }
            TMetpFixCard card = new TMetpFixCard();
            card.setBizNo(newBizNo());
            card.setBuildingName(building);
            card.setClueNo(clue.getClueNo());
            // 受理即建卷：受理段已办完，卷停在调查段
            card.setStage(Integer.valueOf(FixStage.INVESTIGATE.getCode()));
            card.setStatus(Integer.valueOf(STATUS_OPEN));
            card.setSignRound(Integer.valueOf(0));
            card.setOpenKey(building);
            card.setContent(clue.getContent());
            card.setLastAction(FixAction.ACCEPT.getLabel());
            card.setDelFlag(Integer.valueOf(0));
            try {
                this.metpFixCardMapper.insert(card);
            } catch (DuplicateKeyException e) {
                // 在办判重键撞了：并发的另一头先落了卷，本条线索不再另开第二卷
                continue;
            }
            TMetpFixLog log = newLog(card.getId(), 1, FixStage.ACCEPT, FixAction.ACCEPT,
                    clue.getContent(), operator);
            log.setRemark(clue.getClueNo());
            this.metpFixLogMapper.insert(log);
            opened++;
        }
        return opened;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpFixCard submitInvestigate(Long id, BigDecimal horizontalDist, BigDecimal blockedAzimuth,
                                          BigDecimal excessHeight, String content, String operator) {
        requireOperator(operator);
        requireNonNegative(horizontalDist, "与仪器平台的水平距离");
        requireAzimuth(blockedAzimuth);
        requireNonNegative(excessHeight, "高出仪器平台");
        TMetpFixCard card = mustLoad(id);
        Date now = new Date();
        // 条件更新即闸门：此刻停在调查段且在办，才许落笔；同一段递到第二遍，只有头一次算数
        int rows = this.metpFixCardMapper.update(null, new UpdateWrapper<TMetpFixCard>()
                .eq("id", id)
                .eq("stage", FixStage.INVESTIGATE.getCode())
                .eq("status", STATUS_OPEN)
                .eq("del_flag", 0)
                .set("stage", FixStage.NOTIFY.getCode())
                .set("horizontal_dist", horizontalDist)
                .set("blocked_azimuth", blockedAzimuth)
                .set("excess_height", excessHeight)
                .set("content", content)
                .set("last_action", FixAction.INVESTIGATE.getLabel())
                .set("update_by", operator)
                .set("update_time", now));
        if (rows == 0) {
            throw rejected(card, FixStage.INVESTIGATE);
        }
        TMetpFixLog log = newLog(card.getId(), nextSeq(card.getId()), FixStage.INVESTIGATE,
                FixAction.INVESTIGATE, content, operator);
        log.setHorizontalDist(horizontalDist);
        log.setBlockedAzimuth(blockedAzimuth);
        log.setExcessHeight(excessHeight);
        this.metpFixLogMapper.insert(log);
        return mustLoad(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpFixCard submitNotify(Long id, BigDecimal demolishHeight, String content, String operator) {
        requireOperator(operator);
        requireNonNegative(demolishHeight, "拆除高度");
        TMetpFixCard card = mustLoad(id);
        Date now = new Date();
        // 技术意见一进告知段即冻结：本段只许照着调查段的取，三项一概不动
        int rows = this.metpFixCardMapper.update(null, new UpdateWrapper<TMetpFixCard>()
                .eq("id", id)
                .eq("stage", FixStage.NOTIFY.getCode())
                .eq("status", STATUS_OPEN)
                .eq("del_flag", 0)
                .set("stage", FixStage.DECIDE.getCode())
                .set("demolish_height", demolishHeight)
                .set("content", content)
                .set("last_action", FixAction.NOTIFY.getLabel())
                .set("update_by", operator)
                .set("update_time", now));
        if (rows == 0) {
            throw rejected(card, FixStage.NOTIFY);
        }
        TMetpFixLog log = newLog(card.getId(), nextSeq(card.getId()), FixStage.NOTIFY,
                FixAction.NOTIFY, content, operator);
        log.setDemolishHeight(demolishHeight);
        snapshotTech(log, card);
        this.metpFixLogMapper.insert(log);
        return mustLoad(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpFixCard submitDecide(Long id, String decisionText, String content, String operator) {
        requireOperator(operator);
        if (StrUtil.isBlank(decisionText)) {
            throw new FixFlowException("处理决定正文不能为空");
        }
        TMetpFixCard card = mustLoad(id);
        Date now = new Date();
        int rows = this.metpFixCardMapper.update(null, new UpdateWrapper<TMetpFixCard>()
                .eq("id", id)
                .eq("stage", FixStage.DECIDE.getCode())
                .eq("status", STATUS_OPEN)
                .eq("del_flag", 0)
                .set("stage", FixStage.REVIEW.getCode())
                .set("decision_text", decisionText)
                .set("content", content)
                .set("last_action", FixAction.DECIDE.getLabel())
                .set("update_by", operator)
                .set("update_time", now));
        if (rows == 0) {
            throw rejected(card, FixStage.DECIDE);
        }
        TMetpFixLog log = newLog(card.getId(), nextSeq(card.getId()), FixStage.DECIDE,
                FixAction.DECIDE, StrUtil.blankToDefault(content, decisionText), operator);
        snapshotTech(log, card);
        log.setDemolishHeight(card.getDemolishHeight());
        this.metpFixLogMapper.insert(log);
        return mustLoad(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpFixCard reviewSign(Long id, String role, String operator) {
        requireOperator(operator);
        if (!ROLE_OFFICER.equals(role) && !ROLE_CHIEF.equals(role)) {
            throw new FixFlowException("复查点头角色只认县局执法承办人与台站长");
        }
        TMetpFixCard card = mustLoad(id);
        if (card.getStage() != null && card.getStage().intValue() == FixStage.REVIEW.getCode()
                && demolishMismatch(card)) {
            // 复查时看出告知段写的拆除高度与调查记录对不上：签不得，须退回告知段重做
            throw new FixFlowException("告知段拆除高度与调查记录对不上，须退回告知段重做后再点头");
        }
        // 同人不得充两角：执法承办人与台站长须各是一人
        if (ROLE_OFFICER.equals(role) && StrUtil.isNotBlank(card.getChiefSignBy())
                && card.getChiefSignBy().equals(operator)) {
            throw new FixFlowException("执法承办人与台站长须各是一人，不得同人充两角");
        }
        if (ROLE_CHIEF.equals(role) && StrUtil.isNotBlank(card.getOfficerSignBy())
                && card.getOfficerSignBy().equals(operator)) {
            throw new FixFlowException("执法承办人与台站长须各是一人，不得同人充两角");
        }
        Date now = new Date();
        String signField = ROLE_OFFICER.equals(role) ? "officer_sign_by" : "chief_sign_by";
        String signTimeField = ROLE_OFFICER.equals(role) ? "officer_sign_time" : "chief_sign_time";
        int rows = this.metpFixCardMapper.update(null, new UpdateWrapper<TMetpFixCard>()
                .eq("id", id)
                .eq("stage", FixStage.REVIEW.getCode())
                .eq("status", STATUS_OPEN)
                .eq("del_flag", 0)
                .isNull(signField)
                .set(signField, operator)
                .set(signTimeField, now)
                .set("last_action", FixAction.REVIEW_SIGN.getLabel())
                .set("update_by", operator)
                .set("update_time", now));
        if (rows == 0) {
            TMetpFixCard fresh = mustLoad(id);
            if (fresh.getStage() != null && fresh.getStage().intValue() == FixStage.REVIEW.getCode()
                    && fresh.getStatus() != null && fresh.getStatus().intValue() == STATUS_OPEN) {
                throw new FixFlowException("该角色本轮已点过头，同一段递到第二遍只有头一次算数");
            }
            throw rejected(fresh, FixStage.REVIEW);
        }
        TMetpFixLog log = newLog(card.getId(), nextSeq(card.getId()), FixStage.REVIEW,
                FixAction.REVIEW_SIGN, (ROLE_OFFICER.equals(role) ? "县局执法承办人" : "台站长") + "点头", operator);
        log.setSignRole(role);
        log.setSignRound(card.getSignRound());
        snapshotTech(log, card);
        log.setDemolishHeight(card.getDemolishHeight());
        this.metpFixLogMapper.insert(log);
        // 两人各点头一次，少一次不算复查过；点齐即推到结案段
        TMetpFixCard fresh = mustLoad(id);
        if (StrUtil.isNotBlank(fresh.getOfficerSignBy()) && StrUtil.isNotBlank(fresh.getChiefSignBy())) {
            this.metpFixCardMapper.update(null, new UpdateWrapper<TMetpFixCard>()
                    .eq("id", id)
                    .eq("stage", FixStage.REVIEW.getCode())
                    .eq("status", STATUS_OPEN)
                    .eq("del_flag", 0)
                    .set("stage", FixStage.CLOSE.getCode())
                    .set("update_by", operator)
                    .set("update_time", new Date()));
        }
        return mustLoad(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpFixCard returnToNotify(Long id, String reason, String operator) {
        requireOperator(operator);
        TMetpFixCard card = mustLoad(id);
        if (card.getStage() == null || card.getStage().intValue() != FixStage.REVIEW.getCode()) {
            if (card.getStatus() != null && card.getStatus().intValue() == STATUS_CLOSED) {
                throw new FixFlowException("结案即死卷，退也退不动");
            }
            FixStage curStage = FixStage.of(card.getStage());
            throw new FixFlowException("退回只在复查段发起，卷现停在"
                    + (curStage == null ? "未知" : curStage.getLabel()) + "段");
        }
        if (!demolishMismatch(card)) {
            throw new FixFlowException("拆除高度与调查记录对得上，无需退回");
        }
        // 退回去之前那一段的旧话作废留底：告知笔、处理决定笔、本轮复查点头笔一并作废（留底不抹）
        List<TMetpFixLog> live = selectLiveLogs(id);
        List<String> voidedSeqs = new ArrayList<String>();
        for (TMetpFixLog log : live) {
            String action = log.getAction();
            if (FixAction.NOTIFY.name().equals(action) || FixAction.DECIDE.name().equals(action)
                    || FixAction.REVIEW_SIGN.name().equals(action)) {
                voidedSeqs.add(String.valueOf(log.getSeq()));
            }
        }
        Date now = new Date();
        int rows = this.metpFixCardMapper.update(null, new UpdateWrapper<TMetpFixCard>()
                .eq("id", id)
                .eq("stage", FixStage.REVIEW.getCode())
                .eq("status", STATUS_OPEN)
                .eq("del_flag", 0)
                .set("stage", FixStage.NOTIFY.getCode())
                .set("demolish_height", null)
                .set("decision_text", null)
                .set("officer_sign_by", null)
                .set("officer_sign_time", null)
                .set("chief_sign_by", null)
                .set("chief_sign_time", null)
                .set("sign_round", (card.getSignRound() == null ? 0 : card.getSignRound().intValue()) + 1)
                .set("content", reason)
                .set("last_action", FixAction.RETURN.getLabel())
                .set("update_by", operator)
                .set("update_time", now));
        if (rows == 0) {
            throw rejected(mustLoad(id), FixStage.REVIEW);
        }
        TMetpFixLog log = newLog(card.getId(), nextSeq(card.getId()), FixStage.REVIEW,
                FixAction.RETURN, reason, operator);
        log.setFromStage(Integer.valueOf(FixStage.REVIEW.getCode()));
        log.setToStage(Integer.valueOf(FixStage.NOTIFY.getCode()));
        log.setVoidedSeqs(StrUtil.join(",", voidedSeqs));
        snapshotTech(log, card);
        this.metpFixLogMapper.insert(log);
        return mustLoad(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpFixCard submitClose(Long id, String content, String operator) {
        requireOperator(operator);
        TMetpFixCard card = mustLoad(id);
        Date now = new Date();
        // 结案即死卷：落定归档，在办判重键放空，往后一笔也添不得
        int rows = this.metpFixCardMapper.update(null, new UpdateWrapper<TMetpFixCard>()
                .eq("id", id)
                .eq("stage", FixStage.CLOSE.getCode())
                .eq("status", STATUS_OPEN)
                .eq("del_flag", 0)
                .set("status", STATUS_CLOSED)
                .set("open_key", null)
                .set("content", content)
                .set("last_action", FixAction.CLOSE.getLabel())
                .set("update_by", operator)
                .set("update_time", now));
        if (rows == 0) {
            throw rejected(card, FixStage.CLOSE);
        }
        TMetpFixLog log = newLog(card.getId(), nextSeq(card.getId()), FixStage.CLOSE,
                FixAction.CLOSE, content, operator);
        snapshotTech(log, card);
        log.setDemolishHeight(card.getDemolishHeight());
        this.metpFixLogMapper.insert(log);
        return mustLoad(id);
    }

    @Override
    public boolean remove(Long id) {
        TMetpFixCard card = this.metpFixCardMapper.selectById(id);
        if (card == null) {
            return false;
        }
        if (card.getStatus() != null && card.getStatus().intValue() == STATUS_CLOSED) {
            // 结案即死卷，不许抽掉整卷
            return false;
        }
        // 在办卷仅许软删，查处经过留底不动
        return this.metpFixCardMapper.update(null, new UpdateWrapper<TMetpFixCard>()
                .eq("id", id)
                .eq("del_flag", 0)
                .ne("status", STATUS_CLOSED)
                .set("del_flag", 1)
                .set("update_time", new Date())) > 0;
    }

    // ---------------- 本层章程（私有） ----------------

    /** 卷不在或已被抽掉，皆按不存在论 */
    private TMetpFixCard mustLoad(Long id) {
        if (id == null) {
            throw new FixFlowException("卷号不能为空");
        }
        TMetpFixCard card = this.metpFixCardMapper.selectById(id);
        if (card == null || (card.getDelFlag() != null && card.getDelFlag().intValue() == 1)) {
            throw new FixFlowException("卷不存在");
        }
        return card;
    }

    /** 被拒的统一回话：死卷、重复递交、跳段，各说各的话 */
    private FixFlowException rejected(TMetpFixCard card, FixStage attempted) {
        if (card.getStatus() != null && card.getStatus().intValue() == STATUS_CLOSED) {
            return new FixFlowException("结案即死卷，" + attempted.getLabel() + "递不进去");
        }
        Integer cur = card.getStage();
        FixStage curStage = FixStage.of(cur);
        if (cur != null && cur.intValue() > attempted.getCode()) {
            return new FixFlowException(attempted.getLabel() + "段已办完，同一段递到第二遍只有头一次算数");
        }
        return new FixFlowException("卷还停在" + (curStage == null ? "未知" : curStage.getLabel())
                + "段，" + attempted.getLabel() + "不让办");
    }

    /** 拆除高度与调查记录（高出平台）对不上 */
    private boolean demolishMismatch(TMetpFixCard card) {
        if (card.getDemolishHeight() == null || card.getExcessHeight() == null) {
            return true;
        }
        return card.getDemolishHeight().compareTo(card.getExcessHeight()) != 0;
    }

    /** 谁递的、几点几分递的，由系统自己落下 */
    private TMetpFixLog newLog(Long cardId, int seq, FixStage stage, FixAction action,
                               String content, String operator) {
        TMetpFixLog log = new TMetpFixLog();
        log.setId(Long.valueOf(BIZ_NO_WORKER.nextId()));
        log.setCardId(cardId);
        log.setSeq(Integer.valueOf(seq));
        log.setStage(Integer.valueOf(stage.getCode()));
        log.setAction(action.name());
        log.setContent(content);
        log.setOperator(operator);
        log.setOperateTime(new Date());
        return log;
    }

    /** 冻结后的技术意见只许照着取：快照进本笔，不改卷面三项 */
    private void snapshotTech(TMetpFixLog log, TMetpFixCard card) {
        log.setHorizontalDist(card.getHorizontalDist());
        log.setBlockedAzimuth(card.getBlockedAzimuth());
        log.setExcessHeight(card.getExcessHeight());
    }

    private int nextSeq(Long cardId) {
        return this.metpFixLogMapper.selectMaxSeq(cardId) + 1;
    }

    private void requireOperator(String operator) {
        if (StrUtil.isBlank(operator)) {
            throw new FixFlowException("系统未能落下递交人，此笔不认");
        }
    }

    private void requireNonNegative(BigDecimal val, String name) {
        if (val == null || val.compareTo(BigDecimal.ZERO) < 0) {
            throw new FixFlowException(name + "须为不小于零的数");
        }
    }

    private void requireAzimuth(BigDecimal val) {
        if (val == null || val.compareTo(BigDecimal.ZERO) < 0
                || val.compareTo(new BigDecimal("360")) > 0) {
            throw new FixFlowException("遮挡方位角须在0到360度之间");
        }
    }

    /** 一卷一号 */
    private String newBizNo() {
        return "AJ" + new SimpleDateFormat("yyyyMM").format(new Date())
                + "-" + BIZ_NO_WORKER.nextId();
    }
}
