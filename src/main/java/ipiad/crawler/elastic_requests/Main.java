package ipiad.crawler.elastic_requests;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhrasePrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import ipiad.crawler.entity.NewsEntity;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class Main {
    private static final String EL_URL = "http://localhost:9200";
    private static final String INDEX_NAME = "news";
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = JsonMapper.builder().build();
        RestClient restClient = RestClient
                .builder(HttpHost.create(EL_URL)).build();
        ElasticsearchTransport elasticsearchTransport = new RestClientTransport(restClient,
                new JacksonJsonpMapper(mapper));
        ElasticsearchClient elasticsearchClient = new ElasticsearchClient(elasticsearchTransport);
        logger.info("Elastic connection success!");

        Query summaryMatch = MatchQuery.of(m -> m
                .field("summary")
                .query("Китай")
        )._toQuery();

        Query dateMatch = MatchPhrasePrefixQuery.of(m -> m
                .field("date")
                .query("27 мая")
        )._toQuery();


        // AND
        SearchResponse<NewsEntity> andResponse = elasticsearchClient.search(s -> s
                        .index(INDEX_NAME)
                        .query(q -> q
                                .bool(b -> b
                                        .must(summaryMatch,dateMatch )
                                )
                        ),
                NewsEntity.class
        );
        printQuery(andResponse, "AND");

        // OR
        SearchResponse<NewsEntity> orResponse = elasticsearchClient.search(s -> s
                        .index(INDEX_NAME)
                        .query(q -> q
                                .bool(b -> b
                                        .should(summaryMatch, dateMatch)
                                )
                        ),
                NewsEntity.class
        );
        printQuery(orResponse, "OR");
    }


    public static void printQuery(SearchResponse<NewsEntity> response, String responseType) {
        List<Hit<NewsEntity>> hits = response.hits().hits();

        if (hits.isEmpty()) {
            logger.warn("Empty " + responseType + " response");
            return;
        }

        logger.info("Search results for <" + responseType + ">");

        for (Hit<NewsEntity> hit: hits) {
            NewsEntity NewsEntity = hit.source();
            assert NewsEntity != null;
            logger.debug(NewsEntity.toString());
        }
    }
}


