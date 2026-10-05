package com.sicmagroup.gpr.domain.converter;

import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Chiffre une colonne texte à l'écriture et la déchiffre à la lecture.
 * Usage : @Convert(converter = EncryptedStringConverter.class)
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return FieldEncryptor.current().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return FieldEncryptor.current().decrypt(dbData);
    }
}
