package com.didichuxing.datachannel.arius.admin.remote.zeus.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZeusSubTaskLog {
    private String hostname;

    private String stdout;

    private String stderr;
}
