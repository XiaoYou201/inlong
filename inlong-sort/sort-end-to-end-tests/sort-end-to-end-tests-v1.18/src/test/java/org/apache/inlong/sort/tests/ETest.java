package org.apache.inlong.sort.tests;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class ETest {
    private static ElasticsearchContainer container;
    private static RestHighLevelClient client;

    @BeforeAll
    public static void setUp() throws IOException {
        container = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.16.3"));
        container.start();

        // 创建 RestHighLevelClient
        client = new RestHighLevelClient(RestClient.builder(container.getHttpHostAddress()));
    }

    @AfterAll
    public static void tearDown() {
        // 停止 Elasticsearch 容器
        container.stop();
    }

    @Test
    public void testIndexAndSearch() throws IOException {
        // 创建一个包含 id, name 和 description 字段的文档
        Map<String, Object> document = new HashMap<>();
        document.put("id", 1);
        document.put("name", "Test Name");
        document.put("description", "This is a test description.");

        // 索引文档
        IndexRequest indexRequest = new IndexRequest("test");
        indexRequest.id("1");
        indexRequest.source(document);
        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
        assertEquals("1", indexResponse.getId());

        // 搜索刚刚索引的文档
        SearchRequest searchRequest = new SearchRequest("test");
        searchRequest.source(new SearchSourceBuilder().query(QueryBuilders.termQuery("name", "Test Name")));
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] searchHits = searchResponse.getHits().getHits();
        assertEquals(1, searchHits.length);
        assertEquals("1", searchHits[0].getId());
        assertEquals("Test Name", searchHits[0].getSourceAsMap().get("name"));
        assertEquals("This is a test description.", searchHits[0].getSourceAsMap().get("description"));
    }
}
