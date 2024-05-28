package ipiad.crawler;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import ipiad.crawler.services.LinkExtractor;
import ipiad.crawler.services.NewsParser;
import ipiad.crawler.services.NewsLoader;
import ipiad.crawler.services.ElasticStorage;
import ipiad.crawler.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class Main {
    private static final String url = "https://www.vesti.ru/news";
    private static final String INDEX_NAME = "news";
    private static final String EL_URL = "http://localhost:9200";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException {
        logger.info("Start service");
        ConnectionFactory rmqConFactory = new ConnectionFactory();
        rmqConFactory.setHost("127.0.0.1");
        rmqConFactory.setPort(5672);
        rmqConFactory.setVirtualHost("/");
        rmqConFactory.setUsername("rabbitmq");
        rmqConFactory.setPassword("rabbitmq");

        Connection connection = rmqConFactory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(RequestUtils.QUEUE_LINK, false, false, false, null);
        channel.queueDeclare(RequestUtils.QUEUE_PAGE, false, false, false, null);
        channel.close();
        connection.close();

        ElasticStorage elasticStorage = new ElasticStorage(EL_URL, INDEX_NAME);
        elasticStorage.createIndexIfNotExists();

        LinkExtractor linkExtractor = new LinkExtractor(url, rmqConFactory);

        NewsParser newsParser1 = new NewsParser(rmqConFactory, elasticStorage);
        NewsParser newsParser2 = new NewsParser(rmqConFactory, elasticStorage);

        NewsLoader newsLoader1 = new NewsLoader(rmqConFactory, elasticStorage);
        NewsLoader newsLoader2 = new NewsLoader(rmqConFactory, elasticStorage);


        // ---------------запуск---------------------
        linkExtractor.start();

        newsParser1.start();
        newsParser2.start();

        newsLoader1.start();
        newsLoader2.start();

        // ---------------ожидание пока потоки отдадут управление---------------------
        linkExtractor.join();

        newsParser1.join();
        newsParser2.join();

        newsLoader1.join();
        newsLoader2.join();
    }
}