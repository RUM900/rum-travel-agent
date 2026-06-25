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
 * RAG 效果诊断器 — 定位 Faithfulness 偏低的根因
 * <p>
 * 三个诊断维度：
 * 1. 检索结果是否和问题相关？（检索诊断）
 * 2. LLM 是否真的在用检索到的文档？（文档利用率诊断）
 * 3. 知识库文档和 LLM 自带知识谁更丰富？（文档价值诊断）
 */
@SpringBootTest
@Slf4j
class RagDiagnosisTest {

    @Resource
    private VectorStore travelAppVectorStore;

    @Resource
    private TravelApp travelApp;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 诊断 1：检索结果到底搜到了什么？
     * 打印每个问题检索到的文档内容，人工判断是否相关
     */
    @Test
    @DisplayName("诊断1 — 检索结果相关性检查")
    void diagnoseRetrievalRelevance() {
        // 取 5 条有代表性的问题（覆盖路线模板 / 景点数据 / 花期节庆 3 篇知识库文档）
        List<EvalDataset.EvalCase> sample = List.of(
                EvalDataset.ALL_CASES.get(0),   // 川西自驾6天（路线模板）
                EvalDataset.ALL_CASES.get(3),   // 带父母去苏州杭州4天（路线模板）
                EvalDataset.ALL_CASES.get(8),   // 故宫门票多少钱（景点数据）
                EvalDataset.ALL_CASES.get(12),  // 张家界玩2天够吗（景点数据）
                EvalDataset.ALL_CASES.get(17)   // 3月份去哪里看花（花期节庆）
        );

        for (int i = 0; i < sample.size(); i++) {
            EvalDataset.EvalCase c = sample.get(i);
            log.info("");
            log.info("═══════════════════════════════════════════");
            log.info("问题{}: {}", i + 1, c.question());
            log.info("期望关键词: {}", String.join(", ", c.expectedKeywords().subList(0, Math.min(5, c.expectedKeywords().size()))));
            log.info("───────────────────────────────────────────");

            List<Document> results = travelAppVectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(c.question())
                            .topK(3)
                            .similarityThreshold(0.4)
                            .build()
            );

            if (results.isEmpty()) {
                log.warn("❌ 未检索到任何文档！（similarityThreshold=0.4 可能太严格）");
            }

            for (int j = 0; j < results.size(); j++) {
                Document doc = results.get(j);
                String filename = doc.getMetadata() != null
                        ? (String) doc.getMetadata().getOrDefault("filename", "未知")
                        : "未知";
                String status = doc.getMetadata() != null
                        ? (String) doc.getMetadata().getOrDefault("status", "未知")
                        : "未知";
                String content = doc.getText();
                if (content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                log.info("── 结果{} [{}|{}] ──", j + 1, filename, status);
                log.info("{}", content);

                // 检查是否命中期望关键词
                boolean relevant = c.expectedKeywords().stream().anyMatch(doc.getText()::contains);
                log.info("相关: {}", relevant ? "✅" : "❌");
            }
        }

        log.info("");
        log.info("💡 如果大部分文档确实和问题相关 → 问题在 LLM 生成环节，而非检索");
        log.info("💡 如果大部分文档不相关 → 问题在检索/embedding/切分环节");
        log.info("💡 如果文档相关但 Faithfulness 仍然低 → 说明 LLM 的回答超出了文档范围（自带知识更丰富）");
    }

    /**
     * 诊断 2：LLM 有没有真的在使用检索到的文档？
     * 分别用「带 RAG」「不带 RAG」「只给文档不给 LLM 知识」三种模式回答，对比差异
     */
    @Test
    @DisplayName("诊断2 — RAG vs 纯文档 vs 纯LLM 三方对比")
    void diagnoseThreeWayComparison() {
        EvalDataset.EvalCase c = EvalDataset.ALL_CASES.get(0); // "春天带父母去旅游"
        String chatId = "diag-2";

        log.info("");
        log.info("═══════════════════════════════════════════");
        log.info("问题: {}", c.question());
        log.info("");

        // 模式A：纯 LLM（无 RAG，无 System Prompt）
        String pureLlm = ChatClient.builder(dashscopeChatModel)
                .build().prompt().user(c.question()).call().content();
        log.info("【模式A — 纯 LLM（无任何约束）】");
        log.info("{}", pureLlm.substring(0, Math.min(300, pureLlm.length())));
        log.info("");

        // 模式B：RAG 回答（带 System Prompt + 检索文档）
        String ragAnswer = travelApp.doChatWithRag(c.question(), chatId);
        log.info("【模式B — RAG（System Prompt + 检索文档）】");
        log.info("{}", ragAnswer != null ? ragAnswer.substring(0, Math.min(300, ragAnswer.length())) : "null");
        log.info("");

        // 模式C：只给文档，要求 LLM 严格只用文档内容回答
        List<Document> docs = travelAppVectorStore.similaritySearch(
                SearchRequest.builder().query(c.question()).topK(3).similarityThreshold(0.4).build()
        );
        String docContext = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        String strictPrompt = """
                你只能根据以下文档内容回答问题。如果文档中没有相关信息，直接说"文档中没有足够信息回答这个问题"。
                禁止使用文档之外的任何知识。

                【文档】
                %s

                【问题】
                %s

                请严格基于文档回答：""".formatted(docContext, c.question());

        String docOnly = ChatClient.builder(dashscopeChatModel)
                .build().prompt().user(strictPrompt).call().content();
        log.info("【模式C — 仅限文档内容回答（禁止使用外部知识）】");
        log.info("{}", docOnly.substring(0, Math.min(300, docOnly.length())));

        // 诊断结论
        log.info("");
        log.info("═══════════════════════════════════════════");
        log.info("📊 诊断结论：");
        log.info("如果 A ≈ B（RAG 和纯 LLM 回答相似）  → 检索的文档没起作用，LLM 靠自己的知识在答");
        log.info("如果 B ≈ C（RAG 和纯文档回答相似）    → 检索很好但文档内容就是这些，知识库需要补充");
        log.info("如果 A/B/C 三者都不同                  → RAG 在起作用但 LLM 融合了文档+自身知识");
    }

    /**
     * 诊断 3：你的文档到底有没有 LLM 不知道的信息？
     * 逐段检查文档内容，区分"LLM 已知"和"LLM 不知"
     */
    @Test
    @DisplayName("诊断3 — 文档信息独特性评估")
    void diagnoseDocumentUniqueness() {
        // 从向量库取几段文档，问 LLM "这些信息你本来就知道吗？"
        List<Document> docs = travelAppVectorStore.similaritySearch(
                SearchRequest.builder().query("旅行规划").topK(5).build()
        );

        log.info("");
        log.info("═══════════════════════════════════════════");
        log.info("文档信息独特性评估 — 这些内容 LLM 本来就知道吗？");
        log.info("");

        int knownCount = 0;
        int novelCount = 0;

        for (int i = 0; i < docs.size(); i++) {
            String content = docs.get(i).getText();
            if (content.length() > 300) content = content.substring(0, 300);

            String judgePrompt = """
                    请判断以下【文档片段】中包含的信息，你是否在训练数据中就已经掌握了（即不需要检索也能说出来）？

                    【文档片段】
                    %s

                    只回答一个词：
                    - "已知" — 这些信息我训练时就掌握了，不检索也能说出来
                    - "新知" — 这些信息包含了我训练数据中不常见的具体事实、数据或独特观点

                    回答：""".formatted(content);

            String verdict = ChatClient.builder(dashscopeChatModel)
                    .build().prompt().user(judgePrompt).call().content();

            boolean isNovel = verdict.contains("新知");
            if (isNovel) novelCount++; else knownCount++;

            log.info("文档{}: {} | 内容: {}...",
                    i + 1, isNovel ? "新知 ✨" : "已知 📚",
                    content.substring(0, Math.min(100, content.length())));
        }

        log.info("");
        log.info("已知占比: {}/{}  |  新知占比: {}/{}", knownCount, docs.size(), novelCount, docs.size());

        if (novelCount == 0) {
            log.warn("⚠️ 所有文档内容 LLM 都已经知道 → 这些文档不适合做 RAG 知识库");
            log.warn("⚠️ 建议：将文档内容转为 System Prompt（一次性注入），RAG 留给真正需要检索的数据");
        } else if (novelCount < docs.size() / 2) {
            log.info("🔶 大部分内容 LLM 已知 → RAG 的增值有限");
            log.info("🔶 建议：保留独有的那部分（具体数据、独特观点），删除通用方法论");
        } else {
            log.info("✅ 文档包含较多 LLM 不知道的信息 → RAG 有价值");
        }
    }
}
