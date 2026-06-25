package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * 旅行规划大师向量数据库配置（基于 PGVector/内存 向量存储）
 */
@Configuration
public class TravelAppVectorStoreConfig {

    @Resource
    private TravelAppDocumentLoader travelAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    // ==================== 基于内存的向量存储 ====================
    // 如需切换回内存模式：
    //   1. 注释掉下面的 PGVector Bean
    //   2. 取消此方法的注释
    //   3. 在 YuAiAgentApplication 中排除 DataSourceAutoConfiguration
    //   4. 在 pom.xml 中注释掉 spring-ai-pgvector-store 依赖
    @Bean
    VectorStore travelAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        List<Document> documentList = travelAppDocumentLoader.loadMarkdowns();
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
        simpleVectorStore.add(enrichedDocuments);
        return simpleVectorStore;
    }

    // ==================== 基于 PGVector 的向量存储（当前使用） ====================
//    @Bean
//    VectorStore travelAppVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
//        PgVectorStore store = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
//                .dimensions(1024)
//                .distanceType(COSINE_DISTANCE)
//                .indexType(HNSW)
//                .initializeSchema(true)    // 首次自动建表
//                .schemaName("public")
//                .vectorTableName("vector_store")
//                .build();
//        // 手动触发建表：afterPropertiesSet 由 Spring 容器在 Bean 初始化完成后回调，
//        // 但此处需要在此之前执行 add() 写入文档，所以显式调用先建表
//        store.afterPropertiesSet();
//        List<Document> documentList = travelAppDocumentLoader.loadMarkdowns();
//        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
//        // 手动分批写入，避免 DashScope Embedding API 单次 10 条限制
//        int batchSize = 6;
//        for (int i = 0; i < enrichedDocuments.size(); i += batchSize) {
//            int end = Math.min(i + batchSize, enrichedDocuments.size());
//            store.add(enrichedDocuments.subList(i, end));
//        }
//        return store;
//    }
}
