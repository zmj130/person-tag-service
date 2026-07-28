package com.qianfan.tag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "profile-search")
public class ProfileSearchProperties {
    private boolean enabled;
    private String url = "http://127.0.0.1:9200";
    private String index = "person_tag_resume_demo_v1";
    private String username;
    private String password;
    private int maxDocuments = 5000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getIndex() { return index; }
    public void setIndex(String index) { this.index = index; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getMaxDocuments() { return maxDocuments; }
    public void setMaxDocuments(int maxDocuments) { this.maxDocuments = maxDocuments; }
}
