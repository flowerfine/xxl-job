package com.xxl.job.remote.protocol.request;

import com.xxl.job.remote.protocol.Request;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistryParam extends Request {

    private static final long serialVersionUID = 3343896734540819243L;

    private String registryGroup;
    private String registryKey;
    private String registryValue;
}
