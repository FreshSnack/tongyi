package teach.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.*;
import com.alibaba.dashscope.tokenizers.Tokenizer;
import com.alibaba.dashscope.tokenizers.TokenizerFactory;
import com.alibaba.dashscope.utils.JsonUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * 通义千问大语言模型
 * <a>https://help.aliyun.com/document_detail/2786271.html?spm=a2c4g.2712816.0.0.3cc57211N04Sgo</a>
 */
public class QwenLangTest extends BaseTest {

    /**
     * Token是模型用来表示自然语言文本的基本单位，可以直观的理解为“字”或“词”。对于中文文本来说，1个token通常对应一个汉字；对于英文文本来说，1个token通常对应3至4个字母。
     * 通义千问模型服务根据模型输入和输出的token数量进行计量计费，其中多轮对话中的history作为输入也会进行计量计费。每一次模型调用产生的实际token数量可以从 response 中获取。
     */
    @Test
    public void testToken() throws NoSpecialTokenExists, UnSupportedSpecialTokenMode {
        Tokenizer tokenizer = TokenizerFactory.qwen();
        String prompt = "如果现在要你走十万八千里路，需要多长的时间才能到达？";
        // 26个中文字符 => 17 tokens
        List<Integer> ids = tokenizer.encodeOrdinary(prompt);
        System.out.println(ids);

        tokenizer = TokenizerFactory.qwen();
        prompt = "<|im_start|>system\nYour are a helpful assistant.<|im_end|>\n<|im_start|>user\nSanFrancisco is a<|im_end|>\n<|im_start|>assistant\n";
        // 24 tokens
        ids = tokenizer.encode(prompt, "all");
        System.out.println(ids);
    }

    /**
     * {
     * "requestId": "6bb517ba-a98b-94cb-997f-f51973659286",
     * "usage": {
     * "input_tokens": 38,
     * "output_tokens": 382,
     * "total_tokens": 420
     * },
     * "output": {
     * "text": "标题：倡议书：守护蓝色家园，从限塑行动做起\n\n一、引言\n1. 简述现状：简要介绍当前全球海洋污染的严峻形势，特别是塑料垃圾对海洋生态系统的破坏，如海洋生物误食塑料、生态系统链被破坏等。\n\n二、塑料污染的严重性\n1. 数字呈现：引用科学研究数据，说明塑料垃圾在海洋中的数量和增长速度，以及对海洋生物和人类健康的影响。\n2. 生态影响：详细阐述塑料垃圾对海洋生态平衡的破坏，比如海洋生物死亡、食物链断裂等具体案例。\n\n三、塑料制品的过度使用\n1. 日常生活的塑料消耗：列举日常生活中塑料制品的广泛使用，如一次性塑料袋、塑料瓶、塑料餐具等。\n2. 长期累积效应：强调塑料制品的不易降解性，长期堆积会对环境造成持久压力。\n\n四、限塑行动的意义\n1. 保护环境：限塑可以减少塑料垃圾的产生，减轻海洋污染，保护海洋生物和生态多样性。\n2. 健康生活：提倡环保生活方式，有利于个人和社会的健康长远发展。\n\n五、具体的限塑措施\n1. 政策层面：呼吁政府出台更严格的塑料制品限制政策，鼓励环保替代品的研发和使用。\n2. 个人行动：倡导消费者减少购买一次性塑料产品，自带环保购物袋、水杯等。\n3. 教育普及：加强公众对塑料污染问题的认识，提高环保意识。\n\n六、结语\n1. 强调每个人的力量：每个人的小行动都能汇聚成大改变，鼓励大家共同参与限塑行动。\n2. 期待未来：描绘一个没有塑料污染、海洋清澈的未来愿景，激发大家的环保热情。\n\n落款：\n日期\n发起人/组织名称",
     * "finish_reason": "stop"
     * }
     * }
     */
    @Test
    public void qwenQuickStart() throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder().model(Generation.Models.QWEN_TURBO)
                .prompt("就当前的海洋污染的情况，写一份限塑的倡议书提纲，需要有理有据地号召大家克制地使用塑料制品")
                .topP(0.8).build();
        GenerationResult result = gen.call(param);
        System.out.println(JsonUtils.toJson(result));
    }

    @Test
    public void qwenQuickStartCallback() throws NoApiKeyException, ApiException, InputRequiredException, InterruptedException {
        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder().model(Generation.Models.QWEN_TURBO)
                // 在prompt中可以添加系统人设
                .prompt("就当前的海洋污染的情况，写一份限塑的倡议书提纲，需要有理有据地号召大家克制地使用塑料制品")
                // 控制核采样方法的概率阈值，取值越大，生成的随机性越高。
                .topP(0.8)
                // 控制生成随机性和多样性，范围(0,2)。建议该参数和top_p只设置1个。
                .temperature(1.0f)
                // 用于控制生成时遇到某些内容则停止。您可传入多个字符串。
                // .stopString("")
                // 是否参考Web搜索(夸克)的结果，默认false。
                .enableSearch(false)
                // 可以设置API KEY
                // .apiKey(Const.DASHSCOPE_API_KEY)
                .build();
        Semaphore semaphore = new Semaphore(0);
        gen.call(param, new ResultCallback<GenerationResult>() {
            @Override
            public void onEvent(GenerationResult message) {
                System.out.println(message);
            }

            @Override
            public void onError(Exception ex) {
                System.out.println(ex.getMessage());
                semaphore.release();
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete");
                semaphore.release();
            }
        });
        semaphore.acquire();
    }

    @Test
    public void callWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();

        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant.")
                .build();

        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("如何做西红柿炒鸡蛋？")
                .build();

        GenerationParam param = GenerationParam.builder()
                .model(Generation.Models.QWEN_TURBO)
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .topP(0.8)
                .build();

        GenerationResult call = gen.call(param);
        System.out.println(JsonUtils.toJson(call));
    }

    /**
     * 多轮对话
     */
    @Test
    public void callGenerationWithMessages() throws ApiException, NoApiKeyException, InputRequiredException {
        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder().role(Role.SYSTEM.getValue()).content("You are a helpful assistant.").build());
        messages.add(Message.builder().role(Role.USER.getValue()).content("如何做西红柿炖牛腩？").build());

        GenerationParam param = GenerationParam.builder()
                .model(Generation.Models.QWEN_TURBO)
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .topP(0.8)
                .build();
        GenerationResult result = new Generation().call(param);
        System.out.println(JsonUtils.toJson(result));

        // 添加assistant返回的消息到列表
        messages.add(result.getOutput().getChoices().get(0).getMessage());

        // 添加新的用户消息
        messages.add(Message.builder().role(Role.USER.getValue()).content("不放糖可以吗？").build());

        result = new Generation().call(param);
        System.out.println(JsonUtils.toJson(result));
    }

}
