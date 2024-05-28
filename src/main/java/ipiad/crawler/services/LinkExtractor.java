package ipiad.crawler.services;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import ipiad.crawler.entity.URLEntity;
import ipiad.crawler.utils.RequestUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class LinkExtractor extends Thread {
    private final String initialLink;
    private final ConnectionFactory connectionFactory;
    private static final Logger logger = LoggerFactory.getLogger(LinkExtractor.class);

    public LinkExtractor(String initialLink, ConnectionFactory factory) {
        this.initialLink = initialLink;
        this.connectionFactory = factory;
    }

    @Override
    public void run() {
        try {
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            logger.info("Connected to RabbitMQ link queue");
            crawlAndExtractLinks(initialLink, channel);
            logger.info("Finished crawling");
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private void crawlAndExtractLinks(String url, Channel channel) throws IOException {
        logger.info("Starting crawling at " + url);
        Optional<Document> documentOpt = RequestUtils.requestWithRetry(url);
        if (documentOpt.isPresent()) {
            Document originalDocument = documentOpt.get();
            for (Element publicationDiv : originalDocument.select("div.list__item")) {
                Element anchorElement = publicationDiv.children().select("a").first();
                try {
                    String baseUrl = "https://www.vesti.ru/";
                    String href = baseUrl + anchorElement.attributes().get("href");
                    String title = publicationDiv.select("h3.list__title").text();
                    URLEntity extractedUrl = new URLEntity(href, title);
                    logger.info(extractedUrl.toJsonString());
                    channel.basicPublish("", RequestUtils.QUEUE_LINK, null, extractedUrl.toJsonString().getBytes());
                } catch (Exception e) {
                    System.out.println(e);
                    logger.info(e.getMessage());
                }
            }
        }
    }
}