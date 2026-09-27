package com.fc.v2.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TMetpFixCard;
import com.fc.v2.model.custom.FixClueItem;
import com.fc.v2.service.ITMetpFixCardService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 探测环境违规整改事务卡 Controller
 * 进一步退一步的章程都在服务层；本层只接参数、落下办理人、回系统推出来的局面。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "探测环境违规整改事务卡")
@Controller
@RequestMapping("/metpFixCard")
public class MetpFixCardController extends BaseController {

    private final String prefix = "admin/metpFixCard";

    @Autowired
    private ITMetpFixCardService metpFixCardService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("metpFixCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "探测环境违规整改事务卡流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "流转台账")
    @GetMapping("/list")
    @RequiresPermissions("metpFixCard:list")
    @ResponseBody
    public ResultTable list(TMetpFixCard record) {
        QueryWrapper<TMetpFixCard> queryWrapper = new QueryWrapper<TMetpFixCard>();
        queryWrapper.eq("del_flag", 0);
        if (record != null) {
            queryWrapper.eq(record.getStatus() != null, "status", record.getStatus());
            queryWrapper.eq(record.getStage() != null, "stage", record.getStage());
            queryWrapper.like(record.getBuildingName() != null && !record.getBuildingName().isEmpty(),
                    "building_name", record.getBuildingName());
            queryWrapper.like(record.getBizNo() != null && !record.getBizNo().isEmpty(),
                    "biz_no", record.getBizNo());
        }
        queryWrapper.orderByDesc("id");
        startPage();
        com.github.pagehelper.PageInfo<TMetpFixCard> page =
                new com.github.pagehelper.PageInfo<TMetpFixCard>(metpFixCardService.selectTMetpFixCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "查处经过", action = "trail")
    @ApiOperation(value = "查处经过", notes = "正着列一遍、倒着列一遍，含退回作废的留底旧话")
    @GetMapping("/trail")
    @RequiresPermissions("metpFixCard:trail")
    @ResponseBody
    public AjaxResult trail(Long id, @RequestParam(defaultValue = "true") boolean asc) {
        return retobject(200, metpFixCardService.selectTrail(id, asc));
    }

    @ApiOperation(value = "卷面当前各段在册记录", notes = "被退回作废的旧话不摆，只摆重做后那一条")
    @GetMapping("/liveLogs")
    @RequiresPermissions("metpFixCard:trail")
    @ResponseBody
    public AjaxResult liveLogs(Long id) {
        return retobject(200, metpFixCardService.selectLiveLogs(id));
    }

    @Log(title = "违法线索清单转入", action = "intake")
    @ApiOperation(value = "受理转入", notes = "每月初从违法线索清单转进来一批；同一座建筑物已有未结卷的不再另开第二卷")
    @PostMapping("/intake")
    @RequiresPermissions("metpFixCard:intake")
    @ResponseBody
    public AjaxResult intake(@RequestBody List<FixClueItem> clues) {
        int opened = metpFixCardService.intake(clues, ShiroUtils.getLoginName());
        return success(200, "本次转入新开" + opened + "卷", opened);
    }

    @Log(title = "调查段递交", action = "submitInvestigate")
    @ApiOperation(value = "调查段递交", notes = "落下技术意见：水平距离/遮挡方位角/高出平台，一进告知段即冻结")
    @PostMapping("/submitInvestigate")
    @RequiresPermissions("metpFixCard:submit")
    @ResponseBody
    public AjaxResult submitInvestigate(Long id, BigDecimal horizontalDist, BigDecimal blockedAzimuth,
                                        BigDecimal excessHeight, String content) {
        TMetpFixCard card = metpFixCardService.submitInvestigate(id, horizontalDist, blockedAzimuth,
                excessHeight, content, ShiroUtils.getLoginName());
        return success(200, "调查段已落，卷进告知段", card);
    }

    @Log(title = "告知段递交", action = "submitNotify")
    @ApiOperation(value = "告知段递交", notes = "落下拆除高度；技术意见只许照着调查段的取")
    @PostMapping("/submitNotify")
    @RequiresPermissions("metpFixCard:submit")
    @ResponseBody
    public AjaxResult submitNotify(Long id, BigDecimal demolishHeight, String content) {
        TMetpFixCard card = metpFixCardService.submitNotify(id, demolishHeight, content, ShiroUtils.getLoginName());
        return success(200, "告知段已落，卷进处理决定段", card);
    }

    @Log(title = "处理决定段递交", action = "submitDecide")
    @ApiOperation(value = "处理决定段递交", notes = "落下处理决定正文")
    @PostMapping("/submitDecide")
    @RequiresPermissions("metpFixCard:submit")
    @ResponseBody
    public AjaxResult submitDecide(Long id, String decisionText, String content) {
        TMetpFixCard card = metpFixCardService.submitDecide(id, decisionText, content, ShiroUtils.getLoginName());
        return success(200, "处理决定已落，卷进复查段", card);
    }

    @Log(title = "复查点头", action = "reviewSign")
    @ApiOperation(value = "复查点头", notes = "县局执法承办人与台站长各点头一次，少一次不算复查过")
    @PostMapping("/reviewSign")
    @RequiresPermissions("metpFixCard:sign")
    @ResponseBody
    public AjaxResult reviewSign(Long id, String role) {
        TMetpFixCard card = metpFixCardService.reviewSign(id, role, ShiroUtils.getLoginName());
        return success(200, "复查点头已落", card);
    }

    @Log(title = "退回告知段", action = "returnToNotify")
    @ApiOperation(value = "退回告知段", notes = "复查时看出拆除高度与调查记录对不上，退回告知段重做，旧话作废留底")
    @PostMapping("/returnToNotify")
    @RequiresPermissions("metpFixCard:return")
    @ResponseBody
    public AjaxResult returnToNotify(Long id, String reason) {
        TMetpFixCard card = metpFixCardService.returnToNotify(id, reason, ShiroUtils.getLoginName());
        return success(200, "已退回告知段，旧话作废留底", card);
    }

    @Log(title = "结案", action = "submitClose")
    @ApiOperation(value = "结案", notes = "结案即死卷，不许再往上挂记事，不许抽掉整卷")
    @PostMapping("/submitClose")
    @RequiresPermissions("metpFixCard:close")
    @ResponseBody
    public AjaxResult submitClose(Long id, String content) {
        TMetpFixCard card = metpFixCardService.submitClose(id, content, ShiroUtils.getLoginName());
        return success(200, "结案已落，此卷成死卷", card);
    }

    @Log(title = "抽卷", action = "remove")
    @ApiOperation(value = "抽掉整卷", notes = "结案死卷一概不许；在办卷仅许软删，查处经过留底不动")
    @PostMapping("/remove")
    @RequiresPermissions("metpFixCard:remove")
    @ResponseBody
    public AjaxResult remove(Long id) {
        boolean ok = metpFixCardService.remove(id);
        return ok ? success("已抽卷留底") : error("结案即死卷，不许抽掉整卷");
    }
}
