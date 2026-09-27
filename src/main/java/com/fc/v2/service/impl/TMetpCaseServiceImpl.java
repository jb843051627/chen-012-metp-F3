package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.CaseFlowException;
import com.fc.v2.mapper.auto.TMetpCaseActiveMapper;
import com.fc.v2.mapper.auto.TMetpCaseMapper;
import com.fc.v2.mapper.auto.TMetpCaseRecordMapper;
import com.fc.v2.model.auto.TMetpCase;
import com.fc.v2.model.auto.TMetpCaseActive;
import com.fc.v2.model.auto.TMetpCaseRecord;
import com.fc.v2.model.custom.CaseAcceptDto;
import com.fc.v2.model.custom.CaseBatchVo;
import com.fc.v2.model.custom.CaseDetailVo;
import com.fc.v2.model.custom.CaseSubmitDto;
import com.fc.v2.service.ITMetpCaseService;
import com.fc.v2.shiro.util.ShiroUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 探测环境违法建设查处案卷 Service 实现。
 *
 * 章程（只此一处立规矩，控制器不掺业务判断）：
 * 1. 进门只此「受理」一扇：线索转入才开卷立号，同一建筑物名下有未结案卷的不开第二卷；
 * 2. 六段顺序办理，不许跳段；卷停在哪一段，就只收哪一段的动作；
 * 3. 同一段递到第二遍不认（只头一次算数）；办理人与时刻由系统自落，接口带的一概不认；
 * 4. 调查段技术意见一进告知段即冻结，后面各段只照录不改；
 * 5. 复查段须执法承办人(OFFICER)与台站长(CHIEF)各点头一次，少一次不算复查过；
 * 6. 复查见告知拆除高度与调查记录对不上，退回告知重做；旧话作废留底不抹，页面只摆新话；
 * 7. 结案即死卷：不再挂记事、不抽卷、不抹任何一笔；省台重查另立新号从受理走；
 * 8. 查处经过只许 INSERT，倒着列每一笔与当日屏上所摆一致。
 *
 * @author fuce
 * @date 2026-09-27
 */
@Service
public class TMetpCaseServiceImpl implements ITMetpCaseService {

    // 六段次序
    public static final int STAGE_ACCEPT = 1;
    public static final int STAGE_INVESTIGATE = 2;
    public static final int STAGE_NOTICE = 3;
    public static final int STAGE_DECISION = 4;
    public static final int STAGE_REVIEW = 5;
    public static final int STAGE_CLOSE = 6;

    public static final String[] STAGE_NAMES = {"", "受理", "调查", "告知", "处理决定", "复查", "结案"};

    // 经过动作
    private static final String ACT_SUBMIT = "SUBMIT";
    private static final String ACT_SIGN = "SIGN";
    private static final String ACT_RETURN = "RETURN";
    private static final String ACT_CLOSE = "CLOSE";

    // 复查点头角色
    public static final String SIGN_OFFICER = "OFFICER";
    public static final String SIGN_CHIEF = "CHIEF";

    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_CLOSED = 1;

    @Resource
    private TMetpCaseMapper caseMapper;
    @Resource
    private TMetpCaseRecordMapper recordMapper;
    @Resource
    private TMetpCaseActiveMapper activeMapper;

    // ============================ 查询 ============================

    @Override
    public List<TMetpCase> selectCaseList(QueryWrapper<TMetpCase> queryWrapper) {
        List<TMetpCase> list = caseMapper.selectList(queryWrapper);
        for (TMetpCase c : list) {
            fillDisplay(c);
        }
        return list;
    }

    @Override
    public TMetpCase selectCaseById(Long id) {
        TMetpCase c = caseMapper.selectById(id);
        if (c != null) {
            fillDisplay(c);
        }
        return c;
    }

    @Override
    public CaseDetailVo caseDetail(Long id) {
        TMetpCase c = caseMapper.selectById(id);
        if (c == null) {
            throw new CaseFlowException("案卷不存在");
        }
        fillDisplay(c);

        List<TMetpCaseRecord> all = recordMapper.selectList(new QueryWrapper<TMetpCaseRecord>()
                .eq("case_id", id).orderByAsc("seq", "id"));
        List<TMetpCaseRecord> desc = new ArrayList<>(all);
        Collections.reverse(desc);
        List<TMetpCaseRecord> current = new ArrayList<>();
        for (TMetpCaseRecord r : all) {
            if (r.getCurrentFlag() != null && r.getCurrentFlag() == 1) {
                current.add(r);
            }
        }

        CaseDetailVo vo = new CaseDetailVo();
        vo.setCaseInfo(c);
        vo.setLedgerAsc(all);
        vo.setLedgerDesc(desc);
        vo.setCurrentRecords(current);
        return vo;
    }

    /** 卷面上随时看得见：此刻停在哪一段、上一段是谁办的。 */
    private void fillDisplay(TMetpCase c) {
        c.setStageName(STAGE_NAMES[c.getStage()]);
        if (c.getStage() != null && c.getStage() == STAGE_NOTICE) {
            c.setNoticeRedo(hasVoidedNotice(c.getId()));
        }
        if (c.getStage() != null && c.getStage() > STAGE_ACCEPT) {
            TMetpCaseRecord prev = recordMapper.selectOne(new QueryWrapper<TMetpCaseRecord>()
                    .eq("case_id", c.getId())
                    .lt("stage", c.getStage())
                    .orderByDesc("seq", "id")
                    .last("limit 1"));
            if (prev != null) {
                c.setPrevOperator(prev.getOperator());
            }
        }
    }

    // ============================ 受理（唯一进门） ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpCase accept(CaseAcceptDto dto) {
        if (dto == null || isBlank(dto.getBuildingName())) {
            throw new CaseFlowException("违法建筑物名称不能为空，无从建卷");
        }
        String building = dto.getBuildingName().trim();

        // 同一建筑物名下已有一卷没走到终点（结案）的，不再另开第二卷。
        TMetpCaseActive exist = activeMapper.selectOne(new QueryWrapper<TMetpCaseActive>()
                .eq("building_name", building).last("limit 1"));
        if (exist != null) {
            throw new CaseFlowException("建筑物「" + building + "」名下已有在办案卷 "
                    + exist.getCaseNo() + "，不再另开第二卷");
        }

        TMetpCase c = new TMetpCase();
        c.setCaseNo(nextCaseNo());
        c.setClueNo(isBlank(dto.getClueNo()) ? null : dto.getClueNo().trim());
        c.setBuildingName(building);
        c.setStage(STAGE_ACCEPT);
        c.setStatus(STATUS_RUNNING);
        c.setTechFrozen(0);
        c.setReviewOfficerSign(0);
        c.setReviewChiefSign(0);
        c.setDelFlag(0);
        c.setAcceptTime(new Date());
        caseMapper.insert(c);

        // 门禁行先立住：唯一键兜底并发，撞键即名下有在办卷。
        TMetpCaseActive gate = new TMetpCaseActive();
        gate.setBuildingName(building);
        gate.setCaseId(c.getId());
        gate.setCaseNo(c.getCaseNo());
        try {
            activeMapper.insert(gate);
        } catch (DuplicateKeyException e) {
            throw new CaseFlowException("建筑物「" + building + "」名下已有在办案卷，不再另开第二卷");
        }

        append(c, STAGE_ACCEPT, ACT_SUBMIT, null, null, dto.getContent());
        // 受理卷成，开门进入调查段。
        c.setStage(STAGE_INVESTIGATE);
        caseMapper.updateById(c);
        return selectCaseById(c.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaseBatchVo acceptBatch(List<CaseAcceptDto> list) {
        CaseBatchVo vo = new CaseBatchVo();
        if (list == null) {
            return vo;
        }
        vo.setTotal(list.size());
        for (CaseAcceptDto dto : list) {
            try {
                TMetpCase c = accept(dto);
                vo.setOpened(vo.getOpened() + 1);
                vo.getCaseNos().add(c.getCaseNo());
            } catch (CaseFlowException e) {
                // 名下有未结案卷的那几件跳过，不影响其余线索转入。
                vo.getSkipped().add(dto == null ? "(空件)" : dto.getBuildingName());
            }
        }
        return vo;
    }

    // ============================ 逐段递交 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpCase submit(CaseSubmitDto dto) {
        if (dto == null || dto.getId() == null) {
            throw new CaseFlowException("未指明案卷");
        }
        TMetpCase c = lockCase(dto.getId());
        int stage = c.getStage();

        if (stage == STAGE_ACCEPT) {
            throw new CaseFlowException("受理段只由线索转入进门，不在此递");
        }
        if (stage == STAGE_REVIEW) {
            throw new CaseFlowException("复查段不递交，须由执法承办人与台站长各自点头");
        }
        if (stage == STAGE_CLOSE) {
            throw new CaseFlowException("案卷已结案（死卷），不得再挂记事");
        }
        if (stage == STAGE_NOTICE && hasVoidedNotice(c.getId())) {
            throw new CaseFlowException("告知段已退回重做，请走重做入口，原递交不作数");
        }
        if (hasCurrentRecord(c.getId(), stage)) {
            throw new CaseFlowException(STAGE_NAMES[stage] + "段头一次递交已算数，同段第二遍不认");
        }

        if (stage == STAGE_INVESTIGATE) {
            submitInvestigate(c, dto);
        } else if (stage == STAGE_NOTICE) {
            submitNotice(c, dto);
        } else if (stage == STAGE_DECISION) {
            submitDecision(c, dto);
        }
        caseMapper.updateById(c);
        return selectCaseById(c.getId());
    }

    /** 调查段：技术意见三件落卷，一进告知段便冻结。 */
    private void submitInvestigate(TMetpCase c, CaseSubmitDto dto) {
        if (dto.getTechDistance() == null || dto.getTechDistance().signum() <= 0
                || isBlank(dto.getTechAzimuth())
                || dto.getTechHeight() == null) {
            throw new CaseFlowException("调查技术意见不全：水平距离、所挡方位角、高出平台米数缺一不可");
        }
        c.setTechDistance(dto.getTechDistance());
        c.setTechAzimuth(dto.getTechAzimuth().trim());
        c.setTechHeight(dto.getTechHeight());

        append(c, STAGE_INVESTIGATE, ACT_SUBMIT, null, dto, dto.getContent());

        c.setTechFrozen(1);
        c.setStage(STAGE_NOTICE);
    }

    /** 告知段：载明应拆除高度；过此口后技术意见只许照录。 */
    private void submitNotice(TMetpCase c, CaseSubmitDto dto) {
        if (dto.getDemolishHeight() == null || dto.getDemolishHeight().signum() <= 0) {
            throw new CaseFlowException("告知段须载明应拆除高度（米）");
        }
        c.setDemolishHeight(dto.getDemolishHeight());
        append(c, STAGE_NOTICE, ACT_SUBMIT, null, dto, dto.getContent());
        c.setStage(STAGE_DECISION);
    }

    /** 处理决定段：技术意见照录冻结值，中途改不得。 */
    private void submitDecision(TMetpCase c, CaseSubmitDto dto) {
        if (isBlank(dto.getContent())) {
            throw new CaseFlowException("处理决定段须写明决定内容");
        }
        append(c, STAGE_DECISION, ACT_SUBMIT, null, dto, dto.getContent());
        // 新入复查，两个点头都清零另候。
        c.setReviewOfficerSign(0);
        c.setReviewChiefSign(0);
        c.setStage(STAGE_REVIEW);
    }

    // ============================ 复查：两人各点头一次 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpCase reviewSign(Long id, String signRole) {
        TMetpCase c = lockCase(id);
        if (c.getStage() != STAGE_REVIEW) {
            throw new CaseFlowException("卷未停在复查段，掀不动复查点头（事再急也不许撇开中间各段）");
        }
        if (!SIGN_OFFICER.equals(signRole) && !SIGN_CHIEF.equals(signRole)) {
            throw new CaseFlowException("点头角色不合法");
        }
        // 同一回复查里，同一角色头一次算数，第二遍不认。
        if (SIGN_OFFICER.equals(signRole)) {
            if (c.getReviewOfficerSign() == 1) {
                throw new CaseFlowException("执法承办人已点过头，同一点头第二遍不认");
            }
            c.setReviewOfficerSign(1);
        } else {
            if (c.getReviewChiefSign() == 1) {
                throw new CaseFlowException("台站长已点过头，同一点头第二遍不认");
            }
            c.setReviewChiefSign(1);
        }
        append(c, STAGE_REVIEW, ACT_SIGN, signRole, null, null);
        caseMapper.updateById(c);
        return selectCaseById(c.getId());
    }

    // ============================ 退回告知重做（另一码事） ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpCase returnToNotice(Long id) {
        TMetpCase c = lockCase(id);
        if (c.getStage() != STAGE_REVIEW) {
            throw new CaseFlowException("只在复查段看得出入时才许退回");
        }
        if (c.getDemolishHeight() == null || c.getTechHeight() == null
                || c.getDemolishHeight().compareTo(c.getTechHeight()) == 0) {
            throw new CaseFlowException("告知拆除高度与调查记录并无出入，不得退回");
        }

        // 旧告知话作废留底：行不抹，只翻面；页面只摆重做后的一条。
        TMetpCaseRecord oldNotice = currentRecord(c.getId(), STAGE_NOTICE);
        if (oldNotice != null) {
            oldNotice.setCurrentFlag(0);
            oldNotice.setVoided(1);
            recordMapper.updateById(oldNotice);
        }
        // 本回复查的旧签名原封留底，另起一回重载。
        List<TMetpCaseRecord> signs = recordMapper.selectList(new QueryWrapper<TMetpCaseRecord>()
                .eq("case_id", id).eq("stage", STAGE_REVIEW).eq("action", ACT_SIGN)
                .eq("current_flag", 1));
        for (TMetpCaseRecord s : signs) {
            s.setCurrentFlag(0);
            s.setVoided(1);
            recordMapper.updateById(s);
        }

        append(c, STAGE_REVIEW, ACT_RETURN, null, null,
                "复查发现告知拆除高度 " + c.getDemolishHeight()
                        + " 米与调查记录高出平台 " + c.getTechHeight() + " 米对不上，退回告知段重做");

        // updateById 默认不带 null 列，拆除高度须显式置空，与作废的旧告知话彻底分开。
        c.setDemolishHeight(null);
        c.setReviewOfficerSign(0);
        c.setReviewChiefSign(0);
        c.setStage(STAGE_NOTICE);
        caseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TMetpCase>()
                .eq("id", c.getId())
                .set("stage", STAGE_NOTICE)
                .set("demolish_height", null)
                .set("review_officer_sign", 0)
                .set("review_chief_sign", 0));
        return selectCaseById(c.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpCase redoNotice(CaseSubmitDto dto) {
        if (dto == null || dto.getId() == null) {
            throw new CaseFlowException("未指明案卷");
        }
        TMetpCase c = lockCase(dto.getId());
        if (c.getStage() != STAGE_NOTICE || !hasVoidedNotice(c.getId())) {
            throw new CaseFlowException("只有被复查退回的告知段才走重做");
        }
        if (dto.getDemolishHeight() == null || dto.getDemolishHeight().signum() <= 0) {
            throw new CaseFlowException("重做告知仍须载明应拆除高度（米）");
        }
        // 技术意见冻结值照录，重做只改告知自己的拆除高度。
        c.setDemolishHeight(dto.getDemolishHeight());
        append(c, STAGE_NOTICE, ACT_SUBMIT, null, dto, dto.getContent());
        // 重做之后另起一回回复查，旧签名已留底、新点头从头候。
        c.setReviewOfficerSign(0);
        c.setReviewChiefSign(0);
        c.setStage(STAGE_REVIEW);
        caseMapper.updateById(c);
        return selectCaseById(c.getId());
    }

    // ============================ 结案即死卷 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMetpCase close(Long id) {
        TMetpCase c = lockCase(id);
        if (c.getStage() != STAGE_REVIEW) {
            throw new CaseFlowException("卷未走到复查段，结不了案（不许撇开中间各段）");
        }
        if (c.getReviewOfficerSign() != 1 || c.getReviewChiefSign() != 1) {
            throw new CaseFlowException("复查须执法承办人与台站长各点头一次，少一次不算复查过，不能结案");
        }
        append(c, STAGE_CLOSE, ACT_CLOSE, null, null, "结案成卷");
        c.setStage(STAGE_CLOSE);
        c.setStatus(STATUS_CLOSED);
        c.setCloseTime(new Date());
        caseMapper.updateById(c);
        // 门禁摘除：此后省台要查是另起一号从受理重走，旧卷原样存着。
        activeMapper.delete(new QueryWrapper<TMetpCaseActive>().eq("case_id", id));
        return selectCaseById(c.getId());
    }

    // ============================ 内部章法 ============================

    /** 行锁取卷并挡住死卷；流转每一笔都先过这道门。 */
    private TMetpCase lockCase(Long id) {
        TMetpCase c = caseMapper.selectByIdForUpdate(id);
        if (c == null) {
            throw new CaseFlowException("案卷不存在");
        }
        if (c.getStatus() != null && c.getStatus() == STATUS_CLOSED) {
            throw new CaseFlowException("案卷已结案，是死卷：不许再挂记事");
        }
        return c;
    }

    /** 该段当前生效记载是否已存在（存在即头一次已算数）。 */
    private boolean hasCurrentRecord(Long caseId, int stage) {
        return currentRecord(caseId, stage) != null;
    }

    private TMetpCaseRecord currentRecord(Long caseId, int stage) {
        List<TMetpCaseRecord> list = recordMapper.selectList(new QueryWrapper<TMetpCaseRecord>()
                .eq("case_id", caseId).eq("stage", stage).eq("current_flag", 1)
                .orderByDesc("seq", "id").last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 是否存在被复查退回、留底作废的告知旧话。 */
    private boolean hasVoidedNotice(Long caseId) {
        Integer cnt = recordMapper.selectCount(new QueryWrapper<TMetpCaseRecord>()
                .eq("case_id", caseId).eq("stage", STAGE_NOTICE).eq("voided", 1));
        return cnt != null && cnt > 0;
    }

    /**
     * 查处经过追加一笔。谁递的、几点几分递的由系统自落：
     * 办理人取登录人、时刻取库外当前时刻，接口传什么都不认、也没有事后补记的门。
     * 卷行已在事务里锁住，seq 串行不重不乱。
     */
    private void append(TMetpCase c, int stage, String action, String signRole,
                        CaseSubmitDto dto, String content) {
        Integer maxSeq = recordMapper.selectObjs(new QueryWrapper<TMetpCaseRecord>()
                .select("COALESCE(MAX(seq),0) AS m").eq("case_id", c.getId())
                .last("limit 1"))
                .stream().findFirst().map(o -> Integer.valueOf(o.toString())).orElse(0);

        TMetpCaseRecord r = new TMetpCaseRecord();
        r.setCaseId(c.getId());
        r.setCaseNo(c.getCaseNo());
        r.setSeq(maxSeq + 1);
        r.setStage(stage);
        r.setAction(action);
        // 退回是事件，不是该段的当前记载：留账但不摆上当前台面，避免与重做后新一回点头并列。
        r.setCurrentFlag(ACT_RETURN.equals(action) ? 0 : 1);
        r.setVoided(0);
        r.setSignRole(signRole);
        r.setOperator(ShiroUtils.getLoginName());
        r.setOperateTime(new Date());
        // 技术意见快照：进告知后取主表冻结值，dto 带什么都不覆盖。
        r.setTechDistance(c.getTechDistance());
        r.setTechAzimuth(c.getTechAzimuth());
        r.setTechHeight(c.getTechHeight());
        r.setDemolishHeight(c.getDemolishHeight());
        r.setContent(isBlank(content) ? null : content.trim());
        recordMapper.insert(r);
    }

    /** 卷号：CC + 年月日 + 当日四位流水；case_no 唯一键兜底，撞号重取。 */
    private String nextCaseNo() {
        String day = new SimpleDateFormat("yyyyMMdd").format(new Date());
        for (int attempt = 0; attempt < 5; attempt++) {
            String prefix = "CC" + day;
            TMetpCase last = caseMapper.selectOne(new QueryWrapper<TMetpCase>()
                    .likeRight("case_no", prefix).orderByDesc("case_no").last("limit 1"));
            int seq = 1;
            if (last != null && last.getCaseNo() != null && last.getCaseNo().length() >= prefix.length() + 4) {
                try {
                    seq = Integer.parseInt(last.getCaseNo().substring(prefix.length())) + 1;
                } catch (NumberFormatException ignore) {
                    seq = 1;
                }
            }
            String no = prefix + String.format("%04d", seq);
            if (caseMapper.selectCount(new QueryWrapper<TMetpCase>().eq("case_no", no)) == 0) {
                return no;
            }
        }
        throw new CaseFlowException("卷号繁忙，请重试");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
