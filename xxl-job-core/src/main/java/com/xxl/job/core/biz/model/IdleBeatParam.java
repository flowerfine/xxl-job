package com.xxl.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdleBeatParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private int jobId;
}