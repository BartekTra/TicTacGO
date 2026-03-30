package com.tictactoer.backend.game.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter
public class StringArrayJsonConverter implements AttributeConverter<String[], PGobject> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public PGobject convertToDatabaseColumn(String[] attribute) {
        if (attribute == null) return null;
        try {
            String json = OBJECT_MAPPER.writeValueAsString(attribute);
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(json);
            return pgObject;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Nie udało się zamienić tablicy String[] na JSON", e);
        } catch (org.postgresql.util.PSQLException e) {
            throw new IllegalArgumentException("Nie udało się ustawić typu PGobject dla jsonb", e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] convertToEntityAttribute(PGobject dbData) {
        if (dbData == null) return null;
        try {
            return OBJECT_MAPPER.readValue(dbData.getValue(), String[].class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Nie udało się zamienić JSON na tablicę String[]", e);
        }
    }
}

