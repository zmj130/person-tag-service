package com.qianfan.tag.remote;

import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.config.RemotePersonProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** 通过标准 HTTP 游标接口拉取人员变更。 */
@Component
@ConditionalOnProperty(name = "remote-person.mode", havingValue = "http")
public class HttpRemotePersonClient implements RemotePersonClient {
    private final RestTemplate restTemplate;
    private final RemotePersonProperties properties;

    public HttpRemotePersonClient(RestTemplate restTemplate, RemotePersonProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public RemotePersonPage fetchChanges(String cursor, int pageSize) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path("/api/persons/changes")
                .queryParam("cursor", cursor == null ? "" : cursor)
                .queryParam("pageSize", pageSize)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + properties.getToken());
        try {
            ResponseEntity<RemotePersonPage> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<Void>(headers), RemotePersonPage.class);
            if (response.getBody() == null) {
                throw new BusinessException("REMOTE_EMPTY_RESPONSE", "远程人员接口返回空响应");
            }
            return response.getBody();
        } catch (RestClientException ex) {
            throw new BusinessException("REMOTE_REQUEST_FAILED", "远程人员接口调用失败：" + ex.getMessage());
        }
    }
}

