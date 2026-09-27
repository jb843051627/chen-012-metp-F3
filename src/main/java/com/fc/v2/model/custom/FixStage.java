package com.fc.v2.model.custom;

/**
 * 查处六段：受理、调查、告知、处理决定、复查、结案。
 * 一段一段往前挪，不许跳着办；卷面 stage 记的是此刻停在的待办段（受理即建卷，受理段已办完）。
 *
 * @author fuce
 * @date 2026-09-26
 */
public enum FixStage {

    /** 受理（进门只此一扇，建卷即办完） */
    ACCEPT(0, "受理"),
    /** 调查（出技术意见：水平距离/遮挡方位角/高出平台） */
    INVESTIGATE(1, "调查"),
    /** 告知（技术意见一进本段即冻结；落下拆除高度） */
    NOTIFY(2, "告知"),
    /** 处理决定 */
    DECIDE(3, "处理决定"),
    /** 复查（县局执法承办人与台站长各点头一次） */
    REVIEW(4, "复查"),
    /** 结案（结案即死卷） */
    CLOSE(5, "结案");

    private final int code;
    private final String label;

    FixStage(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static FixStage of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FixStage s : values()) {
            if (s.code == code.intValue()) {
                return s;
            }
        }
        return null;
    }
}
