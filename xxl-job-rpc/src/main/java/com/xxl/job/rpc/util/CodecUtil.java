package com.xxl.job.rpc.util;

import cn.sliew.milky.serialize.protostuff.ProtostuffDataInputView;
import cn.sliew.milky.serialize.protostuff.ProtostuffDataOutputView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CodecUtil {

    private CodecUtil() {
        throw new IllegalStateException("no instance");
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ProtostuffDataOutputView dataOutputView = new ProtostuffDataOutputView(outputStream);
        dataOutputView.writeObject(obj);
        dataOutputView.flushBuffer();
        return outputStream.toByteArray();
    }

    public static <T> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ProtostuffDataInputView dataInputView = new ProtostuffDataInputView(inputStream);
        return (T) dataInputView.readObject();
    }
}
