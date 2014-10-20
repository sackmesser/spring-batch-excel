package org.springframework.core.io;

import java.io.*;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Diogo
 * Date: 17/10/14
 * Time: 14:59
 * To change this template use File | Settings | File Templates.
 */
public class CustomByteArrayResource extends AbstractResource implements WritableResource{

    private final byte[] byteArray;
    private final String description;
    private OutputStream outputStream;

    /**
     * Create a new ByteArrayResource.
     */
    public CustomByteArrayResource() {
        this.byteArray = null;
        this.description = "resource not loaded from byte array (for item writer)";
    }

    /**
     * Create a new ByteArrayResource.
     * @param byteArray the byte array to wrap
     */
    public CustomByteArrayResource(byte[] byteArray) {
        this(byteArray, "resource loaded from byte array");
    }

    /**
     * Create a new ByteArrayResource.
     * @param byteArray the byte array to wrap
     * @param description where the byte array comes from
     */
    public CustomByteArrayResource(byte[] byteArray, String description) {
        if (byteArray == null) {
            throw new IllegalArgumentException("Byte array must not be null");
        }
        this.byteArray = byteArray;
        this.description = (description != null ? description : "");
    }

    /**
     * Return the underlying byte array.
     */
    public final byte[] getByteArray() {
        if(this.byteArray!= null){
            return this.byteArray;
        }else{
            return ((ByteArrayOutputStream)getOutputStream()).toByteArray();
        }
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(){
        if(outputStream == null)
            outputStream = new ByteArrayOutputStream();
        return outputStream;
    }


    /**
     * This implementation always returns <code>true</code>.
     */
    @Override
    public boolean exists() {
        return true;
    }

    /**
     * This implementation returns the length of the underlying byte array.
     */
    @Override
    public long contentLength() {
        return this.byteArray.length;
    }

    /**
     * This implementation returns a ByteArrayInputStream for the
     * underlying byte array.
     * @see java.io.ByteArrayInputStream
     */
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(this.byteArray);
    }

    /**
     * This implementation returns the passed-in description, if any.
     */
    public String getDescription() {
        return this.description;
    }


    /**
     * This implementation compares the underlying byte array.
     * @see java.util.Arrays#equals(byte[], byte[])
     */
    @Override
    public boolean equals(Object obj) {
        return (obj == this ||
                (obj instanceof CustomByteArrayResource && Arrays.equals(((CustomByteArrayResource) obj).byteArray, this.byteArray)));
    }

    /**
     * This implementation returns the hash code based on the
     * underlying byte array.
     */
    @Override
    public int hashCode() {
        return (byte[].class.hashCode() * 29 * this.byteArray.length);
    }
}
