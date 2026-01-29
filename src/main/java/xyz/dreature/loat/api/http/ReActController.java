package xyz.dreature.loat.api.http;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.dreature.loat.common.model.Result;
import xyz.dreature.loat.domain.agent.model.ReActRequest;
import xyz.dreature.loat.domain.agent.service.ReActService;

// 操作接口（ReAct）
@RestController
@RequestMapping("/react")
@Validated
public class ReActController {
    // ReAct 服务
    @Autowired
    private ReActService reactService;

    // 手动处理流程（程序控制）
    @RequestMapping("/process")
    public ResponseEntity<Result<String>> process(@RequestBody @NotNull @Validated ReActRequest reActRequest) {
        String response = reactService.process(reActRequest);
        return ResponseEntity.ok(Result.success(response));
    }

    // 自动处理流程（框架托管，需模型原生支持 function calling 协议）
    @RequestMapping("/process-x")
    public ResponseEntity<Result<String>> processX(@RequestBody @NotNull @Validated ReActRequest reActRequest) {
        String response = reactService.processX(reActRequest);
        return ResponseEntity.ok(Result.success(response));
    }
}
