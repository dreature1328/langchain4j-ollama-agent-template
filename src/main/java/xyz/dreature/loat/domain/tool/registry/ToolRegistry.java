package xyz.dreature.loat.domain.tool.registry;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import xyz.dreature.loat.domain.tool.model.ExecutableTool;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ToolRegistry implements SmartInitializingSingleton {
    // 存储工具名到工具执行包装类的映射
    private final Map<String, ExecutableTool> toolMap = new HashMap<>();
    @Autowired
    private ApplicationContext context;

    // 实现接口函数，调用于容器完全初始化后，避免循环依赖
    @Override
    public void afterSingletonsInstantiated() {
        // 1. 扫描容器所有含 @Component 注解的 Bean
        String[] beanNames = context.getBeanNamesForAnnotation(Component.class);
        for (String beanName : beanNames) {
            Object bean = context.getBean(beanName);
            Class<?> beanClass = bean.getClass();

            // 2. 获取含 @Tool 注解的方法
            List<Method> toolMethods = Arrays.stream(beanClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(Tool.class))
                    .toList();

            // 3. 生成工具规范并注册
            for (Method method : toolMethods) {
                // 生成工具规范
                ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
                String toolName = spec.name();

                // 注册入表
                toolMap.put(toolName, new ExecutableTool(spec, bean, method));
                log.debug("注册工具：{} -> {}.{}()", toolName, bean.getClass().getSimpleName(), method.getName());
            }
        }
        log.debug("工具集描述：\n{}", getDescription());
    }

    // 根据工具名获取工具执行包装类
    public ExecutableTool get(String toolName) {
        return toolMap.get(toolName);
    }

    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        if (toolMap.isEmpty()) {
            sb.append("（暂无可用工具）");
        } else {
            int count = 0;
            for (ExecutableTool tool : toolMap.values()) {
                ToolSpecification spec = tool.getSpecification();
                Method method = tool.getMethod();
                Parameter[] parameters = method.getParameters();

                List<String> paramSignatures = Arrays.stream(parameters)
                        .map(param -> String.format("%s: %s", param.getName(), param.getType().getSimpleName()))
                        .toList();

                sb.append(String.format("%d. %s(%s): %s\n",
                        ++count,
                        spec.name(),
                        String.join(", ", paramSignatures),
                        spec.description()
                ));
            }
            log.debug("可用工具数：{}", count);
        }

        return sb.toString();
    }
}
