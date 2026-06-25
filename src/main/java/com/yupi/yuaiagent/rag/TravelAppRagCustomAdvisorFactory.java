package com.yupi.yuaiagent.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 创建自定义的 RAG 检索增强顾问的工厂（旅行规划场景）
 * 支持按分类维度过滤：路线模板、景点数据、花期节庆、小众平替
 */
public class TravelAppRagCustomAdvisorFactory {

    /**
     * 创建自定义的 RAG 检索增强顾问
     *
     * @param vectorStore 向量存储
     * @param category   分类维度（路线模板/景点数据/花期节庆/小众平替）
     * @return 自定义的 RAG 检索增强顾问
     */
    public static Advisor createTravelAppRagCustomAdvisor(VectorStore vectorStore, String category) {
        // 过滤特定分类的文档
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("category", category)
                .build();
        // 创建文档检索器
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression) // 过滤条件
                .similarityThreshold(0.5) // 相似度阈值
                .topK(3) // 返回文档数量
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(TravelAppContextualQueryAugmenterFactory.createInstance())
                .build();
    }
}
