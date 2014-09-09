package org.springframework.batch.item.excel.transform;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindException;

/**
 * Created by fvalmeida on 9/9/14.
 */
public class DefaultFieldSetMapper<T> implements FieldSetMapper<T> {

    final Class<T> typeParameterClass;

    public DefaultFieldSetMapper(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
    }

    @Override
    public T mapFieldSet(FieldSet fieldSet) throws BindException {

        if (fieldSet == null) {
            return null;
        }

        T object = null;
        try {
            object = typeParameterClass.newInstance();
            ReflectionUtils.findField(typeParameterClass,"id").set(object,fieldSet.readString("ID"));
            ReflectionUtils.findField(typeParameterClass,"lastName").set(object, fieldSet.readString("lastName"));
            ReflectionUtils.findField(typeParameterClass,"firstName").set(object, fieldSet.readString("firstName"));
            ReflectionUtils.findField(typeParameterClass,"position").set(object, fieldSet.readString("position"));
            ReflectionUtils.findField(typeParameterClass,"debutYear").set(object, fieldSet.readInt("debutYear"));
            ReflectionUtils.findField(typeParameterClass,"birthYear").set(object, fieldSet.readInt("birthYear"));
        } catch (InstantiationException e) {
            e.printStackTrace(); // TODO treat exception
        } catch (IllegalAccessException e) {
            e.printStackTrace(); // TODO treat exception
        }

        return object;
    }
}
