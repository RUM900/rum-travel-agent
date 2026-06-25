package com.yupi.yuaiagent.rag.eval;

import com.yupi.yuaiagent.app.TravelApp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 生成质量评估器（LLM-as-Judge）
 * <p>
 * 评估维度：
 * 1. Faithfulness（忠实度）— AI 回答是否严格基于检索到的文档，有没有编造事实
 * 2. Answer Relevance（答案相关性）— AI 回答是否直接、完整地回应了用户问题
 * <p>
 * ⚠️ 注意：
 * - 此测试会消耗 DashScope API 额度（每条用例约 2 次 LLM 调用），建议只在需要时运行
 * - 默认抽取 5 条用例做快速评估，如需全量测试修改 SAMPLE_SIZE
 */
@SpringBootTest
@Slf4j
class GenerationEvaluatorTest {

    @Resource
    private TravelApp travelApp;

    @Resource
    private VectorStore travelAppVectorStore;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 评测用例数（每条用例消耗 2 次 LLM 调用：1次生成回答 + 1次裁判评分）
     * 设为 ALL_CASES.size() 进行全量评估
     */
    private static final int SAMPLE_SIZE = 5;

    // ==================== LLM 裁判 ====================

    /**
     * Faithfulness（忠实度）— 回答是否完全基于检索文档，没有编造
     * 评分 1-5
     */
    private double judgeFaithfulness(String question, String answer, List<Document> retrievedDocs) {
        String context = retrievedDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 截断过长上下文，避免超出 token 限制
        if (context.length() > 4000) {
            context = context.substring(0, 4000) + "\n\n...(后续内容已截断)";
        }

        String judgePrompt = """
                你是一个严格的 RAG 质量评估专家。请评估以下 AI 的回答是否忠实于检索到的文档。

                【检索到的文档内容】
                %s

                【用户问题】
                %s

                【AI 回答】
                %s

                请根据以下标准打分（仅输出数字 1-5，不要输出其他内容）：

                1 = 回答内容与文档完全无关，纯属编造（幻觉）
                2 = 大部分内容无法在文档中找到依据
                3 = 部分内容可验证，但有明显无依据的陈述
                4 = 基本忠实于文档，只有极少数次要细节无法在文档中对应
                5 = 完全忠实，所有关键信息都能在文档中找到明确依据

                输出格式：只输出一个数字（1/2/3/4/5），不要包含任何解释。""".formatted(context, question, answer);

        return callJudge(judgePrompt, "Faithfulness");
    }

    /**
     * Answer Relevance（答案相关性）— 回答是否直接回应了用户问题
     * 评分 1-5
     */
    private double judgeAnswerRelevance(String question, String answer) {
        String judgePrompt = """
                你是一个严格的 RAG 质量评估专家。请评估以下 AI 回答是否直接、完整地回应了用户问题。

                【用户问题】
                %s

                【AI 回答】
                %s

                请根据以下标准打分（仅输出数字 1-5，不要输出其他内容）：

                1 = 完全跑题，答非所问
                2 = 只有少量内容相关，大部分答非所问
                3 = 基本回答了问题，但不够完整或不够直接
                4 = 回答较好，直接回应了问题核心，有少量不足
                5 = 完美回应，直接命中问题核心，信息完整无冗余

                输出格式：只输出一个数字（1/2/3/4/5），不要包含任何解释。""".formatted(question, answer);

        return callJudge(judgePrompt, "AnswerRelevance");
    }

    /**
     * 调用裁判 LLM 并解析分数
     */
    private double callJudge(String prompt, String metricName) {
        try {
            String response = ChatClient.builder(dashscopeChatModel)
                    .build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 从回复中提取数字
            String cleaned = response.trim().replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) {
                log.warn("{} 裁判返回异常内容: {}", metricName, response.substring(0, Math.min(50, response.length())));
                return 0;
            }
            double score = Double.parseDouble(cleaned.substring(0, 1)); // 只取第一个数字
            if (score < 1 || score > 5) {
                log.warn("{} 裁判打分越界: {} → 截断为 {}", metricName, score, Math.max(1, Math.min(5, score)));
                score = Math.max(1, Math.min(5, score));
            }
            return score;
        } catch (Exception e) {
            log.error("{} 裁判调用失败: {}", metricName, e.getMessage());
            return 0;
        }
    }

    // ==================== 单条评估 ====================

    /**
     * 评估单条用例的完整生成质量（一次 RAG 问答 + 两个维度的裁判打分）
     */
    private record GenEvalResult(
            String questionPreview,
            double faithfulness,
            double relevance,
            String answerPreview,
            int docCount
    ) {}

    private GenEvalResult evaluateOne(EvalDataset.EvalCase testCase, int index) {
        String chatId = "eval-gen-" + index;

        // 1. 调用 RAG 对话
        String answer = travelApp.doChatWithRag(testCase.question(), chatId);

        // 2. 检索相关文档（用于 faithfulness 判断）
        List<Document> retrievedDocs = travelAppVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(testCase.question())
                        .topK(3)
                        .similarityThreshold(0.4)
                        .build()
        );

        // 3. 裁判打分
        double faith = judgeFaithfulness(testCase.question(), answer, retrievedDocs);
        double relevance = judgeAnswerRelevance(testCase.question(), answer);

        String preview = answer != null && answer.length() > 60
                ? answer.substring(0, 60).replace("\n", " ") + "..."
                : answer;

        return new GenEvalResult(
                testCase.question().length() > 30
                        ? testCase.question().substring(0, 30) + "..."
                        : testCase.question(),
                faith, relevance, preview, retrievedDocs.size()
        );
    }

    // ==================== 格式化辅助方法 ====================

    private static String rpad(String s, int n) {
        if (s == null) s = "";
        // 中文字符占 2 个显示宽度，简单估算
        StringBuilder sb = new StringBuilder(s);
        while (displayWidth(sb.toString()) < n) sb.append(' ');
        // 截断超长的
        while (displayWidth(sb.toString()) > n && sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private static int displayWidth(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += (c >= 0x4E00 && c <= 0x9FFF) ? 2 : 1;
        }
        return w;
    }

    private static String ft1(double v) {
        return String.format("%.1f", v);
    }

    // ==================== 运行入口 ====================

    @Test
    @DisplayName("RAG 生成质量评估（LLM-as-Judge，抽样5条）")
    void evaluateGenerationQuality() {
        List<EvalDataset.EvalCase> cases = EvalDataset.ALL_CASES;
        int sampleN = Math.min(SAMPLE_SIZE, cases.size());

        log.info("");
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║   RAG 生成质量 — LLM 裁判评估（抽样{}条）      ║", sampleN);
        log.info("╚══════════════════════════════════════════════════╝");
        log.info("");
        log.info("⚠️  每条用例消耗约 3 次 LLM 调用（1次RAG回答 + 2次裁判打分）");
        log.info("⚠️  本次评估预计消耗约 {} 次 DashScope API 调用", sampleN * 3);
        log.info("");

        double sumFaith = 0, sumRelevance = 0;
        int validFaith = 0, validRelevance = 0;

        log.info("┌──────┬──────────────────────────────────┬───────────┬───────────┬──────────────────────────────────────────┐");
        log.info("│  序号 │ 问题                             │ Faithfulness│ Relevance │ 回答预览                                 │");
        log.info("├──────┼──────────────────────────────────┼───────────┼───────────┼──────────────────────────────────────────┤");

        for (int i = 0; i < sampleN; i++) {
            GenEvalResult r = evaluateOne(cases.get(i), i);

            if (r.faithfulness() > 0) { sumFaith += r.faithfulness(); validFaith++; }
            if (r.relevance() > 0) { sumRelevance += r.relevance(); validRelevance++; }

            log.info(String.format("│ %4d │ %-30s │     %.0f/5  │     %.0f/5  │ %-40s │",
                    i + 1, r.questionPreview(), r.faithfulness(), r.relevance(), r.answerPreview()));
        }
        log.info("└──────┴──────────────────────────────────┴───────────┴───────────┴──────────────────────────────────────────┘");

        double avgFaith = validFaith > 0 ? sumFaith / validFaith : 0;
        double avgRelevance = validRelevance > 0 ? sumRelevance / validRelevance : 0;

        String faithRating = avgFaith >= 4.0 ? "✅ 达标" : avgFaith >= 3.0 ? "🔶 一般" : "❌ 偏低";
        String relevanceRating = avgRelevance >= 4.0 ? "✅ 达标" : avgRelevance >= 3.0 ? "🔶 一般" : "❌ 偏低";

        log.info("");
        log.info("┌──────────────────────────────────────────────────┐");
        log.info("│            生成质量总分卡                         │");
        log.info("├──────────────────┬─────────┬──────────┬──────────┤");
        log.info("│ 指标             │   平均分 │   目标值  │   评级   │");
        log.info("├──────────────────┼─────────┼──────────┼──────────┤");
        log.info("│ Faithfulness     │  {}/5  │  > 4.0   │  {}      │", ft1(avgFaith), faithRating);
        log.info("│ Answer Relevance │  {}/5  │  > 4.0   │  {}      │", ft1(avgRelevance), relevanceRating);
        log.info("└──────────────────┴─────────┴──────────┴──────────┘");

        // 诊断
        log.info("");
        log.info("🔍 诊断：");
        if (avgFaith < 3.0) {
            log.info("❌ Faithfulness 偏低 — AI 回答存在明显幻觉，可能原因：");
            log.info("   1. 检索到的文档与问题不相关（先优化检索质量）");
            log.info("   2. System Prompt 中「不要凭记忆编造数据」的约束力不够");
            log.info("   3. LLM 模型本身倾向于自信编造（考虑换模型或降低 temperature）");
        }
        if (avgRelevance < 3.0) {
            log.info("❌ Answer Relevance 偏低 — AI 回答偏离用户问题，可能原因：");
            log.info("   1. 检索文档和用户问题不匹配，LLM 被迫在无关文档上作答");
            log.info("   2. QueryRewriter 改写了用户意图，导致检索偏了");
            log.info("   3. System Prompt 引导 LLM 输出过多模板化内容");
        }
        if (avgFaith >= 4.0 && avgRelevance >= 4.0) {
            log.info("✅ 生成质量良好，RAG 端到端效果达标");
        }

        log.info("");
        log.info("💡 如需全量评估，修改 SAMPLE_SIZE 为 EvalDataset.ALL_CASES.size()");
    }

    @Test
    @DisplayName("RAG vs 纯 LLM 生成效果对比")
    void compareRagVsPureLlm() {
        // 取前 3 条用例做对比
        List<EvalDataset.EvalCase> sample = EvalDataset.ALL_CASES.subList(0, Math.min(3, EvalDataset.ALL_CASES.size()));

        log.info("");
        log.info("========== RAG vs 纯 LLM 效果对比（Faithfulness）==========");
        log.info("此测试对比：带上文 RAG 回答 vs 纯 LLM 直接回答（无文档参考）");
        log.info("");

        double ragFaithSum = 0, pureFaithSum = 0;
        int count = 0;

        for (int i = 0; i < sample.size(); i++) {
            EvalDataset.EvalCase c = sample.get(i);
            String chatId = "eval-compare-" + i;

            // RAG 回答
            String ragAnswer = travelApp.doChatWithRag(c.question(), chatId);
            List<Document> docs = travelAppVectorStore.similaritySearch(
                    SearchRequest.builder().query(c.question()).topK(3).similarityThreshold(0.4).build()
            );
            double ragFaith = judgeFaithfulness(c.question(), ragAnswer, docs);

            // 纯 LLM 回答（无 RAG）
            String pureAnswer = ChatClient.builder(dashscopeChatModel)
                    .build()
                    .prompt()
                    .user(c.question())
                    .call()
                    .content();
            double pureFaith = judgeFaithfulness(c.question(), pureAnswer, docs);

            ragFaithSum += ragFaith;
            pureFaithSum += pureFaith;
            count++;

            log.info(String.format("Q%d: RAG Faith=%.0f/5 | 纯LLM Faith=%.0f/5 | %s",
                    i + 1, ragFaith, pureFaith,
                    c.question().substring(0, Math.min(30, c.question().length())) + "..."));
        }

        double avgRagFaith = count > 0 ? ragFaithSum / count : 0;
        double avgPureFaith = count > 0 ? pureFaithSum / count : 0;

        log.info("");
        log.info("RAG Faithfulness 均值:   {}/5", ft1(avgRagFaith));
        log.info("纯LLM Faithfulness 均值: {}/5", ft1(avgPureFaith));
        log.info("RAG 提升: {}", avgRagFaith > avgPureFaith
                ? String.format("+%.1f ✅", avgRagFaith - avgPureFaith)
                : String.format("%.1f ❌ RAG反而更差", avgRagFaith - avgPureFaith));

        if (avgRagFaith <= avgPureFaith) {
            log.warn("⚠️ RAG 未提升 Faithfulness，说明检索环节出了问题 — 文档不相关或 LLM 忽略了文档");
        }
    }
}
