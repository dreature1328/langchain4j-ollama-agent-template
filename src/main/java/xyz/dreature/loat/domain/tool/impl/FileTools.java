package xyz.dreature.loat.domain.tool.impl;


import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// 文件工具
@Component
public class FileTools {
    @Tool("读取指定文件的全部内容")
    public String readFile(
            @P("要读取的文件的绝对路径或相对路径，例如：'D:/example.txt', '/home/usr/data.json'")
            String filePath
    ) {
        try {
            Path path = Paths.get(filePath);
            return Files.readString(path);
        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }
    }

    @Tool("将指定内容写入文件，如果文件不存在则创建，如果存在则覆盖")
    public String writeToFile(
            @P("目标文件的绝对路径，例如: 'D:/output.txt'") String filePath,
            @P("要写入文件的文本内容，支持包含换行符的多行文本") String content
    ) {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            return "写入成功: " + filePath;
        } catch (IOException e) {
            return "写入文件失败: " + e.getMessage();
        }
    }
}
