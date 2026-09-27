package com.fc.v2.metp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.ISqlSegment;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fc.v2.common.exception.FixFlowException;
import com.fc.v2.mapper.auto.TMetpFixCardMapper;
import com.fc.v2.mapper.auto.TMetpFixLogMapper;
import com.fc.v2.model.auto.TMetpFixCard;
import com.fc.v2.model.auto.TMetpFixLog;
import com.fc.v2.model.custom.FixClueItem;
import com.fc.v2.service.impl.TMetpFixCardServiceImpl;

/**
 * 查处卷宗六段流转章程测试。
 * 用与 MySQL 行为一致的条件更新假数据层，验证：
 * 一段一段往前挪不许跳着办；同一段递到第二遍只有头一次算数；
 * 技术意见进告知段即冻结；复查两人各点头一次；结案即死卷；
 * 退回作废旧话留底、页面只摆重做后那一条；查处经过正倒列一致。
 */
public class TMetpFixCardFlowTest {

    private FakeCardMapper cardMapper;
    private FakeLogMapper logMapper;
    private TMetpFixCardServiceImpl service;

    @BeforeEach
    void setUp() {
        cardMapper = new FakeCardMapper();
        logMapper = new FakeLogMapper();
        service = new TMetpFixCardServiceImpl();
        ReflectionTestUtils.setField(service, "metpFixCardMapper", cardMapper);
        ReflectionTestUtils.setField(service, "metpFixLogMapper", logMapper);
    }

    // ------------------------------------------------------------------
    // 受理（唯一进门）
    // ------------------------------------------------------------------

    @Test
    void intakeCreatesOpenCardStoppedAtInvestigate() {
        int opened = service.intake(
                Collections.singletonList(new FixClueItem("城西通信基站", "CLUE-001", "观测场西侧三百米")), "zhangsan");
        assertEquals(1, opened);

        TMetpFixCard card = onlyCard();
        assertNotNull(card.getBizNo());
        assertEquals(Integer.valueOf(1), card.getStage(), "受理即建卷，受理段已办完，卷停在调查段");
        assertEquals(Integer.valueOf(0), card.getStatus(), "在办");
        assertEquals("城西通信基站", card.getOpenKey());

        assertEquals(1, logMapper.rows.size(), "受理是查处经过头一笔");
        TMetpFixLog accept = logMapper.rows.get(0);
        assertEquals(Integer.valueOf(1), accept.getSeq());
        assertEquals(Integer.valueOf(0), accept.getStage());
        assertEquals("ACCEPT", accept.getAction());
        assertEquals("zhangsan", accept.getOperator(), "谁递的由系统落下");
        assertNotNull(accept.getOperateTime(), "几点几分递的由系统落下");
    }

    @Test
    void intakeNeverOpensSecondFileForSameBuildingWhileOpen() {
        List<FixClueItem> batch = new ArrayList<FixClueItem>();
        batch.add(new FixClueItem("城西通信基站", "CLUE-001", null));
        batch.add(new FixClueItem("城西通信基站", "CLUE-002", null));
        batch.add(new FixClueItem("城北铁塔", "CLUE-003", null));
        assertEquals(2, service.intake(batch, "zhangsan"), "同批同座建筑物只开一卷");

        // 下月再转：两座都已有未结卷，不再另开第二卷
        List<FixClueItem> nextMonth = new ArrayList<FixClueItem>();
        nextMonth.add(new FixClueItem("城西通信基站", "CLUE-101", null));
        nextMonth.add(new FixClueItem("城北铁塔", "CLUE-102", null));
        assertEquals(0, service.intake(nextMonth, "zhangsan"));
        assertEquals(2, cardMapper.rows.size());
    }

    @Test
    void provinceReinvestigationOpensNewNumberAfterClosed() {
        Long id = intakeOne("城西通信基站");
        driveToClose(id);
        TMetpFixCard closed = card(id);
        String oldBizNo = closed.getBizNo();
        Integer oldStage = closed.getStage();
        int oldLogCount = logMapper.rows.size();

        // 省台要重查，另起一号从受理重新走，旧卷原样存着
        assertEquals(1, service.intake(
                Collections.singletonList(new FixClueItem("城西通信基站", "PROV-RE-1", "省台重查")), "zhangsan"));
        assertEquals(2, cardMapper.rows.size());
        TMetpFixCard reopened = cardMapper.rows.get(1);
        assertFalse(reopened.getBizNo().equals(oldBizNo), "一卷一号，新卷新号");
        assertEquals(Integer.valueOf(1), reopened.getStage());
        assertEquals(Integer.valueOf(0), reopened.getStatus());

        TMetpFixCard oldCard = cardMapper.rows.get(0);
        assertEquals(oldBizNo, oldCard.getBizNo());
        assertEquals(oldStage, oldCard.getStage());
        assertEquals(Integer.valueOf(2), oldCard.getStatus(), "旧卷原样存着");
        assertEquals(oldLogCount + 1, logMapper.rows.size(), "只添新卷的受理笔，旧卷经过一笔不动");
    }

    // ------------------------------------------------------------------
    // 六段次序
    // ------------------------------------------------------------------

    @Test
    void fullFlowWalksSixStagesInOrderAndTrailListsBothWays() {
        Long id = intakeOne("城西通信基站");

        TMetpFixCard c = service.submitInvestigate(id, bd("300"), bd("270.5"), bd("12"), "现场测定", "lisi");
        assertEquals(Integer.valueOf(2), c.getStage());
        c = service.submitNotify(id, bd("12"), "责令限期拆除", "wangwu");
        assertEquals(Integer.valueOf(3), c.getStage());
        c = service.submitDecide(id, "限三十日拆除超高部分", null, "wangwu");
        assertEquals(Integer.valueOf(4), c.getStage());
        c = service.reviewSign(id, "OFFICER", "officer1");
        assertEquals(Integer.valueOf(4), c.getStage(), "少一次不算复查过");
        c = service.reviewSign(id, "CHIEF", "chief1");
        assertEquals(Integer.valueOf(5), c.getStage(), "两人各点头一次，卷进结案段");
        c = service.submitClose(id, "整改到位，结案", "zhangsan");
        assertEquals(Integer.valueOf(2), c.getStatus(), "结案即死卷");
        assertNull(c.getOpenKey(), "结案后在办判重键放空");

        // 查处经过：受理、调查、告知、决定、两笔点头、结案，共七笔，序号连贯
        assertEquals(7, logMapper.rows.size());
        for (int i = 0; i < 7; i++) {
            assertEquals(Integer.valueOf(i + 1), logMapper.rows.get(i).getSeq());
        }
        List<TMetpFixLog> asc = service.selectTrail(id, true);
        List<TMetpFixLog> desc = service.selectTrail(id, false);
        assertEquals(7, asc.size());
        assertEquals(7, desc.size());
        for (int i = 0; i < 7; i++) {
            assertSame(asc.get(i), desc.get(6 - i), "倒着看每一笔都与当日办理时屏上所摆的一致");
        }
    }

    @Test
    void cannotSkipStages() {
        Long id = intakeOne("城西通信基站");
        // 卷还停在调查那一段，有人来点复查，页面不让按；服务层一样不让
        FixFlowException e = assertThrows(FixFlowException.class,
                () -> service.reviewSign(id, "OFFICER", "officer1"));
        assertTrue(e.getMessage().contains("调查"), e.getMessage());
        assertThrows(FixFlowException.class, () -> service.submitNotify(id, bd("12"), null, "a"));
        assertThrows(FixFlowException.class, () -> service.submitDecide(id, "决定", null, "a"));
        assertThrows(FixFlowException.class, () -> service.submitClose(id, null, "a"));
        assertThrows(FixFlowException.class, () -> service.returnToNotify(id, "由头", "a"));
        assertEquals(1, logMapper.rows.size(), "只有受理一笔，谁也没抢着落笔");
        assertEquals(Integer.valueOf(1), card(id).getStage());
    }

    @Test
    void duplicateSubmitOnlyFirstCounts() {
        Long id = intakeOne("城西通信基站");
        service.submitInvestigate(id, bd("300"), bd("270"), bd("12"), "头一次", "lisi");
        FixFlowException e = assertThrows(FixFlowException.class,
                () -> service.submitInvestigate(id, bd("999"), bd("1"), bd("1"), "第二遍", "lisi"));
        assertTrue(e.getMessage().contains("头一次"), e.getMessage());
        TMetpFixCard card = card(id);
        assertEquals(bd("300"), card.getHorizontalDist(), "第二遍递的数一概不算");
        assertEquals(2, logMapper.rows.size(), "第二遍不落笔");
    }

    // ------------------------------------------------------------------
    // 技术意见冻结
    // ------------------------------------------------------------------

    @Test
    void techOpinionFrozenOnceIntoNotifyAndCopiedDownstream() {
        Long id = intakeOne("城西通信基站");
        service.submitInvestigate(id, bd("300"), bd("270.5"), bd("12"), "现场测定", "lisi");
        service.submitNotify(id, bd("12"), "告知书", "wangwu");
        service.submitDecide(id, "决定正文", null, "wangwu");

        TMetpFixCard card = card(id);
        assertEquals(bd("300"), card.getHorizontalDist());
        assertEquals(bd("270.5"), card.getBlockedAzimuth());
        assertEquals(bd("12"), card.getExcessHeight());

        // 告知笔、决定笔、点头笔只许照着调查段的取
        for (TMetpFixLog log : logMapper.rows) {
            if ("NOTIFY".equals(log.getAction()) || "DECIDE".equals(log.getAction())) {
                assertEquals(bd("300"), log.getHorizontalDist(), log.getAction() + "快照照取");
                assertEquals(bd("270.5"), log.getBlockedAzimuth());
                assertEquals(bd("12"), log.getExcessHeight());
            }
        }
        // 后续段接口根本不收这三项，卷面三项中途改不得
        service.reviewSign(id, "OFFICER", "officer1");
        assertEquals(bd("300"), card(id).getHorizontalDist());
        assertEquals(bd("270.5"), card(id).getBlockedAzimuth());
        assertEquals(bd("12"), card(id).getExcessHeight());
    }

    @Test
    void investigateRejectsBadNumbers() {
        Long id = intakeOne("城西通信基站");
        assertThrows(FixFlowException.class,
                () -> service.submitInvestigate(id, null, bd("270"), bd("12"), null, "lisi"));
        assertThrows(FixFlowException.class,
                () -> service.submitInvestigate(id, bd("-1"), bd("270"), bd("12"), null, "lisi"));
        assertThrows(FixFlowException.class,
                () -> service.submitInvestigate(id, bd("300"), bd("361"), bd("12"), null, "lisi"));
        assertThrows(FixFlowException.class,
                () -> service.submitInvestigate(id, bd("300"), bd("270"), bd("-0.5"), null, "lisi"));
        assertEquals(Integer.valueOf(1), card(id).getStage(), "数不合格，卷不动");
    }

    // ------------------------------------------------------------------
    // 复查双点头
    // ------------------------------------------------------------------

    @Test
    void reviewNeedsTwoDistinctPeopleEachOnce() {
        Long id = intakeOne("城西通信基站");
        driveToReview(id, bd("12"));

        service.reviewSign(id, "OFFICER", "officer1");
        assertEquals(Integer.valueOf(4), card(id).getStage(), "少一次不算复查过");

        FixFlowException dup = assertThrows(FixFlowException.class,
                () -> service.reviewSign(id, "OFFICER", "officer2"));
        assertTrue(dup.getMessage().contains("头一次"), dup.getMessage());

        FixFlowException samePerson = assertThrows(FixFlowException.class,
                () -> service.reviewSign(id, "CHIEF", "officer1"));
        assertTrue(samePerson.getMessage().contains("两角"), samePerson.getMessage());

        assertThrows(FixFlowException.class, () -> service.reviewSign(id, "MAYOR", "someone"));

        service.reviewSign(id, "CHIEF", "chief1");
        TMetpFixCard card = card(id);
        assertEquals(Integer.valueOf(5), card.getStage());
        assertEquals("officer1", card.getOfficerSignBy());
        assertEquals("chief1", card.getChiefSignBy());
        assertNotNull(card.getOfficerSignTime());
        assertNotNull(card.getChiefSignTime());
    }

    // ------------------------------------------------------------------
    // 退回：旧话作废留底，页面只摆重做后那一条
    // ------------------------------------------------------------------

    @Test
    void mismatchBlocksSignThenReturnVoidsOldWordsAndRedoWalksAgain() {
        Long id = intakeOne("城西通信基站");
        driveToReview(id, bd("10")); // 告知写拆10米，调查记高出12米，对不上

        // 复查看出对不上：签不得，须退回
        FixFlowException blocked = assertThrows(FixFlowException.class,
                () -> service.reviewSign(id, "OFFICER", "officer1"));
        assertTrue(blocked.getMessage().contains("对不上"), blocked.getMessage());

        TMetpFixCard back = service.returnToNotify(id, "拆除高度与调查记录对不上", "reviewer");
        assertEquals(Integer.valueOf(2), back.getStage(), "退回告知段重做");
        assertNull(back.getDemolishHeight());
        assertNull(back.getDecisionText());
        assertNull(back.getOfficerSignBy());
        assertNull(back.getChiefSignBy());
        assertEquals(Integer.valueOf(1), back.getSignRound(), "退一回进一轮");

        // 退回一笔记下来往，被作废的序号挂在这一笔上
        TMetpFixLog returnLog = logMapper.rows.get(logMapper.rows.size() - 1);
        assertEquals("RETURN", returnLog.getAction());
        assertEquals(Integer.valueOf(4), returnLog.getFromStage());
        assertEquals(Integer.valueOf(2), returnLog.getToStage());

        TMetpFixLog oldNotify = logByAction("NOTIFY");
        TMetpFixLog oldDecide = logByAction("DECIDE");
        assertTrue(returnLog.getVoidedSeqs().contains(String.valueOf(oldNotify.getSeq())));
        assertTrue(returnLog.getVoidedSeqs().contains(String.valueOf(oldDecide.getSeq())));

        // 旧话作废留底：原记录原封不动，查处经过里还在
        assertEquals(bd("10"), oldNotify.getDemolishHeight(), "作废旧话原样留底");
        assertTrue(service.selectTrail(id, true).contains(oldNotify));
        assertTrue(service.selectTrail(id, true).contains(oldDecide));

        // 页面只摆重做后那一条：当前在册记录里看不到被作废的旧话
        List<TMetpFixLog> live = service.selectLiveLogs(id);
        assertFalse(live.contains(oldNotify));
        assertFalse(live.contains(oldDecide));

        // 重做：告知改回12米，与调查记录对得上，往后各段重走，不许撇开中间那一段
        service.submitNotify(id, bd("12"), "重作告知", "wangwu");
        assertThrows(FixFlowException.class,
                () -> service.reviewSign(id, "OFFICER", "officer1"), "不许撇开处理决定段");
        service.submitDecide(id, "重作决定", null, "wangwu");
        service.reviewSign(id, "OFFICER", "officer1");
        service.reviewSign(id, "CHIEF", "chief1");
        TMetpFixCard closed = service.submitClose(id, "结案", "zhangsan");
        assertEquals(Integer.valueOf(2), closed.getStatus());

        // 页面只摆重做后那一条：告知笔在册的只有新的一条
        long liveNotify = 0;
        for (TMetpFixLog log : service.selectLiveLogs(id)) {
            if ("NOTIFY".equals(log.getAction())) {
                liveNotify++;
                assertEquals(bd("12"), log.getDemolishHeight());
            }
        }
        assertEquals(1, liveNotify, "两段话同时挂着来复核的人就要问哪一句算数");
    }

    @Test
    void noReturnWhenDemolishMatchesInvestigation() {
        Long id = intakeOne("城西通信基站");
        driveToReview(id, bd("12"));
        FixFlowException e = assertThrows(FixFlowException.class,
                () -> service.returnToNotify(id, "想退就退", "reviewer"));
        assertTrue(e.getMessage().contains("对得上"), e.getMessage());
        assertEquals(Integer.valueOf(4), card(id).getStage());
    }

    // ------------------------------------------------------------------
    // 结案即死卷
    // ------------------------------------------------------------------

    @Test
    void closedFileIsDead() {
        Long id = intakeOne("城西通信基站");
        driveToClose(id);
        int logCount = logMapper.rows.size();

        assertThrows(FixFlowException.class,
                () -> service.submitInvestigate(id, bd("1"), bd("1"), bd("1"), null, "a"));
        assertThrows(FixFlowException.class, () -> service.submitNotify(id, bd("1"), null, "a"));
        assertThrows(FixFlowException.class, () -> service.submitDecide(id, "x", null, "a"));
        assertThrows(FixFlowException.class, () -> service.reviewSign(id, "OFFICER", "a"));
        assertThrows(FixFlowException.class, () -> service.returnToNotify(id, "x", "a"));
        assertThrows(FixFlowException.class, () -> service.submitClose(id, "再结一次", "a"));
        assertFalse(service.remove(id), "结案即死卷，不许抽掉整卷");
        assertEquals(logCount, logMapper.rows.size(), "死卷一笔也添不得");
        assertEquals(Integer.valueOf(2), card(id).getStatus());
    }

    @Test
    void openCardRemoveIsSoftDeleteAndTrailKept() {
        Long id = intakeOne("城西通信基站");
        service.submitInvestigate(id, bd("300"), bd("270"), bd("12"), null, "lisi");
        assertTrue(service.remove(id), "在办卷仅许软删");
        assertEquals(Integer.valueOf(1), card(id).getDelFlag());
        assertEquals(2, logMapper.rows.size(), "查处经过留底不动，不许抹去中间某一段");
        assertThrows(FixFlowException.class,
                () -> service.submitNotify(id, bd("12"), null, "a"), "抽掉的卷按不存在论");
    }

    // ------------------------------------------------------------------
    // 系统落人落时，事后补记一概不认
    // ------------------------------------------------------------------

    @Test
    void operatorAndTimestampAlwaysServerSide() {
        assertThrows(FixFlowException.class, () -> service.intake(
                Collections.singletonList(new FixClueItem("城西通信基站", null, null)), "  "),
                "落不下递交人，此笔不认");
        Long id = intakeOne("城西通信基站");
        assertThrows(FixFlowException.class,
                () -> service.submitInvestigate(id, bd("1"), bd("1"), bd("1"), null, null));
        service.submitInvestigate(id, bd("300"), bd("270"), bd("12"), null, "lisi");
        TMetpFixLog log = logByAction("INVESTIGATE");
        assertEquals("lisi", log.getOperator(), "谁递的以系统落下来的为准");
        assertNotNull(log.getOperateTime(), "几点几分递的由系统自己落下");
    }

    // ------------------------------------------------------------------
    // 驱动与假数据层
    // ------------------------------------------------------------------

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private Long intakeOne(String building) {
        assertEquals(1, service.intake(
                Collections.singletonList(new FixClueItem(building, "CLUE-X", null)), "zhangsan"));
        return cardMapper.rows.get(cardMapper.rows.size() - 1).getId();
    }

    private TMetpFixCard onlyCard() {
        assertEquals(1, cardMapper.rows.size());
        return cardMapper.rows.get(0);
    }

    private TMetpFixCard card(Long id) {
        for (TMetpFixCard c : cardMapper.rows) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        throw new IllegalStateException("no card " + id);
    }

    private TMetpFixLog logByAction(String action) {
        for (TMetpFixLog log : logMapper.rows) {
            if (action.equals(log.getAction())) {
                return log;
            }
        }
        throw new IllegalStateException("no log " + action);
    }

    /** 走到复查段门口：调查三项 300/270/12，告知拆除高度由参数定 */
    private void driveToReview(Long id, BigDecimal demolishHeight) {
        service.submitInvestigate(id, bd("300"), bd("270"), bd("12"), "现场测定", "lisi");
        service.submitNotify(id, demolishHeight, "告知书", "wangwu");
        service.submitDecide(id, "决定正文", null, "wangwu");
    }

    private void driveToClose(Long id) {
        driveToReview(id, bd("12"));
        service.reviewSign(id, "OFFICER", "officer1");
        service.reviewSign(id, "CHIEF", "chief1");
        service.submitClose(id, "结案", "zhangsan");
    }

    // ------------------------------------------------------------------
    // 与 MySQL 行为一致的条件更新假数据层（只实现服务用到的口子）
    // ------------------------------------------------------------------

    private static final Pattern SET_PAT =
            Pattern.compile("^(\\w+)=#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}$");
    private static final Pattern EQ_PAT =
            Pattern.compile("^(\\w+) = #\\{ew\\.paramNameValuePairs\\.(\\w+)\\}$");
    private static final Pattern NE_PAT =
            Pattern.compile("^(\\w+) <> #\\{ew\\.paramNameValuePairs\\.(\\w+)\\}$");
    private static final Pattern ISNULL_PAT =
            Pattern.compile("^(\\w+) IS NULL$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_PAT =
            Pattern.compile("(?i)^(\\w+) (ASC|DESC)$");

    private static String normalSql(AbstractWrapper<?, ?, ?> wrapper) {
        StringBuilder sb = new StringBuilder();
        for (ISqlSegment seg : wrapper.getExpression().getNormal()) {
            sb.append(seg.getSqlSegment()).append(' ');
        }
        return sb.toString().trim();
    }

    private static String orderSql(AbstractWrapper<?, ?, ?> wrapper) {
        StringBuilder sb = new StringBuilder();
        for (ISqlSegment seg : wrapper.getExpression().getOrderBy()) {
            sb.append(seg.getSqlSegment()).append(' ');
        }
        return sb.toString().trim();
    }

    private static String toCamel(String column) {
        StringBuilder sb = new StringBuilder();
        for (String part : column.split("_")) {
            if (sb.length() == 0) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private static Object fieldVal(Object bean, String column) {
        try {
            Field f = bean.getClass().getDeclaredField(toCamel(column));
            f.setAccessible(true);
            return f.get(bean);
        } catch (Exception e) {
            throw new IllegalStateException("no field for column " + column, e);
        }
    }

    private static void setField(Object bean, String column, Object value) {
        try {
            Field f = bean.getClass().getDeclaredField(toCamel(column));
            f.setAccessible(true);
            f.set(bean, value);
        } catch (Exception e) {
            throw new IllegalStateException("no field for column " + column, e);
        }
    }

    private static boolean matches(Object bean, String whereSql, Map<String, Object> params) {
        if (whereSql == null || whereSql.trim().isEmpty()) {
            return true;
        }
        for (String raw : whereSql.split("(?i) AND ")) {
            String tok = raw.trim();
            if (tok.startsWith("(") && tok.endsWith(")")) {
                tok = tok.substring(1, tok.length() - 1).trim();
            }
            if (tok.isEmpty()) {
                continue;
            }
            Matcher eq = EQ_PAT.matcher(tok);
            Matcher ne = NE_PAT.matcher(tok);
            Matcher isNull = ISNULL_PAT.matcher(tok);
            if (eq.matches()) {
                if (!Objects.equals(fieldVal(bean, eq.group(1)), params.get(eq.group(2)))) {
                    return false;
                }
            } else if (ne.matches()) {
                if (Objects.equals(fieldVal(bean, ne.group(1)), params.get(ne.group(2)))) {
                    return false;
                }
            } else if (isNull.matches()) {
                if (fieldVal(bean, isNull.group(1)) != null) {
                    return false;
                }
            } else {
                throw new IllegalStateException("fake cannot parse condition: " + tok);
            }
        }
        return true;
    }

    private static Map<String, Object> parseSets(UpdateWrapper<?> wrapper) {
        Map<String, Object> sets = new LinkedHashMap<String, Object>();
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        for (String part : wrapper.getSqlSet().split(",")) {
            Matcher m = SET_PAT.matcher(part.trim());
            if (!m.matches()) {
                throw new IllegalStateException("fake cannot parse set: " + part);
            }
            sets.put(m.group(1), params.get(m.group(2)));
        }
        return sets;
    }

    @SuppressWarnings("unchecked")
    private static <T> int applyUpdate(List<T> rows, Wrapper<T> wrapper) {
        UpdateWrapper<T> uw = (UpdateWrapper<T>) wrapper;
        Map<String, Object> sets = parseSets(uw);
        String where = normalSql(uw);
        Map<String, Object> params = uw.getParamNameValuePairs();
        int n = 0;
        for (T row : rows) {
            if (matches(row, where, params)) {
                for (Map.Entry<String, Object> e : sets.entrySet()) {
                    setField(row, e.getKey(), e.getValue());
                }
                n++;
            }
        }
        return n;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> applySelect(List<T> rows, Wrapper<T> wrapper) {
        AbstractWrapper<T, ?, ?> qw = (AbstractWrapper<T, ?, ?>) wrapper;
        String where = normalSql(qw);
        String orderCol = null;
        boolean orderAsc = true;
        Matcher om = ORDER_PAT.matcher(orderSql(qw));
        if (om.matches()) {
            orderCol = om.group(1);
            orderAsc = "ASC".equalsIgnoreCase(om.group(2));
        }
        List<T> out = new ArrayList<T>();
        for (T row : rows) {
            if (matches(row, where, qw.getParamNameValuePairs())) {
                out.add(row);
            }
        }
        if (orderCol != null) {
            final String col = orderCol;
            final int dir = orderAsc ? 1 : -1;
            Collections.sort(out, new Comparator<T>() {
                @SuppressWarnings({"rawtypes", "unchecked"})
                public int compare(T a, T b) {
                    return dir * ((Comparable) fieldVal(a, col)).compareTo(fieldVal(b, col));
                }
            });
        }
        return out;
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("fake 不实现此口");
    }

    static class FakeCardMapper implements TMetpFixCardMapper {
        final List<TMetpFixCard> rows = new ArrayList<TMetpFixCard>();
        private long idSeq = 1000;

        @Override
        public int insert(TMetpFixCard entity) {
            if (entity.getId() == null) {
                entity.setId(++idSeq);
            }
            rows.add(entity);
            return 1;
        }

        @Override
        public TMetpFixCard selectById(Serializable id) {
            for (TMetpFixCard c : rows) {
                if (c.getId().longValue() == ((Number) id).longValue()) {
                    return c;
                }
            }
            return null;
        }

        @Override
        public int update(TMetpFixCard entity, Wrapper<TMetpFixCard> updateWrapper) {
            return applyUpdate(rows, updateWrapper);
        }

        @Override
        public Integer selectCount(Wrapper<TMetpFixCard> queryWrapper) {
            return Integer.valueOf(applySelect(rows, queryWrapper).size());
        }

        @Override
        public List<TMetpFixCard> selectList(Wrapper<TMetpFixCard> queryWrapper) {
            return applySelect(rows, queryWrapper);
        }

        @Override
        public int deleteById(Serializable id) { throw unsupported(); }

        @Override
        public int deleteByMap(Map<String, Object> columnMap) { throw unsupported(); }

        @Override
        public int delete(Wrapper<TMetpFixCard> queryWrapper) { throw unsupported(); }

        @Override
        public int deleteBatchIds(Collection<? extends Serializable> idList) { throw unsupported(); }

        @Override
        public int updateById(TMetpFixCard entity) { throw unsupported(); }

        @Override
        public List<TMetpFixCard> selectBatchIds(Collection<? extends Serializable> idList) { throw unsupported(); }

        @Override
        public List<TMetpFixCard> selectByMap(Map<String, Object> columnMap) { throw unsupported(); }

        @Override
        public TMetpFixCard selectOne(Wrapper<TMetpFixCard> queryWrapper) { throw unsupported(); }

        @Override
        public List<Map<String, Object>> selectMaps(Wrapper<TMetpFixCard> queryWrapper) { throw unsupported(); }

        @Override
        public List<Object> selectObjs(Wrapper<TMetpFixCard> queryWrapper) { throw unsupported(); }

        @Override
        public <E extends IPage<TMetpFixCard>> E selectPage(E page, Wrapper<TMetpFixCard> queryWrapper) { throw unsupported(); }

        @Override
        public <E extends IPage<Map<String, Object>>> E selectMapsPage(E page, Wrapper<TMetpFixCard> queryWrapper) { throw unsupported(); }
    }

    static class FakeLogMapper implements TMetpFixLogMapper {
        final List<TMetpFixLog> rows = new ArrayList<TMetpFixLog>();

        @Override
        public int selectMaxSeq(Long cardId) {
            int max = 0;
            for (TMetpFixLog log : rows) {
                if (log.getCardId().equals(cardId) && log.getSeq() != null && log.getSeq() > max) {
                    max = log.getSeq();
                }
            }
            return max;
        }

        @Override
        public int insert(TMetpFixLog entity) {
            rows.add(entity);
            return 1;
        }

        @Override
        public List<TMetpFixLog> selectTrail(Long cardId, String dir) {
            List<TMetpFixLog> out = new ArrayList<TMetpFixLog>();
            for (TMetpFixLog log : rows) {
                if (log.getCardId().equals(cardId)) {
                    out.add(log);
                }
            }
            final int dirSign = "DESC".equalsIgnoreCase(dir) ? -1 : 1;
            Collections.sort(out, new Comparator<TMetpFixLog>() {
                public int compare(TMetpFixLog a, TMetpFixLog b) {
                    return dirSign * a.getSeq().compareTo(b.getSeq());
                }
            });
            return out;
        }
    }
}
