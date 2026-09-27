package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import com.fc.v2.service.impl.TMetpCaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 查处案卷六段流转章程的服务层自测。
 * 不启 Spring、不落库：三张表用内存 Map 顶替，Wrapper 条件用极简解释器过滤。
 *
 * @author fuce
 * @date 2026-09-27
 */
public class TMetpCaseServiceTest {

    private ITMetpCaseService svc;

    final Map<Long, TMetpCase> cases = new LinkedHashMap<>();
    final Map<Long, TMetpCaseRecord> records = new LinkedHashMap<>();
    final Map<Long, TMetpCaseActive> actives = new LinkedHashMap<>();
    final AtomicLong idSeq = new AtomicLong(1000);

    @BeforeEach
    void setUp() {
        cases.clear();
        records.clear();
        actives.clear();
        TMetpCaseServiceImpl impl = new TMetpCaseServiceImpl();
        ReflectionTestUtils.setField(impl, "caseMapper", fake(TMetpCaseMapper.class, TMetpCase.class, cases));
        ReflectionTestUtils.setField(impl, "recordMapper", fake(TMetpCaseRecordMapper.class, TMetpCaseRecord.class, records));
        ReflectionTestUtils.setField(impl, "activeMapper", fake(TMetpCaseActiveMapper.class, TMetpCaseActive.class, actives));
        svc = impl;
    }

    // ----------------------------- 场景 -----------------------------

    @Test
    void fullFlow_signsFrozenClosed() {
        TMetpCase c = accept("城西通信基站", "XS-1");
        final long id0 = c.getId();
        assertEquals(2, c.getStage(), "受理后停在调查段");
        assertEquals(1, countRecords(id0, 1, "SUBMIT", 0));

        // 技术意见不全，不收
        CaseSubmitDto bad = submit(id0);
        assertThrows(CaseFlowException.class, () -> svc.submit(bad));

        c = svc.submit(investigate(id0, new BigDecimal("300"), "270", new BigDecimal("18.5")));
        assertEquals(3, c.getStage());
        assertEquals(1, c.getTechFrozen(), "一进告知段技术意见冻结");

        c = svc.submit(notice(id0, new BigDecimal("18.5")));
        assertEquals(4, c.getStage());

        // 处理决定段即便另带技术值，也照录冻结值
        CaseSubmitDto decision = submit(id0);
        decision.setContent("责令限期拆除超高部分");
        decision.setTechHeight(new BigDecimal("99")); // 想中途改
        c = svc.submit(decision);
        assertEquals(5, c.getStage());
        TMetpCaseRecord decRec = lastCurrent(id0, 4);
        assertEquals(0, new BigDecimal("18.5").compareTo(decRec.getTechHeight()), "快照照录冻结值");
        assertNotEquals(0, new BigDecimal("99").compareTo(decRec.getTechHeight()));

        // 未点齐不能结案
        svc.reviewSign(id0, "OFFICER");
        assertThrows(CaseFlowException.class, () -> svc.close(id0), "少一次点头不算复查过");
        // 同一人第二遍不认
        assertThrows(CaseFlowException.class, () -> svc.reviewSign(id0, "OFFICER"));
        c = svc.reviewSign(id0, "CHIEF");
        assertEquals(1, c.getReviewOfficerSign());
        assertEquals(1, c.getReviewChiefSign());

        c = svc.close(id0);
        assertEquals(6, c.getStage());
        assertEquals(1, c.getStatus(), "结案即死卷");
        assertNotNull(c.getCloseTime());
        final long closedId = c.getId();
        assertTrue(actives.values().stream().noneMatch(a -> a.getCaseId().equals(closedId)), "结案摘门禁");

        // 死卷不许再记事
        assertThrows(CaseFlowException.class, () -> svc.reviewSign(closedId, "CHIEF"));
        assertThrows(CaseFlowException.class, () -> svc.close(closedId));
        CaseSubmitDto after = submit(closedId);
        after.setContent("x");
        assertThrows(CaseFlowException.class, () -> svc.submit(after));
    }

    @Test
    void noJump_noSkipStages() {
        TMetpCase c = accept("跳段楼", null);
        final long id = c.getId();
        // 停在调查(2)：复查掀不动、结不了案、退不回
        assertThrows(CaseFlowException.class, () -> svc.reviewSign(id, "OFFICER"));
        assertThrows(CaseFlowException.class, () -> svc.close(id));
        assertThrows(CaseFlowException.class, () -> svc.returnToNotice(id));
        // 告知/重做也不许越段
        assertThrows(CaseFlowException.class, () -> svc.redoNotice(notice(id, new BigDecimal("1"))));
    }

    @Test
    void duplicateBuilding_noSecondCase() {
        accept("同一座塔", "XS-2");
        assertThrows(CaseFlowException.class, () -> accept("同一座塔", "XS-9"));
        assertEquals(1, cases.size(), "名下有未结案卷不再另开第二卷");
    }

    @Test
    void monthlyBatch_skipsDupes() {
        CaseBatchVo vo = svc.acceptBatch(Arrays.asList(
                clue("甲楼", "B-1"), clue("乙楼", "B-2"), clue("甲楼", "B-3")));
        assertEquals(3, vo.getTotal());
        assertEquals(2, vo.getOpened());
        assertEquals(1, vo.getSkipped().size());
    }

    @Test
    void returnNotice_oldWordsKept_redoneThenClose() {
        TMetpCase c = accept("退回塔", "R-1");
        final long id = c.getId();
        c = svc.submit(investigate(id, new BigDecimal("300"), "270", new BigDecimal("20")));
        c = svc.submit(notice(id, new BigDecimal("8"))); // 告知只拆 8，与高出 20 对不上
        c = svc.submit(content(id, "决定拆除"));
        svc.reviewSign(id, "OFFICER");
        svc.reviewSign(id, "CHIEF");

        // 对不上才许退；退回到复查段
        TMetpCase returned = svc.returnToNotice(id);
        assertEquals(3, returned.getStage());
        assertNull(returned.getDemolishHeight());
        assertEquals(0, returned.getReviewOfficerSign() + returned.getReviewChiefSign(), "点头另起一回");

        TMetpCaseRecord oldNotice = lastRecord(id, 3);
        assertEquals(1, oldNotice.getVoided(), "旧告知话作废留底");
        assertEquals(0, oldNotice.getCurrentFlag(), "页面不再摆旧话");
        assertEquals(2, records.values().stream().filter(r -> r.getCaseId().equals(id)
                && r.getStage() == 5 && r.getVoided() == 1).count(), "两个旧签名原封留底");
        TMetpCaseRecord retEvent = records.values().stream().filter(r -> r.getCaseId().equals(id)
                && "RETURN".equals(r.getAction())).findFirst().orElse(null);
        assertNotNull(retEvent);
        assertEquals(0, retEvent.getCurrentFlag(), "退回是事件，不占当前记载");

        // 退回到告知段，普通递交门被挡，只能走重做
        assertThrows(CaseFlowException.class,
                () -> svc.submit(notice(id, new BigDecimal("20"))));
        c = svc.redoNotice(notice(id, new BigDecimal("20")));
        assertEquals(5, c.getStage(), "重做后另起一回复查");
        assertEquals(new BigDecimal("20"), c.getDemolishHeight());
        // 页面摆出的告知当前记载只有重做那一条
        assertEquals(1, records.values().stream().filter(r -> r.getCaseId().equals(id)
                && r.getStage() == 3 && r.getCurrentFlag() == 1).count());

        svc.reviewSign(id, "OFFICER");
        svc.reviewSign(id, "CHIEF");
        c = svc.close(id);
        assertEquals(1, c.getStatus());
    }

    @Test
    void returnRejectedWhenHeightsAgree() {
        TMetpCase c = accept("相符楼", "Q-1");
        final long id = c.getId();
        c = svc.submit(investigate(id, new BigDecimal("300"), "270", new BigDecimal("12")));
        c = svc.submit(notice(id, new BigDecimal("12")));
        c = svc.submit(content(id, "决定"));
        assertThrows(CaseFlowException.class, () -> svc.returnToNotice(id), "无出入不得退回");
    }

    @Test
    void ledger_forwardAndReverseMatch() {
        TMetpCase c = accept("台账楼", "L-1");
        final Long id = c.getId();
        c = svc.submit(investigate(id, new BigDecimal("300"), "270", new BigDecimal("20")));
        c = svc.submit(notice(id, new BigDecimal("20")));
        c = svc.submit(content(id, "决定"));
        CaseDetailVo vo = svc.caseDetail(id);
        int n = vo.getLedgerAsc().size();
        assertEquals(4, n, "受理/调查/告知/决定各一笔");
        List<Integer> ascSeq = new ArrayList<>();
        vo.getLedgerAsc().forEach(r -> ascSeq.add(r.getSeq()));
        assertEquals(Arrays.asList(1, 2, 3, 4), ascSeq);
        assertEquals(Integer.valueOf(4), vo.getLedgerDesc().get(0).getSeq(), "倒列首笔即最后一笔");
        // 每一笔办理人与时刻均为系统所落，不缺
        vo.getLedgerAsc().forEach(r -> {
            assertNotNull(r.getOperator());
            assertNotNull(r.getOperateTime());
        });
    }

    @Test
    void reopenAfterClosedIsANewCase() {
        TMetpCase c = accept("结案后再查楼", "N-1");
        final long id = c.getId();
        c = svc.submit(investigate(id, new BigDecimal("300"), "270", new BigDecimal("20")));
        c = svc.submit(notice(id, new BigDecimal("20")));
        c = svc.submit(content(id, "决定"));
        svc.reviewSign(id, "OFFICER");
        svc.reviewSign(id, "CHIEF");
        svc.close(id);
        // 门禁已摘，省台重查另起一号，旧卷原样在
        TMetpCase again = accept("结案后再查楼", "N-2");
        assertEquals(2, cases.size());
        assertNotEquals(c.getCaseNo(), again.getCaseNo());
        TMetpCase old = cases.get(id);
        assertEquals(1, old.getStatus());
    }

    // ----------------------------- 造数帮手 -----------------------------

    private TMetpCase accept(String building, String clue) {
        CaseAcceptDto dto = clue(building, clue);
        return svc.accept(dto);
    }

    private CaseAcceptDto clue(String building, String clue) {
        CaseAcceptDto d = new CaseAcceptDto();
        d.setBuildingName(building);
        d.setClueNo(clue);
        d.setContent("受理：" + building);
        return d;
    }

    private CaseSubmitDto submit(Long id) {
        CaseSubmitDto d = new CaseSubmitDto();
        d.setId(id);
        return d;
    }

    private CaseSubmitDto investigate(Long id, BigDecimal dist, String az, BigDecimal h) {
        CaseSubmitDto d = submit(id);
        d.setTechDistance(dist);
        d.setTechAzimuth(az);
        d.setTechHeight(h);
        d.setContent("现场实测");
        return d;
    }

    private CaseSubmitDto notice(Long id, BigDecimal demolish) {
        CaseSubmitDto d = submit(id);
        d.setDemolishHeight(demolish);
        d.setContent("告知拆除");
        return d;
    }

    private CaseSubmitDto content(Long id, String s) {
        CaseSubmitDto d = submit(id);
        d.setContent(s);
        return d;
    }

    private long countRecords(long caseId, int stage, String action, int voided) {
        return records.values().stream().filter(r -> r.getCaseId().equals(caseId)
                && r.getStage() == stage && action.equals(r.getAction()) && r.getVoided() == voided).count();
    }

    private TMetpCaseRecord lastCurrent(Long caseId, int stage) {
        return records.values().stream().filter(r -> r.getCaseId().equals(caseId)
                && r.getStage() == stage && r.getCurrentFlag() == 1)
                .reduce((a, b) -> b).orElse(null);
    }

    private TMetpCaseRecord lastRecord(Long caseId, int stage) {
        return records.values().stream().filter(r -> r.getCaseId().equals(caseId) && r.getStage() == stage)
                .reduce((a, b) -> b).orElse(null);
    }

    // ----------------------------- 内存假 Mapper -----------------------------

    @SuppressWarnings("unchecked")
    private <M> M fake(Class<M> mapperType, Class<?> entityType, Map<Long, ?> store) {
        InvocationHandler h = new StoreHandler(entityType, store);
        return (M) Proxy.newProxyInstance(mapperType.getClassLoader(), new Class[]{mapperType}, h);
    }

    /** 只实现服务用到的方法；Wrapper 用极简解释器。 */
    class StoreHandler implements InvocationHandler {
        private final Class<?> entityType;
        private final Map<Long, Object> store;

        @SuppressWarnings("unchecked")
        StoreHandler(Class<?> entityType, Map<Long, ?> store) {
            this.entityType = entityType;
            this.store = (Map<Long, Object>) store;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String m = method.getName();
            switch (m) {
                case "insert": {
                    Object e = args[0];
                    Long id = (Long) read(e, "id");
                    if (id == null) {
                        id = idSeq.incrementAndGet();
                        write(e, "id", id);
                    }
                    // 门禁唯一键
                    if (e instanceof TMetpCaseActive) {
                        String bn = (String) read(e, "building_name");
                        boolean dup = store.values().stream()
                                .anyMatch(x -> bn.equals(read(x, "building_name")));
                        if (dup) {
                            throw new DuplicateKeyException("uk_active_building");
                        }
                    }
                    store.put(id, e);
                    return 1;
                }
                case "updateById": {
                    Object e = args[0];
                    Object old = store.get(read(e, "id"));
                    if (old == null) {
                        return 0;
                    }
                    for (java.lang.reflect.Field f : entityType.getDeclaredFields()) {
                        if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                            continue;
                        }
                        com.baomidou.mybatisplus.annotation.TableField tf =
                                f.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);
                        if (tf != null && !tf.exist()) {
                            continue;
                        }
                        String col = columnOf(f);
                        Object v = read(e, col);
                        if (v != null) {
                            write(old, col, v);
                        }
                    }
                    return 1;
                }
                case "update": {
                    // update(null, UpdateWrapper)
                    UpdateWrapper<?> uw = (UpdateWrapper<?>) args[1];
                    String sqlSet = uw.getSqlSet();
                    // 参数按 MPGENVAL 编号排序，SET 在前、WHERE 在后，按出现次序对位。
                    List<Map.Entry<String, Object>> ps = new ArrayList<>(uw.getParamNameValuePairs().entrySet());
                    ps.sort(java.util.Comparator.comparingInt(e -> Integer.parseInt(e.getKey().replaceAll("\\D", ""))));
                    List<Object> ordered = new ArrayList<>();
                    ps.forEach(e -> ordered.add(e.getValue()));

                    String target = uw.getTargetSql() == null ? "" : uw.getTargetSql();
                    Matcher wm = Pattern.compile("(\\w+)\\s*=\\s*\\?").matcher(target);
                    Object whereId = null;
                    while (wm.find()) {
                        if ("id".equalsIgnoreCase(wm.group(1))) {
                            // .eq 在 .set 之后编号最大，取末位即 id。
                            whereId = ordered.get(ordered.size() - 1);
                            break;
                        }
                    }
                    Object old = store.get(whereId);
                    if (old == null) {
                        return 0;
                    }
                    // SET 子句里的 ? 排在前面（stage 等），null 以字面量出现不占参。
                    Matcher sm = Pattern.compile("(\\w+)\\s*=\\s*([^,]+)(?:,|$)").matcher(sqlSet);
                    int setIdx = 0;
                    while (sm.find()) {
                        String col = sm.group(1);
                        String expr = sm.group(2).trim();
                        Object val;
                        if ("null".equalsIgnoreCase(expr)) {
                            val = null;
                        } else if (expr.contains("MPGENVAL")) {
                            val = ordered.get(setIdx++);
                        } else {
                            val = expr;
                        }
                        write(old, col, val);
                    }
                    return 1;
                }
                case "delete": {
                    Predicate p = parse((AbstractWrapper<?, ?, ?>) args[0]);
                    store.values().removeIf(o -> p.test(o));
                    return 1;
                }
                case "selectById":
                case "selectByIdForUpdate":
                    return store.get(args[0]);
                case "selectCount": {
                    Predicate p = parse((AbstractWrapper<?, ?, ?>) args[0]);
                    return Math.toIntExact(store.values().stream().filter(p::test).count());
                }
                case "selectOne": {
                    List<Object> l = query((AbstractWrapper<?, ?, ?>) args[0]);
                    return l.stream().findFirst().orElse(null);
                }
                case "selectList": {
                    return query((AbstractWrapper<?, ?, ?>) args[0]);
                }
                case "selectObjs": {
                    AbstractWrapper<?, ?, ?> w = (AbstractWrapper<?, ?, ?>) args[0];
                    Predicate p = parse(w);
                    if (w.getSqlSelect() != null && w.getSqlSelect().contains("MAX(seq)")) {
                        long max = store.values().stream().filter(p::test)
                                .mapToLong(o -> ((Number) read(o, "seq")).longValue()).max().orElse(0);
                        List<Object> r = new ArrayList<>();
                        r.add((int) max);
                        return r;
                    }
                    return new ArrayList<>(store.values());
                }
                default:
                    if (method.getDeclaringClass() == Object.class) {
                        switch (m) {
                            case "toString": return "FakeMapper";
                            case "hashCode": return System.identityHashCode(proxy);
                            case "equals": return proxy == args[0];
                            default: return null;
                        }
                    }
                    throw new UnsupportedOperationException("fake mapper 未实现: " + m);
            }
        }

        /** 过滤 + ORDER BY + LIMIT 的极简实现。 */
        private List<Object> query(AbstractWrapper<?, ?, ?> w) {
            Predicate p = parse(w);
            List<Object> list = new ArrayList<>();
            store.values().stream().filter(p::test).forEach(list::add);

            String rawSql = w.getTargetSql() == null ? "" : w.getTargetSql();
            String sql = rawSql.toUpperCase();
            int ob = rawSql.toUpperCase().indexOf("ORDER BY");
            if (ob >= 0) {
                String orderPart = rawSql.substring(ob + 8);
                int lim = orderPart.toUpperCase().indexOf("LIMIT");
                if (lim >= 0) {
                    orderPart = orderPart.substring(0, lim);
                }
                List<String[]> orders = new ArrayList<>();
                for (String token : orderPart.split(",")) {
                    String t = token.trim();
                    if (t.isEmpty()) {
                        continue;
                    }
                    String[] parts = t.split("\\s+");
                    orders.add(new String[]{parts[0], parts.length > 1 ? parts[1] : "ASC"});
                }
                list.sort((x, y) -> {
                    for (String[] o : orders) {
                        Object ax = read(x, o[0]);
                        Object by = read(y, o[0]);
                        int cmp = compareNumOrStr(ax, by);
                        if ("DESC".equals(o[1])) {
                            cmp = -cmp;
                        }
                        if (cmp != 0) {
                            return cmp;
                        }
                    }
                    return 0;
                });
            }
            if (sql.matches("(?s).*\\bLIMIT\\s+1\\b.*")) {
                if (list.size() > 1) {
                    return new ArrayList<>(list.subList(0, 1));
                }
            }
            return list;
        }
    }

    /** 从 targetSql 的 ? 与参数对中解出 = / < / > / LIKE 条件。 */
    private static Predicate parse(AbstractWrapper<?, ?, ?> w) {
        String sql = w.getTargetSql() == null ? "" : w.getTargetSql();
        // MPGENVAL 的 Map 是 HashMap，必须按编号排回条件加入顺序，再与 ? 对位。
        List<Map.Entry<String, Object>> paramEntries =
                new ArrayList<>(w.getParamNameValuePairs().entrySet());
        paramEntries.sort(java.util.Comparator.comparingInt(
                e -> Integer.parseInt(e.getKey().replaceAll("\\D", ""))));
        List<Object> vals = new ArrayList<>();
        for (Map.Entry<String, Object> e : paramEntries) {
            vals.add(e.getValue());
        }
        List<Object[]> conds = new ArrayList<>();
        Matcher m = Pattern.compile("(\\w+)\\s*(=|<>|<|>|LIKE)\\s*\\?").matcher(sql);
        int i = 0;
        while (m.find()) {
            conds.add(new Object[]{m.group(1), m.group(2), vals.get(i++)});
        }
        return o -> {
            for (Object[] c : conds) {
                Object actual = read(o, (String) c[0]);
                Object expect = c[2];
                String op = (String) c[1];
                if ("LIKE".equals(op)) {
                    String pat = String.valueOf(expect).replace("%", "");
                    String av = actual == null ? "" : actual.toString();
                    String ev = String.valueOf(expect);
                    boolean ok = ev.endsWith("%") && ev.startsWith("%") ? av.contains(pat)
                            : ev.endsWith("%") ? av.startsWith(pat)
                            : ev.startsWith("%") ? av.endsWith(pat) : av.equals(pat);
                    if (!ok) {
                        return false;
                    }
                } else {
                    int cmp = compareNumOrStr(actual, expect);
                    switch (op) {
                        case "=": if (cmp != 0) return false; break;
                        case "<>": if (cmp == 0) return false; break;
                        case "<": if (!(cmp < 0)) return false; break;
                        case ">": if (!(cmp > 0)) return false; break;
                        default: return false;
                    }
                }
            }
            return true;
        };
    }

    interface Predicate {
        boolean test(Object o);
    }

    private static Object firstValue(AbstractWrapper<?, ?, ?> w) {
        return w.getParamNameValuePairs().values().iterator().next();
    }

    @SuppressWarnings("unchecked")
    private static int compareNumOrStr(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()));
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private static String snakeToCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char ch : s.toCharArray()) {
            if (ch == '_') {
                up = true;
            } else {
                sb.append(up ? Character.toUpperCase(ch) : ch);
                up = false;
            }
        }
        return sb.toString();
    }

    private static Object read(Object o, String col) {
        try {
            String name = "get" + Character.toUpperCase(col.charAt(0))
                    + snakeToCamel(col.substring(1)).replace("Id", "Id");
            // 直接按字段找最稳
            java.lang.reflect.Field f = findField(o.getClass(), col);
            f.setAccessible(true);
            return f.get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void write(Object o, String col, Object v) {
        try {
            java.lang.reflect.Field f = findField(o.getClass(), col);
            f.setAccessible(true);
            if (v != null && f.getType() == Long.class && v instanceof Number) {
                v = ((Number) v).longValue();
            } else if (v != null && (f.getType() == Integer.class || f.getType() == int.class)
                    && v instanceof Number) {
                v = ((Number) v).intValue();
            }
            f.set(o, v);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String columnOf(java.lang.reflect.Field f) {
        com.baomidou.mybatisplus.annotation.TableId ti =
                f.getAnnotation(com.baomidou.mybatisplus.annotation.TableId.class);
        if (ti != null) {
            return "id";
        }
        com.baomidou.mybatisplus.annotation.TableField tf =
                f.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);
        if (tf != null && !tf.value().isEmpty()) {
            return tf.value();
        }
        return f.getName();
    }

    private static java.lang.reflect.Field findField(Class<?> c, String col) {
        Class<?> cur = c;
        while (cur != null) {
            for (java.lang.reflect.Field f : cur.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                com.baomidou.mybatisplus.annotation.TableField tf =
                        f.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);
                if (tf != null && !tf.exist()) {
                    continue;
                }
                if (col.equals(columnOf(f))) {
                    return f;
                }
            }
            cur = cur.getSuperclass();
        }
        throw new RuntimeException("no field for column " + col + " on " + c);
    }
}
