package ipiad.crawler.services;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import ipiad.crawler.entity.NewsEntity;
import ipiad.crawler.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NewsLoader extends Thread {
    private final ConnectionFactory rmqFactory;
    private final ElasticStorage elasticStorage;
    private static final Logger logger = LoggerFactory.getLogger(NewsLoader.class);

    public NewsLoader(ConnectionFactory factory, ElasticStorage elasticStorage) {
        this.rmqFactory = factory;
        this.elasticStorage = elasticStorage;
    }

    @Override
    public void run() {
        try {
            Connection connection = rmqFactory.newConnection();
            Channel channel = connection.createChannel();
            logger.info("PageLoader connected to RabbitMQ");
            while (true) {
                GetResponse rmqResp = channel.basicGet(RequestUtils.QUEUE_PAGE, true);
                if (rmqResp == null) {
                    // basic get не смог ничего вычитать
                    continue;
                }
                String newsJson = new String(rmqResp.getBody(),UTF_8);
                NewsEntity newsEntity = new NewsEntity();
                logger.info("Got parsed data to insert" + newsJson);
                newsEntity.objectFromStrJson(newsJson);
                if (!elasticStorage.checkExistence(newsEntity.getHash())) {
                    elasticStorage.insertData(newsEntity);
                    logger.info("Inserted data from " + newsEntity.getURL() + " into Elastic");
                } else {
                    logger.info("[!] URL: " + newsEntity.getURL() + " was found in Elastic. Hash: " + newsEntity.getHash());
                }
            }
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
