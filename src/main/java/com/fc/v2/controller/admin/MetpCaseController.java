package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TMetpCase;
import com.fc.v2.model.custom.CaseAcceptDto;
import com.fc.v2.model.custom.CaseBatchVo;
import com.fc.v2.model.custom.CaseDetailVo;
import com.fc.v2.model.custom.CaseSubmitDto;
import com.fc.v2.service.ITMetpCaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 探测环境违法建设查处案卷 Controller（state-machine 形状：只有流转入口）。
 * 进门只此「受理」一扇；进一步、退一步的章程全在服务层，本子类不立业务规矩；
 * 没有普通的立单/存单/删单门——案卷局面由系统一笔一笔推出来，接口回话说了算。
 *
 * @author fuce
 * @date 2026-09-27
 */
@Api(value = "探测环境违法建设查处案卷")
@Controller
@RequestMapping("/metpCase")
public class MetpCaseController extends BaseController {

    private final String prefix = "admin/metpCase";

    @Autowired
    private ITMetpCaseService metpCaseService;

    @ApiOperation(value = "查处案卷台账跳转", notes = "查处案卷台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("metpCase:view")
    public String view() {
        return prefix + "/list";
    }

    @Log(title = "查处案卷台账", action = "list")
    @ApiOperation(value = "案卷台账", notes = "案卷台账")
    @GetMapping("/list")
    @RequiresPermissions("metpCase:list")
    @ResponseBody
    public ResultTable list(TMetpCase query) {
        QueryWrapper<TMetpCase> qw = new QueryWrapper<>();
        if (query.getCaseNo() != null && !query.getCaseNo().trim().isEmpty()) {
            qw.likeRight("case_no", query.getCaseNo().trim());
        }
        if (query.getBuildingName() != null && !query.getBuildingName().trim().isEmpty()) {
            qw.like("building_name", query.getBuildingName().trim());
        }
        if (query.getStage() != null) {
            qw.eq("stage", query.getStage());
        }
        if (query.getStatus() != null) {
            qw.eq("status", query.getStatus());
        }
        qw.orderByDesc("create_time");
        startPage();
        com.github.pagehelper.PageInfo<TMetpCase> page =
                new com.github.pagehelper.PageInfo<>(metpCaseService.selectCaseList(qw));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "案卷详情页", notes = "卷头与查处经过（正列/倒列）")
    @GetMapping("/detail/{id}")
    @RequiresPermissions("metpCase:view")
    public String detail(@PathVariable("id") Long id, ModelMap model) {
        model.put("vo", metpCaseService.caseDetail(id));
        return prefix + "/detail";
    }

    @Log(title = "查处案卷受理", action = "accept")
    @ApiOperation(value = "受理建卷", notes = "违法线索转入，一卷一号；同名建筑物有在办卷的不开第二卷")
    @PostMapping("/accept")
    @RequiresPermissions("metpCase:accept")
    @ResponseBody
    public AjaxResult accept(CaseAcceptDto dto) {
        TMetpCase c = metpCaseService.accept(dto);
        return AjaxResult.success("已受理建卷 " + c.getCaseNo()).put("data", c);
    }

    @Log(title = "查处案卷批量受理", action = "acceptBatch")
    @ApiOperation(value = "月初线索批量转入", notes = "一批违法线索转受理")
    @PostMapping("/acceptBatch")
    @RequiresPermissions("metpCase:accept")
    @ResponseBody
    public AjaxResult acceptBatch(@RequestBody List<CaseAcceptDto> list) {
        CaseBatchVo vo = metpCaseService.acceptBatch(list);
        return AjaxResult.success("转入完成：新开 " + vo.getOpened() + " 卷，另开被拒 "
                + vo.getSkipped().size() + " 件").put("data", vo);
    }

    @Log(title = "查处案卷递交", action = "submit")
    @ApiOperation(value = "递交当前段", notes = "只许递当前段，不许跳段；同段第二遍不认；技术意见进告知即冻结")
    @PostMapping("/submit")
    @RequiresPermissions("metpCase:submit")
    @ResponseBody
    public AjaxResult submit(CaseSubmitDto dto) {
        return AjaxResult.success("已递交").put("data", metpCaseService.submit(dto));
    }

    @Log(title = "查处案卷复查点头", action = "sign")
    @ApiOperation(value = "复查点头", notes = "OFFICER 执法承办人、CHIEF 台站长各点一次")
    @PostMapping("/sign")
    @RequiresPermissions("metpCase:sign")
    @ResponseBody
    public AjaxResult sign(Long id, String signRole) {
        return AjaxResult.success("点头已落账").put("data", metpCaseService.reviewSign(id, signRole));
    }

    @Log(title = "告知段退回重做", action = "returnNotice")
    @ApiOperation(value = "退回告知重做", notes = "拆除高度与调查记录对不上时退回；旧话作废留底不抹")
    @PostMapping("/returnNotice")
    @RequiresPermissions("metpCase:return")
    @ResponseBody
    public AjaxResult returnNotice(Long id) {
        return AjaxResult.success("已退回告知段重做，旧话留底").put("data", metpCaseService.returnToNotice(id));
    }

    @Log(title = "告知段重做递交", action = "redoNotice")
    @ApiOperation(value = "告知重做递交", notes = "退回后重做告知，另起一回回复查")
    @PostMapping("/redoNotice")
    @RequiresPermissions("metpCase:submit")
    @ResponseBody
    public AjaxResult redoNotice(CaseSubmitDto dto) {
        return AjaxResult.success("告知已重做，重入复查").put("data", metpCaseService.redoNotice(dto));
    }

    @Log(title = "查处案卷结案", action = "close")
    @ApiOperation(value = "结案", notes = "复查两头点齐方可结案；结案即死卷")
    @PostMapping("/close")
    @RequiresPermissions("metpCase:close")
    @ResponseBody
    public AjaxResult close(Long id) {
        return AjaxResult.success("已结案成卷，旧卷原样封存").put("data", metpCaseService.close(id));
    }
}
