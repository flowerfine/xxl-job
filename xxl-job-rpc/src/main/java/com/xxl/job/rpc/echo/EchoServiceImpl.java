package com.xxl.job.rpc.echo;

public class EchoServiceImpl implements EchoService {

    @Override
    public String echo(String name) {
        return String.format("hello, %s!", name);
    }
}
