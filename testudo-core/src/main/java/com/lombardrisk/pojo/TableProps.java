package com.lombardrisk.pojo;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public final class TableProps {

    private static final Logger logger = LoggerFactory.getLogger(RequiredTables.class);

    private String name;
    private String typeSize;
    private String nullable;
    private int order;

    public TableProps(String name, String typesize, String nullable, String order) {
        int or = Integer.parseInt(order.replace("col", ""));
        this.setName(name);
        this.setTypeSize(typesize);
        this.setNullable(nullable);
        this.setOrder(or);
    }

    public TableProps(String name, String typesize, String nullable, int order) {
        this.setName(name);
        this.setTypeSize(typesize);
        this.setNullable(nullable);
        this.setOrder(order);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                String value = null;
                Object obj = field.get(this);
                if (obj == null || StringUtils.isBlank(obj.toString())) {
                    continue;
                } else {
                    value = field.get(this).toString();
                }
                stringBuilder.append(field.getName() + "[" + value + "] ");
            } catch (IllegalArgumentException|IllegalAccessException e) {
                logger.error("Unable to display TableProps", e);
            }
        }
        return stringBuilder.toString();
    }

    public String getTypeSize() {
        return typeSize;
    }

    public void setTypeSize(String typeSize) {
        this.typeSize = typeSize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNullable() {
        return nullable;
    }

    public void setNullable(String nullable) {
        this.nullable = nullable;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
