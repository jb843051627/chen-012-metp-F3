package com.fc.v2.model.custom;

/**
 * 查处经过的过口动作。每一笔都是系统落下的一回重载，先人记录原封不动。
 *
 * @author fuce
 * @date 2026-09-26
 */
public enum FixAction {

    /** 受理（建卷，唯一进门） */
    ACCEPT("受理"),
    /** 调查递交（落下技术意见三项） */
    INVESTIGATE("调查"),
    /** 告知递交（落下拆除高度） */
    NOTIFY("告知"),
    /** 处理决定递交 */
    DECIDE("处理决定"),
    /** 复查点头（执法承办人/台站长各一次） */
    REVIEW_SIGN("复查点头"),
    /** 退回上一段（旧话作废留底，页面只摆重做后那一条） */
    RETURN("退回"),
    /** 结案（结案即死卷） */
    CLOSE("结案");

    private final String label;

    FixAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
