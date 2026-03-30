package com.tictactoer.backend.game.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.List;

@Converter
public class IntegerListJsonConverter implements AttributeConverter<List<Integer>, PGobject> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Integer>> TYPE_REF = new TypeReference<>() {};

    @Override
    public PGobject convertToDatabaseColumn(List<Integer> attribute) {
        if (attribute == null) return null;
        try {
            String json = OBJECT_MAPPER.writeValueAsString(attribute);
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(json);
            return pgObject;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Nie udało się zamienić List<Integer> na JSON", e);
        } catch (org.postgresql.util.PSQLException e) {
            throw new IllegalArgumentException("Nie udało się ustawić typu PGobject dla jsonb", e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Integer> convertToEntityAttribute(PGobject dbData) {
        if (dbData == null) return null;
        try {
            return OBJECT_MAPPER.readValue(dbData.getValue(), TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Nie udało się zamienić JSON na List<Integer>", e);
        }
    }
}

