package org.springframework.batch.item.excel.transform;

import lombok.extern.log4j.Log4j;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;

import javax.persistence.Column;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by fvalmeida on 9/9/14.
 */
@Log4j
public class DefaultFieldSetMapper<T> implements FieldSetMapper<T> {

    final Class<T> typeParameterClass;
    private static String DEFAULT_DATE_PATTERN = "yyyy/MM/dd hh:mm:ss aa";

    private Map<String, Field> classFields = new HashMap<String, Field>();;

    public DefaultFieldSetMapper(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
        // Get the fields of given class and add to map for faster access
        Field[] fields = this.typeParameterClass.getDeclaredFields();
        for(Field f : fields){
            classFields.put(f.getName(), f);
            ReflectionUtils.makeAccessible(f);
        }
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

            for (String propertyName : fieldSet.getProperties().stringPropertyNames()) {
                String value = fieldSet.readString(propertyName);
                try {
                    Field field = classFields.get(propertyName);
                    if(field == null) throw new NoSuchFieldException();
                    checkNotNullableField(value, field);
                    try{
                        field.set(finalObject, getValue(field, propertyName, fieldSet));
                    }catch (NumberFormatException e){
                        throw new RuntimeException("Unparseable " + field.getType() + ": \"" + value + "\", name: " + propertyName);
                    }catch(IllegalArgumentException e){
                        throw new RuntimeException(e.getMessage());
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                } catch (NoSuchFieldException e) {
                    log.warn("Field [" + propertyName + "] not found on class [" + object.getClass() + "]");
                    continue;
                }
            }
        } catch (InstantiationException e) {
            ReflectionUtils.handleReflectionException(e);
        } catch (IllegalAccessException e) {
            ReflectionUtils.handleReflectionException(e);
        }

        return object;
    }

    private void checkNotNullableField(String value, Field field){
        Column columnAnnotation = field.getAnnotation(Column.class);
        if(columnAnnotation!= null && !columnAnnotation.nullable() && !StringUtils.hasText(value)){
            throw new RuntimeException("Field " + field.getName() + " cannot be empty.");
        }
    }
    /**
        Create the object for a given property.
        This method gets dynamically the field type for a given property,
            reads the property related to this on the FieldSet and return.
        
        Date columns may come by two ways :
              #1: A Long value for date, if Excel cell is set to date.
              #2: A String representing the date in a format if Excel
                      cell is set to text.

        java.util.Date is the default date type and annotation is
          not needed (only if the pattern for String value is not the default).

        Other than java.util.Date must have the
          org.springframework.format.annotation.DateTimeFormat annotation.

        Other date type must have a constructor receiving milliseconds time.

        For this version, only the pattern attribute will be read from
          org.springframework.format.annotation.DateTimeFormat annotation.

     */
    private static Object getValue(Field field, String propertyName, FieldSet fieldSet) throws Exception {
        Class<?> fieldType = field.getType();
        if (Boolean.class == fieldType || boolean.class == fieldType) {
            return fieldSet.readBoolean(propertyName);
        }
        if (Byte.class == fieldType || byte.class == fieldType) {
            return fieldSet.readByte(propertyName);
        }
        if (Short.class == fieldType || short.class == fieldType) {
            return fieldSet.readShort(propertyName);
        }
        if (Integer.class == fieldType || int.class == fieldType) {
            return fieldSet.readInt(propertyName);
        }
        if (Long.class == fieldType || long.class == fieldType) {
            return fieldSet.readLong(propertyName);
        }
        if (Float.class == fieldType || float.class == fieldType) {
            return fieldSet.readFloat(propertyName);
        }
        if (Double.class == fieldType || double.class == fieldType) {
            return fieldSet.readDouble(propertyName);
        }
        if (String.class == fieldType) {
            return fieldSet.readString(propertyName);
        }
        if (BigDecimal.class == fieldType) {
            return fieldSet.readBigDecimal(propertyName);
        }

        if(Date.class == fieldType){
            return createDateObject(Date.class, field, propertyName, fieldSet);
        }

        if(field.getAnnotation(DateTimeFormat.class)!= null){
            return createDateObject(fieldType, field, propertyName, fieldSet);
        }
        throw new Exception(String.format("Error getting value for:" +
                "\nField name: %s" +
                "\nProperty name: %s" +
                "", field.getName(), propertyName));
    }

    private static <E extends Object> E createDateObject(Class<E> clazz, Field field, String propertyName, FieldSet fieldSet){
        // Try reading Long Value
        try{
            String msString = fieldSet.readString(propertyName);
            if(!StringUtils.hasText(msString))
                return null;
            // remove the dot
            int dotIndex = msString.indexOf(".");
            if(dotIndex > 0)
                msString = msString.substring(0, dotIndex);
            Long milliseconds = Long.parseLong(msString);
            // convert date using POI DateUtil class because Date type in Excel starts from 1900 instead 1970
            Date javaDate = DateUtil.getJavaDate(milliseconds);
            return instantiateDateFromMilliseconds(clazz, javaDate.getTime());
        }catch(NumberFormatException e){
            // Find annotation to get the pattern
            DateTimeFormat dpAnnotation = field.getAnnotation(DateTimeFormat.class);
            String pattern = DEFAULT_DATE_PATTERN;
            // TODO read all properties from the DateTimeFormat annotation
            if((dpAnnotation != null) && !(dpAnnotation.pattern().isEmpty())){
                pattern = dpAnnotation.pattern();
            }
            // Get the Date from field set using the pattern
            Date javaDate = fieldSet.readDate(propertyName, pattern);
            return instantiateDateFromMilliseconds(clazz, javaDate.getTime());
        }
    }

    private static  <E extends Object> E instantiateDateFromMilliseconds(Class<E> clazz, Long milliseconds){
        try {
            Constructor<E> constructor = clazz.getConstructor(long.class);
            return constructor.newInstance(milliseconds);
        } catch (Exception e) {
            throw new RuntimeException(String.format("No constructors for %s receive " +
                    "long as parameter. \n" +
                    "Check if the DateTimeFormat annotation is in a date type object.", clazz));
        }

    }

}
