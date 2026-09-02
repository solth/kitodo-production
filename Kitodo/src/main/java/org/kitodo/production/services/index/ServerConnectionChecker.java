/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.services.index;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.mapper.orm.Search;
import org.kitodo.data.database.persistence.HibernateUtil;

/**
 * Checks the connection to the search service. This is an asynchronous function
 * to {@link IndexingService}. It uses its variables for inter-thread
 * communication.
 */
class ServerConnectionChecker implements Runnable {
    private static final Logger logger = LogManager.getLogger(ServerConnectionChecker.class);
    private static final int WAIT_BETWEEN_CHECKS_SECS = 3;

    private final IndexingService indexingService;

    public ServerConnectionChecker(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @Override
    public void run() {
        if (Objects.nonNull(indexingService.serverInformation) && SECONDS.convert(System.nanoTime()
                - indexingService.serverLastCheck, NANOSECONDS) < WAIT_BETWEEN_CHECKS_SECS) {
            return;
        }

        boolean clearId = false;
        try {
            if (indexingService.serverCheckThreadId == 0) {
                indexingService.serverCheckThreadId = currentThread().threadId();
                if (indexingService.serverCheckThreadId == currentThread().threadId()) {
                    clearId = true;
                    Map<String, String> serverInformation = downloadServerInformation();
                    indexingService.serverInformation = serverInformation.get("logMessage");
                    JsonNode root = new ObjectMapper().readTree(serverInformation.get("responseEntity"));
                    JsonNode versionNode = root.path("version");
                    indexingService.serverVersion = versionNode.path("distribution").asText() + " - " + versionNode.path("number").asText();
                    indexingService.serverLastCheck = System.nanoTime();
                }
            }
        } catch (RuntimeException | JsonProcessingException e) {
            logger.error(e);
            indexingService.serverInformation = "";
            indexingService.serverLastCheck = System.nanoTime();
        } finally {
            if (clearId) {
                indexingService.serverCheckThreadId = 0;
            }
        }
    }

    /**
     * Get search server information.
     */
    private static Map<String, String> downloadServerInformation() {
        HashMap<String, String> serverInformation = new HashMap<>();
        try {
            ElasticsearchBackend elasticsearchBackend = Search.mapping(HibernateUtil.getSession().getSessionFactory())
                    .backend()
                    .unwrap(ElasticsearchBackend.class);
            // do not call close() on restClient as this will terminate the connection to search index
            RestClient restClient = elasticsearchBackend.client(RestClient.class);
            Request request = new Request("GET", "/");
            Response response = restClient.performRequest(request);
            serverInformation.put("statusCode", String.valueOf(response.getStatusLine().getStatusCode()));
            if ("200".equals(serverInformation.get("statusCode"))) {
                String uri = response.getHost().toURI();
                String logMessage = String.format("Connection established to %s", uri);
                logger.info("Search server found: {}", logMessage);
                serverInformation.put("logMessage", logMessage);
                serverInformation.put("uri", uri);
                serverInformation.put("responseEntity", EntityUtils.toString(response.getEntity()));
            } else {
                String message = String.format("Error connecting to Elasticsearch server: %s",
                        response.getStatusLine().getReasonPhrase());
                logger.error(message);
            }
        } catch (IOException e) {
            logger.error("searchServerNotRunning", e);
        }
        return serverInformation;
    }
}
