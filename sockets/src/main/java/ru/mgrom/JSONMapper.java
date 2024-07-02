package ru.mgrom;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONMapper extends ObjectMapper {
    public <T> T read(String content, Class<T> valueType) {
        try {
            return this.reader().readValue(content, valueType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String write(Object value) {
        try {
            return this.writer().writeValueAsString(value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
