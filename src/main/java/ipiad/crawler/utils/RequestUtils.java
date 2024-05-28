package ipiad.crawler.utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class RequestUtils {
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int TIME_TO_RETRY_MS = 3000;
    public static final String QUEUE_LINK = "crawler_link";
    public static final String QUEUE_PAGE = "crawler_news";
    private static final Logger logger = LoggerFactory.getLogger(RequestUtils.class);

    public static Optional<Document> requestWithRetry(String url) throws IOException {
        Optional<Document> doc = Optional.empty();
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                final HttpGet httpGet = new HttpGet(url);
                // сайт без заголовков не отдавал html страницу
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
                httpGet.setHeader("Sec-Ch-Ua", """
                        Google Chrome";v="125", "Chromium";v="125", "Not.A/Brand";v="24"
                        """);
                httpGet.setHeader("Sec-Ch-Ua-Platform","macOS");
                httpGet.setHeader("sec-ch-ua-mobile","?0");
                httpGet.setHeader("Referer", "https://www.google.com/");
                httpGet.setHeader("Upgrade-Insecure-Requests", "1");


                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    switch (statusCode) {
                        case 200: {
                            HttpEntity entity = response.getEntity();
                            if (entity != null) {
                                doc = Optional.ofNullable(Jsoup.parse(entity.getContent(), "UTF-8", url));
                                logger.info("[*] Thread ID: " + Thread.currentThread().getId() + " - Got page for: " + url);
                                return doc;
                            }
                            break;
                        }
                        case 429: {
                            logger.info("[*] Thread ID: " + Thread.currentThread().getId() +
                                    " - Error at " + url + " with status code " + statusCode);
                            response.close();
                            httpClient.close();
                            logger.info("[!] Got 429 brute err, sleep for " + TIME_TO_RETRY_MS +" ms");
                            Thread.sleep(TIME_TO_RETRY_MS);
                            break;
                        }
                        case 404:
                            logger.info("[*] Thread ID: " + Thread.currentThread().getId() + " - Got 404 for " + url);
                            break;
                        default:
                            break;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return doc;
    }
}