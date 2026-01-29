package xyz.dreature.loat.domain.tool.impl;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 网页工具
@Slf4j
@Component
public class WebTools {
    @Tool("使用搜索引擎搜索网络内容")
    public Map<String, Object> searchWeb(
            @P("搜索关键词或问题") String query,
            @P("返回的搜索结果数量") int numResults
    ) {
        Map<String, Object> result = new HashMap<>();

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://duckduckgo.com/html/?q=" + encodedQuery;

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            List<Map<String, String>> results = new ArrayList<>();
            Elements searchResults = doc.select(".result");

            for (int i = 0; i < Math.min(numResults, searchResults.size()); i++) {
                Element item = searchResults.get(i);
                Element titleElem = item.selectFirst(".result__a");
                Element snippetElem = item.selectFirst(".result__snippet");

                if (titleElem != null) {
                    Map<String, String> resultItem = new HashMap<>();
                    resultItem.put("title", titleElem.text().trim());
                    resultItem.put("link", titleElem.attr("href"));
                    resultItem.put("snippet", snippetElem != null ?
                            snippetElem.text().trim() : "");
                    results.add(resultItem);
                }
            }

            result.put("status", "success");
            result.put("query", query);
            result.put("results", results);
            result.put("count", results.size());

        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", "搜索失败: " + e.getMessage());
        }

        return result;
    }

    @Tool("获取网页内容并提取主要文本")
    public Map<String, Object> fetchWebPageContent(
            @P("要获取内容的网页 URL 地址") String url
    ) {
        // 暂不作抓取长度限制
        int maxLength = Integer.MAX_VALUE;

        Map<String, Object> result = new HashMap<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();

            // 移除不需要的元素
            doc.select("script, style, nav, footer, aside").remove();

            String title = doc.title();
            if (title == null || title.trim().isEmpty()) {
                title = "无标题";
            }

            // 尝试获取主要内容
            String content = "";
            String[] contentSelectors = {
                    "article", "main", ".content", ".post", ".entry",
                    "div[class*=content]", "div[class*=article]"
            };

            for (String selector : contentSelectors) {
                Element contentElem = doc.selectFirst(selector);
                if (contentElem != null) {
                    content = contentElem.text();
                    break;
                }
            }

            if (content.isEmpty()) {
                Element body = doc.selectFirst("body");
                if (body != null) {
                    content = body.text();
                }
            }

            // 限制长度
            if (content.length() > maxLength) {
                content = content.substring(0, maxLength) + "...";
            }

            result.put("status", "success");
            result.put("url", url);
            result.put("title", title.trim());
            result.put("content", content.trim());
            result.put("length", content.length());

        } catch (Exception e) {
            result.put("status", "error");
            result.put("url", url);
            result.put("error", "获取网页内容失败: " + e.getMessage());
        }

        return result;
    }
}
