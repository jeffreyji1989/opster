package com.opster.module.project.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opster.module.project.dto.RepositoryDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 仓库列表 JSON 转换器
 * 用于 JPA 实体类中 List<RepositoryDTO> 与数据库 JSON 字符串的相互转换
 */
@Converter
public class RepositoriesConverter implements AttributeConverter<List<RepositoryDTO>, String> {

    private static final Logger log = LoggerFactory.getLogger(RepositoriesConverter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<RepositoryDTO> repositories) {
        if (repositories == null || repositories.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(repositories);
        } catch (JsonProcessingException e) {
            log.error("仓库列表转换为 JSON 失败", e);
            return "[]";
        }
    }

    @Override
    public List<RepositoryDTO> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<RepositoryDTO>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON 转换为仓库列表失败: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}
