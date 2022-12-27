package com.aws.demo.aurora;

import lombok.Data;

@Data
public class DbSecret {

    private String username;
    private String password;
    private String host;
    private String port;
    private String engine;
    private String dbClusterIdentifier;
}
