package teach.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RealInTimeMain {

    public static void main(String[] args) throws NoApiKeyException, InputRequiredException {
        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder().role(Role.SYSTEM.getValue()).content("You are a helpful assistant.").build());
        for (int i = 0; i < 3; i++) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("请输入：");
            String userInput = scanner.nextLine();
            if ("exit".equalsIgnoreCase(userInput)) {
                break;
            }
            messages.add(Message.builder().role(Role.USER.getValue()).content(userInput).build());
            GenerationParam param = GenerationParam.builder()
                    .model(Generation.Models.QWEN_TURBO)
                    .messages(messages)
                    .apiKey(Const.DASHSCOPE_API_KEY)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.8)
                    .build();
            GenerationResult result = new Generation().call(param);
            System.out.println("模型输出：" + result.getOutput().getChoices().get(0).getMessage().getContent());
            messages.add(result.getOutput().getChoices().get(0).getMessage());
        }
    }
}
