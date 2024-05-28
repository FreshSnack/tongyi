package teach.ai;

import com.alibaba.dashscope.aigc.conversation.ConversationParam.ResultFormat;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationOutput.Choice;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ToolMain {

    public static class GetWhetherTool {
        private final String location;

        public GetWhetherTool(String location) {
            this.location = location;
        }

        public String call() {
            return location + "今天是晴天";
        }
    }

    public static class GetTimeTool {

        public GetTimeTool() {
        }

        public String call() {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return "当前时间：" + now.format(formatter) + "。";
        }
    }

    public static void SelectTool()
            throws NoApiKeyException, ApiException, InputRequiredException {

        SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING).build();
        SchemaGenerator generator = new SchemaGenerator(config);


        ObjectNode jsonSchema_whether = generator.generateSchema(GetWhetherTool.class);
        ObjectNode jsonSchema_time = generator.generateSchema(GetTimeTool.class);


        FunctionDefinition fd_whether = FunctionDefinition.builder().name("get_current_whether").description("获取指定地区的天气")
                .parameters(JsonUtils.parseString(jsonSchema_whether.toString()).getAsJsonObject()).build();

        FunctionDefinition fd_time = FunctionDefinition.builder().name("get_current_time").description("获取当前时刻的时间")
                .parameters(JsonUtils.parseString(jsonSchema_time.toString()).getAsJsonObject()).build();

        Message systemMsg = Message.builder().role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant. When asked a question, use tools wherever possible.")
                .build();

        Scanner scanner = new Scanner(System.in);
        System.out.print("\n请输入：");
        String userInput = scanner.nextLine();
        Message userMsg =
                Message.builder().role(Role.USER.getValue()).content(userInput).build();

        List<Message> messages = new ArrayList<>(Arrays.asList(systemMsg, userMsg));

        GenerationParam param = GenerationParam.builder().model(Generation.Models.QWEN_MAX)
                .messages(messages).resultFormat(ResultFormat.MESSAGE)
                .tools(Arrays.asList(ToolFunction.builder().function(fd_whether).build(), ToolFunction.builder().function(fd_time).build())).build();
        // 大模型的第一轮调用
        Generation gen = new Generation();
        GenerationResult result = gen.call(param);

        System.out.println("\n大模型第一轮输出信息：" + JsonUtils.toJson(result));

        for (Choice choice : result.getOutput().getChoices()) {
            messages.add(choice.getMessage());
            // 如果需要调用工具
            if (result.getOutput().getChoices().get(0).getMessage().getToolCalls() != null) {
                for (ToolCallBase toolCall : result.getOutput().getChoices().get(0).getMessage()
                        .getToolCalls()) {
                    if (toolCall.getType().equals("function")) {
                        // 获取工具函数名称和入参
                        String functionName = ((ToolCallFunction) toolCall).getFunction().getName();
                        String functionArgument = ((ToolCallFunction) toolCall).getFunction().getArguments();
                        // 大模型判断调用天气查询工具的情况
                        if (functionName.equals("get_current_whether")) {
                            GetWhetherTool GetWhetherFunction =
                                    JsonUtils.fromJson(functionArgument, GetWhetherTool.class);
                            String whether = GetWhetherFunction.call();
                            Message toolResultMessage = Message.builder().role("tool")
                                    .content(String.valueOf(whether)).toolCallId(toolCall.getId()).build();
                            messages.add(toolResultMessage);
                            System.out.println("\n工具输出信息：" + whether);
                        }
                        // 大模型判断调用时间查询工具的情况
                        else if (functionName.equals("get_current_time")) {
                            GetTimeTool GetTimeFunction =
                                    JsonUtils.fromJson(functionArgument, GetTimeTool.class);
                            String time = GetTimeFunction.call();
                            Message toolResultMessage = Message.builder().role("tool")
                                    .content(String.valueOf(time)).toolCallId(toolCall.getId()).build();
                            messages.add(toolResultMessage);
                            System.out.println("\n工具输出信息：" + time);
                        }
                    }
                }
            }
            // 如果无需调用工具，直接输出大模型的回复
            else {
                // 此处直接返回模型的回复，您可以根据您的业务，在无需调用工具时选择最终回复的内容
                System.out.println("\n最终答案：" + result.getOutput().getChoices().get(0).getMessage().getContent());
                return;
            }
        }
        // 大模型的第二轮调用 包含工具输出信息
        param.setMessages(messages);
        result = gen.call(param);
        System.out.println("\n大模型第二轮输出信息：" + JsonUtils.toJson(result));
        System.out.println(("\n最终答案：" + result.getOutput().getChoices().get(0).getMessage().getContent()));
    }


    public static void main(String[] args) {
        try {
            Constants.apiKey = Const.DASHSCOPE_API_KEY;
            SelectTool();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.printf("Exception %s%n", e.getMessage());
        }
        System.exit(0);
    }
}