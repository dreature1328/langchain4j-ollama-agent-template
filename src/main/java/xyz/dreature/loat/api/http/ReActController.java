package xyz.dreature.loat.api.http;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import xyz.dreature.loat.common.model.Result;
import xyz.dreature.loat.domain.agent.model.ReActRequest;
import xyz.dreature.loat.domain.agent.service.ReActService;

// 操作接口（ReAct）
@RestController
@Validated
public class ReActController {
    // ReAct 服务
    @Autowired
    private ReActService reactService;

    // 处理流程
    @PostMapping("/process")
    public ResponseEntity<Result<String>> process(@RequestBody @NotNull @Validated ReActRequest reActRequest) {
        String response = reactService.process(reActRequest);
        return ResponseEntity.ok(Result.success(response));
    }
}
