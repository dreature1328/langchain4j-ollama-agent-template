package xyz.dreature.loat.domain.tool.model;

import dev.langchain4j.agent.tool.ToolSpecification;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

// 工具执行包装类
public class ExecutableTool {
    private ToolSpecification specification;
    private Object target;
    private Method method;

    // ===== 构造方法 =====
    // 全参构造器
    public ExecutableTool(ToolSpecification specification, Object target, Method method) {
        this.specification = specification;
        this.target = target;
        this.method = method;
    }

    // 复制构造器
    public ExecutableTool(ExecutableTool executableTool) {
        this.specification = executableTool.specification;
        this.target = executableTool.target;
        this.method = executableTool.method;
    }

    // ===== Getter 与 Setter 方法 =====
    public ToolSpecification getSpecification() {
        return specification;
    }

    public void setSpecification(ToolSpecification specification) {
        this.specification = specification;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    // ===== 其他 =====
    // 执行工具
    public Object execute(Map<String, Object> arguments) throws InvocationTargetException, IllegalAccessException {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName();
            Object argValue = arguments.get(paramName);

            if (argValue == null) {
                throw new IllegalArgumentException(
                        String.format("执行工具'%s'失败：缺少参数'%s'", specification.name(), paramName)
                );
            }

            // 暂不考虑类型检查
            args[i] = arguments.get(paramName);
        }
        return method.invoke(target, args);
    }

    // 字符串表示
    @Override
    public String toString() {
        return "ExecutableTool{" +
                "specification=" + specification +
                ", target=" + target +
                ", method=" + method +
                '}';
    }
}
