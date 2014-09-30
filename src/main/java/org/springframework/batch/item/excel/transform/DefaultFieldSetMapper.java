package org.springframework.batch.item.excel.transform;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;

/**
 * Created by fvalmeida on 9/9/14.
 */
public class DefaultFieldSetMapper<T> implements FieldSetMapper<T> {

    final Class<T> typeParameterClass;

    public DefaultFieldSetMapper(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
    }

    @Override
    public T mapFieldSet(final FieldSet fieldSet) throws BindException {

        if (fieldSet == null) {
            return null;
        }

        T object = null;
        try {
            object = typeParameterClass.newInstance();
            final T finalObject = object;
            ReflectionUtils.doWithFields(typeParameterClass,
                    new ReflectionUtils.FieldCallback() {
                        @Override
                        public void doWith(final Field field) throws IllegalArgumentException,
                                IllegalAccessException {
                            for (String propertyName : fieldSet.getProperties().stringPropertyNames()) {
                                if (propertyName.equalsIgnoreCase(field.getName())) {
                                    System.out.println("Found field " + field + " in type "
                                            + field.getDeclaringClass());
                                    ReflectionUtils.makeAccessible(field);
                                    try {
                                        field.set(finalObject, getValue(field, propertyName, fieldSet));
                                    } catch (IllegalArgumentException e) {
                                        e.printStackTrace();
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                }
                            }
                        }
                    },
                    new ReflectionUtils.FieldFilter() {
                        @Override
                        public boolean matches(final Field field) {
                            final int modifiers = field.getModifiers();
                            // no static fields please
                            return !Modifier.isStatic(modifiers);
                        }
                    });

//            ReflectionUtils.findField(typeParameterClass, "lastName").set(object, fieldSet.readString("lastName"));
//            ReflectionUtils.findField(typeParameterClass, "firstName").set(object, fieldSet.readString("firstName"));
//            ReflectionUtils.findField(typeParameterClass, "position").set(object, fieldSet.readString("position"));
//            ReflectionUtils.findField(typeParameterClass, "debutYear").set(object, fieldSet.readInt("debutYear"));
//            ReflectionUtils.findField(typeParameterClass, "birthYear").set(object, fieldSet.readInt("birthYear"));
        } catch (InstantiationException e) {
            ReflectionUtils.handleReflectionException(e);
        } catch (IllegalAccessException e) {
            ReflectionUtils.handleReflectionException(e);
        }

        return object;
    }

    private static Object getValue(Field field, String propertyName, FieldSet fieldSet) throws Exception {
        if (Boolean.class == field.getType()) {
            return fieldSet.readBoolean(propertyName);
        }
        if (Byte.class == field.getType()) {
            return fieldSet.readByte(propertyName);
        }
        if (Short.class == field.getType()) {
            return fieldSet.readShort(propertyName);
        }
        if (Integer.class == field.getType()) {
            return fieldSet.readInt(propertyName);
        }
        if (Long.class == field.getType()) {
            return fieldSet.readLong(propertyName);
        }
        if (Float.class == field.getType()) {
            return fieldSet.readFloat(propertyName);
        }
        if (Double.class == field.getType()) {
            return fieldSet.readDouble(propertyName);
        }
        if (String.class == field.getType()) {
            return fieldSet.readString(propertyName);
        }
        if (BigDecimal.class == field.getType()) {
            return fieldSet.readBigDecimal(propertyName);
        }
        throw new Exception(String.format("Error getting value for:" +
                "\nField name: %s" +
                "\nProperty name: %s" +
                "", field.getName(), propertyName));
    }

}
