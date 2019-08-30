package com.swingfrog.summer.db.repository;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TableValueBuilder {

    private static final Logger log = LoggerFactory.getLogger(TableValueBuilder.class);

    public static Object convert(Object obj, Type target) {
        if (TableSupport.isJavaBean(target)) {
            return JSON.toJSONString(obj);
        } else {
            return obj;
        }
    }

    public static Object getFieldValue(Field field, Object obj) {
        Object res = null;
        try {
            res = field.get(obj);
            res = convert(res, field.getGenericType());
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
        return res;
    }

    public static void jsonConvertJavaBean(Field field, Object obj) {
        try {
            field.set(obj, JSON.parseObject(JSON.toJSONString(field.get(obj)), field.getGenericType()));
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static boolean isEqulsColumnValue(TableMeta.ColumnMeta columnMeta, Object obj, Object value) {
        Object columnValue = getColumnValue(columnMeta, obj);
        if (columnValue.equals(value)) {
            return true;
        }
        return columnValue.toString().equals(value.toString());
    }

    public static Object getColumnValue(TableMeta.ColumnMeta columnMeta, Object obj) {
        return getFieldValue(columnMeta.getField(), obj);
    }

    public static Object getPrimaryKeyValue(TableMeta tableMeta, Object obj) {
        return getColumnValue(tableMeta.getPrimaryColumn(), obj);
    }

    public static void setPrimaryKeyIntNumberValue(TableMeta tableMeta, Object obj, long primaryValue) {
        Field field = tableMeta.getPrimaryColumn().getField();
        Type type = field.getGenericType();
        try {
            if (type == long.class || type == Long.class) {
                field.setLong(obj, primaryValue);
            } else if (type == int.class || type == Integer.class) {
                field.setInt(obj, (int) primaryValue);
            } else if (type == short.class || type == Short.class) {
                field.setShort(obj, (short) primaryValue);
            } else if (type == byte.class || type == Byte.class) {
                field.setByte(obj, (byte) primaryValue);
            } else {
                throw new UnsupportedOperationException("primary key must be number");
            }
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static List<String> listValidFieldByOptional(TableMeta tableMeta, Map<String, Object> optional) {
        return optional.keySet().stream().filter(tableMeta.getColumnMetaMap()::containsKey).collect(Collectors.toList());
    }

    public static Object[] listValidValueByOptional(TableMeta tableMeta, Map<String, Object> optional, List<String> fields) {
        return fields.stream().map(field -> {
            TableMeta.ColumnMeta columnMeta = tableMeta.getColumnMetaMap().get(field);
            return convert(optional.get(field), columnMeta.getField().getGenericType());
        }).toArray();
    }

    public static Object[] listUpdateValue(TableMeta tableMeta, Object obj) {
        List<Object> list = tableMeta.getColumns().stream()
                .filter(columnMeta -> !columnMeta.isReadOnly())
                .map(columnMeta -> getColumnValue(columnMeta, obj))
                .collect(Collectors.toList());
        list.add(getPrimaryKeyValue(tableMeta, obj));
        return list.toArray();
    }

    public static Object[] listInsertValue(TableMeta tableMeta, Object obj) {
        List<Object> list = tableMeta.getColumns().stream()
                .map(columnMeta -> getColumnValue(columnMeta, obj))
                .collect(Collectors.toList());
        list.add(0, getPrimaryKeyValue(tableMeta, obj));
        return list.toArray();
    }

}
