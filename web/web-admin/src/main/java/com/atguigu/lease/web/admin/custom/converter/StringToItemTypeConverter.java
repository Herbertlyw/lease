package com.atguigu.lease.web.admin.custom.converter;

import com.atguigu.lease.model.enums.ItemType;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToItemTypeConverter implements Converter<String, ItemType> {
    ItemType itemType;

    @Override
    public ItemType convert(String s) {
        ItemType[] values = ItemType.values();
        for (ItemType value : values) {
            if (value.getCode().equals(Integer.valueOf(s))) {
                return value;
            }
        }
        throw new RuntimeException("没有此类型");
    }
}

