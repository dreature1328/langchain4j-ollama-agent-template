package xyz.dreature.loat.domain.tool.impl;


import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

// 系统工具
@Component
public class SystemTools {
    @Tool("执行系统终端命令并返回详细的执行结果")
    public Map<String, Object> runTerminalCommand(
            @P("要执行的终端命令字符串，例如：'dir', 'java --version'")
            String command,
            @P("命令的安全级别，'safe' 表示安全命令，'dangerous' 表示高危指令")
            String level
    ) {
        Map<String, Object> result = new HashMap<>();

        if ("dangerous".equals(level)) {
            System.out.printf("⚠️  警告：即将执行系统命令：%s%n", command);
            System.out.print("请确认是否继续 (y/n)：");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String confirm = reader.readLine().trim().toLowerCase();
                if (!"y".equals(confirm)) {
                    result.put("status", "aborted");
                    result.put("message", "用户取消执行");
                    return result;
                }
            } catch (Exception e) {
                result.put("status", "error");
                result.put("error", "用户输入读取失败: " + e.getMessage());
                return result;
            }
        }

        try {
            ProcessBuilder processBuilder;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                processBuilder = new ProcessBuilder("bash", "-c", command);
            }

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            result.put("status", exitCode == 0 ? "success" : "error");
            result.put("returncode", exitCode);
            result.put("output", output.toString().trim());
            if (error.length() > 0) {
                result.put("error", error.toString().trim());
            }

        } catch (Exception e) {
            result.put("status", "exception");
            result.put("error", "执行命令时发生异常: " + e.getMessage());
        }

        return result;
    }
}
