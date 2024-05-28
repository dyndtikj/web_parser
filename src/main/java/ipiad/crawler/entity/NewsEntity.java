package ipiad.crawler.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.time.format.DateTimeFormatter;
import java.util.UUID;


public class NewsEntity {
    private String id;
    private String header;
    private String text;
    private String summary;
    private String URL;
    private String date;
    private String time;
    private String hash;

    public NewsEntity(String header, String text, String date, String summary, String URL, String time, String hash) {
        this.id = UUID.randomUUID().toString();
        this.header = header;
        this.text = text;
        this.date = date;
        this.summary = summary;
        this.URL = URL;
        this.time = time;
        this.hash = hash;
    }

    public NewsEntity(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void objectFromStrJson(String jsonData) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonData);
        this.id = node.get("id").asText();
        this.header = node.get("header").asText();
        this.text = node.get("text").asText();
        this.date = node.get("date").asText();
        this.summary = node.get("summary").asText();
        this.URL = node.get("url").asText();
        this.time = node.get("time").asText();
        this.hash = node.get("hash").asText();
    }

    public String toJsonString() throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(this);
    }

    @Override
    public String toString() {
        return "NewsModel{" +
                "id='" + id + '\'' +
                ", header='" + header + '\'' +
                ", text='" + text + '\'' +
                ", date='" + date + '\'' +
                ", summary='" + summary + '\'' +
                ", URL='" + URL + '\'' +
                ", time='" + time + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}
