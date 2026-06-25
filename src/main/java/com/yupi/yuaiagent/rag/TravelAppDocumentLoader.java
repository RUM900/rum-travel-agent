package com.yupi.yuaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 旅行规划大师应用文档加载器
 */
@Component
@Slf4j
public class TravelAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    public TravelAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇 Markdown 文档
     * @return
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                // 根据文件名推断文档分类标签
                String category = inferCategory(filename);
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("category", category)
                        .build();
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader.get());
            }
        } catch (IOException e) {
           log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }

    /**
     * 根据文件名推断文档分类
     */
    private String inferCategory(String filename) {
        if (filename == null) return "通用";
        if (filename.contains("路线") || filename.contains("模板")) return "路线模板";
        if (filename.contains("景点") || filename.contains("数据")) return "景点数据";
        if (filename.contains("花期") || filename.contains("节庆") || filename.contains("日历")) return "花期节庆";
        if (filename.contains("平替") || filename.contains("小众")) return "小众平替";
        // 兼容旧格式：旅游知识库 - XX篇.md
        if (filename.contains("篇")) {
            int endIdx = filename.indexOf("篇");
            if (endIdx >= 2) return filename.substring(endIdx - 2, endIdx);
        }
        return "通用";
    }
}
