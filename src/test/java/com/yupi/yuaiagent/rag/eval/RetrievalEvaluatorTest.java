package com.yupi.yuaiagent.rag.eval;

import com.yupi.yuaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 检索质量评估器
 * <p>
 * 评估维度：
 * 1. HitRate@K  — 前K个结果中至少命中一个相关文档的问题占比（最直观）
 * 2. MRR@K      — 第一个相关文档排名的倒数均值（衡量排序质量）
 * 3. Recall@K   — 期望关键词被检索到的比例（衡量覆盖度）
 * <p>
 * 此外对比查询重写前后的效果差异，并可通过网格搜索找到最优 topK + similarityThreshold 参数组合。
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class RetrievalEvaluatorTest {

    @Resource
    private VectorStore travelAppVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    // ==================== 核心检索方法 ====================

    /**
     * 以指定参数检索向量库
     */
    private List<Document> search(String query, int topK, double similarityThreshold) {
        return travelAppVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );
    }

    /**
     * 判断单个文档是否与任一期望关键词相关
     * 使用简单的子串匹配（中文分词场景下基本够用）
     */
    private boolean isRelevant(Document doc, List<String> keywords) {
        String text = doc.getText();
        return keywords.stream().anyMatch(text::contains);
    }

    // ==================== 核心指标计算 ====================

    /**
     * HitRate@K — 前K个结果中至少有一个相关文档的问题比例
     * 这是业务上最直观的指标：用户问的问题能不能搜到正确文档
     */
    private double computeHitRate(List<EvalDataset.EvalCase> cases, int k, double threshold) {
        int hits = 0;
        for (EvalDataset.EvalCase c : cases) {
            List<Document> results = search(c.question(), k, threshold);
            boolean anyRelevant = results.stream().anyMatch(doc -> isRelevant(doc, c.expectedKeywords()));
            if (anyRelevant) hits++;
        }
        return (double) hits / cases.size();
    }

    /**
     * MRR (Mean Reciprocal Rank) — 第一个相关文档排名的倒数均值
     * 衡量排序质量：正确答案排得越靠前得分越高
     * 排名第1得1.0，第2得0.5，第3得0.33...完全没命中得0
     */
    private double computeMRR(List<EvalDataset.EvalCase> cases, int k, double threshold) {
        double sum = 0;
        for (EvalDataset.EvalCase c : cases) {
            List<Document> results = search(c.question(), k, threshold);
            double rr = 0;
            for (int i = 0; i < results.size(); i++) {
                if (isRelevant(results.get(i), c.expectedKeywords())) {
                    rr = 1.0 / (i + 1);
                    break;
                }
            }
            sum += rr;
        }
        return sum / cases.size();
    }

    /**
     * Recall@K — 期望关键词中被检索到的比例
     * 衡量召回覆盖度：期望的文档内容有没有被搜出来
     */
    private double computeRecall(List<EvalDataset.EvalCase> cases, int k, double threshold) {
        double sum = 0;
        for (EvalDataset.EvalCase c : cases) {
            List<Document> results = search(c.question(), k, threshold);
            long found = c.expectedKeywords().stream()
                    .filter(kw -> results.stream().anyMatch(doc -> doc.getText().contains(kw)))
                    .count();
            sum += (double) found / c.expectedKeywords().size();
        }
        return sum / cases.size();
    }

    // ==================== 逐条详情打印 ====================

    /**
     * 打印每条用例的检索详情，便于定位哪些问题检索效果差
     */
    private void printDetailPerCase(List<EvalDataset.EvalCase> cases, int k, double threshold) {
        log.info("");
        log.info("========== 逐条检索详情 (topK={}, threshold={}) ==========", k, threshold);
        for (int i = 0; i < cases.size(); i++) {
            EvalDataset.EvalCase c = cases.get(i);
            List<Document> results = search(c.question(), k, threshold);

            long matchCount = results.stream()
                    .filter(doc -> isRelevant(doc, c.expectedKeywords()))
                    .count();
            boolean hit = matchCount > 0;

            // 找到第一个相关文档的排名
            int firstRank = -1;
            for (int j = 0; j < results.size(); j++) {
                if (isRelevant(results.get(j), c.expectedKeywords())) {
                    firstRank = j + 1;
                    break;
                }
            }

            String questionPreview = c.question().length() > 30
                    ? c.question().substring(0, 30) + "..."
                    : c.question();

            if (hit) {
                log.info(String.format("[✓] #%2d 排名=%s 命中=%d/%d | %s",
                        i + 1, firstRank, matchCount, k, questionPreview));
            } else {
                log.info(String.format("[✗] #%2d 排名=- 命中=0/%d   | %s",
                        i + 1, k, questionPreview));
                log.info("      期望关键词: {}", c.expectedKeywords().stream()
                        .limit(5).collect(Collectors.joining(", ")));
            }
        }
    }

    // ==================== 查询重写效果对比 ====================

    /**
     * 对比查询重写前后的检索效果
     * 检验 RewriteQueryTransformer 在当前场景下是否正向提升检索质量
     */
    private void compareRewriteEffect(List<EvalDataset.EvalCase> cases, int k, double threshold) {
        // 原始查询
        double originalHit = computeHitRate(cases, k, threshold);
        double originalMRR = computeMRR(cases, k, threshold);
        double originalRecall = computeRecall(cases, k, threshold);

        // 用改写后的查询重新评估（LLM 调用，每条约 1-3s）
        List<EvalDataset.EvalCase> rewrittenCases = new ArrayList<>();
        int successCount = 0;
        for (int i = 0; i < cases.size(); i++) {
            EvalDataset.EvalCase c = cases.get(i);
            try {
                log.info("查询重写中 [{}/{}]: {}", i + 1, cases.size(),
                        c.question().substring(0, Math.min(20, c.question().length())) + "...");
                String rewritten = queryRewriter.doQueryRewrite(c.question());
                rewrittenCases.add(new EvalDataset.EvalCase(rewritten, c.expectedKeywords(), c.referencePoints()));
                successCount++;
            } catch (Exception e) {
                log.warn("查询重写失败 [{}/{}]: {} — 回退使用原始查询", i + 1, cases.size(), e.getMessage());
                rewrittenCases.add(c); // 失败时回退到原始查询
            }
        }
        log.info("查询重写完成: {}/{} 条成功", successCount, cases.size());
        double rewrittenHit = computeHitRate(rewrittenCases, k, threshold);
        double rewrittenMRR = computeMRR(rewrittenCases, k, threshold);
        double rewrittenRecall = computeRecall(rewrittenCases, k, threshold);

        String hitChange = rewrittenHit >= originalHit
                ? "↑ +" + String.format("%.1f%%", (rewrittenHit - originalHit) * 100)
                : "↓ " + String.format("%.1f%%", (rewrittenHit - originalHit) * 100);

        log.info("");
        log.info("========== 查询重写效果对比 (topK={}, threshold={}) ==========", k, threshold);
        log.info("指标          | 原始查询  | 改写查询  | 变化");
        log.info("HitRate@{}    | {}   | {}   | {}",
                k, fmtPct(originalHit), fmtPct(rewrittenHit), hitChange);
        log.info("MRR@{}        | {}  | {}  | {}",
                k, fmt4(originalMRR), fmt4(rewrittenMRR),
                rewrittenMRR >= originalMRR ? "↑" : "↓");
        log.info("Recall@{}     | {}   | {}   | {}",
                k, fmtPct(originalRecall), fmtPct(rewrittenRecall),
                rewrittenRecall >= originalRecall ? "↑" : "↓");

        if (rewrittenHit < originalHit && rewrittenMRR < originalMRR) {
            log.warn("⚠️ 查询重写在当前场景下降低了检索效果，建议检查 RewriteQueryTransformer 的 Prompt 是否适配旅游领域");
        }
    }

    // ==================== 参数网格搜索 ====================

    /**
     * 在 topK 和 similarityThreshold 的网格上搜索最优参数组合
     * 以 MRR 为主要优化目标（兼顾命中率和排序质量）
     */
    private void gridSearchBestParams(List<EvalDataset.EvalCase> cases) {
        int[] kValues = {1, 2, 3, 5, 7, 10};
        double[] thresholds = {0.3, 0.4, 0.5, 0.6, 0.7, 0.8};

        log.info("");
        log.info("========== 参数网格搜索（按 MRR@K 排序 Top 5） ==========");
        log.info(String.format("%-10s %-8s %-12s %-12s %-12s",
                "topK", "threshold", "HitRate", "MRR", "Recall"));

        record GridResult(int k, double t, double hitRate, double mrr, double recall) {}
        List<GridResult> allResults = new ArrayList<>();

        for (int k : kValues) {
            for (double t : thresholds) {
                double hitRate = computeHitRate(cases, k, t);
                double mrr = computeMRR(cases, k, t);
                double recall = computeRecall(cases, k, t);
                allResults.add(new GridResult(k, t, hitRate, mrr, recall));
            }
        }

        // 按 MRR 降序排列
        allResults.sort((a, b) -> Double.compare(b.mrr, a.mrr));

        for (int i = 0; i < Math.min(5, allResults.size()); i++) {
            GridResult r = allResults.get(i);
            String marker = i == 0 ? " ← 最优" : "";
            log.info(String.format("%-10d %-8.1f %-12s %-12s %-12s%s",
                    r.k, r.t, fmtPct(r.hitRate), fmt4(r.mrr), fmtPct(r.recall), marker));
        }
    }

    // ==================== 分场景评估 ====================

    /**
     * 按知识库文档类型分组评估，识别哪个领域的检索效果最差
     */
    private void evaluateByCategory(List<EvalDataset.EvalCase> cases, int k, double threshold) {
        log.info("");
        log.info("========== 分场景检索效果 (topK={}, threshold={}) ==========", k, threshold);
        log.info(String.format("%-12s %-8s %-10s %-10s %-10s",
                "知识库", "用例数", "HitRate", "MRR", "Recall"));

        // 按用例索引对应知识库文档（路线模板 0-7，景点数据 8-14，花期节庆 15-19）
        String[] categories = {"路线模板", "景点数据", "花期节庆"};
        int[] groupSizes = {8, 7, 5};
        int offset = 0;
        for (int g = 0; g < categories.length; g++) {
            int start = offset;
            int end = Math.min(start + groupSizes[g], cases.size());
            offset = end;
            List<EvalDataset.EvalCase> group = cases.subList(start, end);

            double hit = computeHitRate(group, k, threshold);
            double mrr = computeMRR(group, k, threshold);
            double recall = computeRecall(group, k, threshold);

            log.info(String.format("%-12s %-8d %-10s %-10s %-10s",
                    categories[g], group.size(), fmtPct0(hit), fmt4(mrr), fmtPct0(recall)));
        }
    }

    // ==================== 运行入口 ====================

    @Test
    @Order(1)
    @DisplayName("RAG 检索质量 — 基础指标")
    void evaluateBasicMetrics() {
        List<EvalDataset.EvalCase> cases = EvalDataset.ALL_CASES;
        int k = 3;
        double threshold = 0.4; // 用较低阈值起步，避免过度过滤

        log.info("");
        log.info("╔══════════════════════════════════════╗");
        log.info("║   RAG 检索质量 — 基础指标           ║");
        log.info("╚══════════════════════════════════════╝");
        log.info("评测用例总数: {}", cases.size());
        log.info("检索参数: topK={}, similarityThreshold={}", k, threshold);
        log.info("");

        double hitRate = computeHitRate(cases, k, threshold);
        double mrr = computeMRR(cases, k, threshold);
        double recall = computeRecall(cases, k, threshold);

        // ── 指标 ──
        log.info("┌────────────────────────────────────────┐");
        log.info("│ 指标           │ 值        │ 参考目标   │");
        log.info("├────────────────────────────────────────┤");
        log.info("│ HitRate@{}      │ {}    │ > 80%      │", k, fmtPct0(hitRate));
        log.info("│ MRR@{}          │ {}   │ > 0.60     │", k, fmt4(mrr));
        log.info("│ Recall@{}       │ {}    │ > 70%      │", k, fmtPct0(recall));
        log.info("└────────────────────────────────────────┘");

        // ── 诊断 ──
        log.info("");
        log.info("🔍 诊断：");
        if (hitRate >= 0.8 && mrr >= 0.6) {
            log.info("✅ 检索质量良好，HitRate 和 MRR 均达标");
        } else if (hitRate >= 0.8 && mrr < 0.6) {
            log.info("⚠️ 能搜到但排序不佳 — 正确文档排名偏后，考虑调整 embedding 或增加 topK");
        } else if (hitRate < 0.5) {
            log.info("❌ HitRate 偏低 — 过半问题搜不到正确文档，建议排查：");
            log.info("   1. 嵌入模型是否适合中文旅游领域文本？");
            log.info("   2. 文档切分粒度是否合理（尝试开启 MyTokenTextSplitter）？");
            log.info("   3. 查询重写是否在帮倒忙（运行 compareRewriteEffect 验证）？");
        } else {
            log.info("🔶 检索效果一般 — 运行网格搜索找到最优参数组合");
        }

        printDetailPerCase(cases, k, threshold);
    }

    @Test
    @Order(2)
    @DisplayName("查询重写效果对比")
    void evaluateRewriteEffect() {
        compareRewriteEffect(EvalDataset.ALL_CASES, 3, 0.4);
    }

    @Test
    @Order(3)
    @DisplayName("参数网格搜索 — 寻找最优 topK 和 similarityThreshold")
    void evaluateGridSearch() {
        gridSearchBestParams(EvalDataset.ALL_CASES);
    }

    @Test
    @Order(4)
    @DisplayName("分场景评估 — 识别各知识库检索效果差异")
    void evaluateByCategory() {
        evaluateByCategory(EvalDataset.ALL_CASES, 3, 0.4);
    }

    @Test
    @Order(5)
    @DisplayName("全量综合报告")
    @org.junit.jupiter.api.Timeout(value = 5, unit = java.util.concurrent.TimeUnit.MINUTES)
    void evaluateFullReport() {
        List<EvalDataset.EvalCase> cases = EvalDataset.ALL_CASES;

        log.info("");
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║          RAG 检索质量 — 综合评估报告             ║");
        log.info("╚══════════════════════════════════════════════════╝");

        // 当前参数
        int currentK = 3;
        double currentThreshold = 0.4;

        log.info("📊 当前参数: topK={}, similarityThreshold={}", currentK, currentThreshold);
        log.info("📋 评测用例: {} 条（覆盖5篇知识库文档）", cases.size());

        // 基础指标
        double hitRate = computeHitRate(cases, currentK, currentThreshold);
        double mrr = computeMRR(cases, currentK, currentThreshold);
        double recall = computeRecall(cases, currentK, currentThreshold);

        log.info("");
        log.info("┌──────────────────────────────────────────────────┐");
        log.info("│              检索质量总分卡                      │");
        log.info("├──────────────┬─────────┬──────────┬──────────────┤");
        log.info("│ 指标         │   实测值 │   目标值  │   评级       │");
        log.info("├──────────────┼─────────┼──────────┼──────────────┤");
        log.info("│ HitRate@{}    │  {}  │  > 80%   │  {}          │",
                currentK, fmtPct0(hitRate), grade(hitRate, 0.8));
        log.info("│ MRR@{}        │ {}  │  > 0.60  │  {}          │",
                currentK, fmt4(mrr), grade(mrr, 0.6));
        log.info("│ Recall@{}     │  {}  │  > 70%   │  {}          │",
                currentK, fmtPct0(recall), grade(recall, 0.7));
        log.info("└──────────────┴─────────┴──────────┴──────────────┘");

        // 分场景
        log.info("");
        evaluateByCategory(cases, currentK, currentThreshold);

        // 查询重写
        compareRewriteEffect(cases, currentK, currentThreshold);

        log.info("");
        log.info("💡 下一步建议：");
        if (hitRate < 0.6) {
            log.info("   1. 启用 MyTokenTextSplitter 调整文档切分粒度");
            log.info("   2. 检查文档 embedding 是否成功生成");
            log.info("   3. 考虑替换嵌入模型（当前用 DashScope，可对比 Ollama）");
        }
        if (mrr < 0.5) {
            log.info("   4. 增大 topK（当前为{}），尝试5或7", currentK);
            log.info("   5. 降低 similarityThreshold（当前为{}）避免过度过滤", currentThreshold);
        }
        log.info("   6. 运行 GridSearch 找到最优参数后更新 TravelAppRagCustomAdvisorFactory");
    }

    // ==================== 格式化辅助方法 ====================

    /** 百分比 1 位小数，如 "85.0%" */
    private static String fmtPct(double v) {
        return String.format("%.1f%%", v * 100);
    }

    /** 百分比 0 位小数，如 "85%" */
    private static String fmtPct0(double v) {
        return String.format("%.0f%%", v * 100);
    }

    /** 4 位小数，如 "0.9500" */
    private static String fmt4(double v) {
        return String.format("%.4f", v);
    }

    /**
     * 评级辅助方法
     */
    private String grade(double actual, double target) {
        if (actual >= target) return "✅ 达标";
        if (actual >= target * 0.8) return "🔶 接近";
        return "❌ 偏低";
    }
}
