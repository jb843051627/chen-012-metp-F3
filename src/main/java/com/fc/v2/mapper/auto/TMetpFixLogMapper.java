package com.fc.v2.mapper.auto;

import java.util.List;

import com.fc.v2.model.auto.TMetpFixLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 查处经过流水数据层。
 * 存单门焊严：此口只有「添一笔」与「按卷正/倒列」，没有改、没有删；
 * 先人的签名原封不动，作废留底靠退回一笔的 voided_seqs 指向，旧话一概不动。
 *
 * @author fuce
 * @date 2026-09-26
 */
public interface TMetpFixLogMapper {

    /** 添一笔（唯一写口） */
    @Insert("INSERT INTO t_metp_fix_log (id, card_id, seq, stage, action, horizontal_dist, blocked_azimuth,"
            + " excess_height, demolish_height, sign_role, sign_round, from_stage, to_stage, voided_seqs,"
            + " content, operator, operate_time, remark)"
            + " VALUES (#{id}, #{cardId}, #{seq}, #{stage}, #{action}, #{horizontalDist}, #{blockedAzimuth},"
            + " #{excessHeight}, #{demolishHeight}, #{signRole}, #{signRound}, #{fromStage}, #{toStage},"
            + " #{voidedSeqs}, #{content}, #{operator}, #{operateTime}, #{remark})")
    int insert(TMetpFixLog entity);

    /** 卷内当前最大序号（无记录返回 0），下一笔序号 = 返回值 + 1 */
    @Select("SELECT IFNULL(MAX(seq), 0) FROM t_metp_fix_log WHERE card_id = #{cardId}")
    int selectMaxSeq(@Param("cardId") Long cardId);

    /** 查处经过：dir=ASC 正着列一遍，dir=DESC 倒着列一遍（仅许服务层传入 ASC/DESC 字面量） */
    @Select("<script>SELECT id, card_id, seq, stage, action, horizontal_dist, blocked_azimuth, excess_height,"
            + " demolish_height, sign_role, sign_round, from_stage, to_stage, voided_seqs, content, operator,"
            + " operate_time, remark FROM t_metp_fix_log WHERE card_id = #{cardId} ORDER BY seq ${dir}</script>")
    List<TMetpFixLog> selectTrail(@Param("cardId") Long cardId, @Param("dir") String dir);
}
