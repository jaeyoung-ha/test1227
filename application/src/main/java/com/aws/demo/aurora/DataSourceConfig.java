package com.aws.demo.aurora;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DataSourceConfig {
    private final Gson gson = new Gson();

    @Bean
    public DataSource datasource() {
        final DbSecret dbCredentials = getSecret();

        return DataSourceBuilder.create()
                .url("jdbc:postgresql://"+dbCredentials.getHost()+":"+dbCredentials.getPort()+"/postgres")
                .username(dbCredentials.getUsername())
                .password(dbCredentials.getPassword())
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    private DbSecret getSecret() {
        String secretName = "/secret/dbsecret";

        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard().withRegion(Regions.AP_NORTHEAST_2).build();

        String secret = "";
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName);
        GetSecretValueResult getSecretValueResult = null;

        try {
            getSecretValueResult = client.getSecretValue(getSecretValueRequest);

            if (getSecretValueResult.getSecretString() != null) {
                secret = getSecretValueResult.getSecretString();
            }
        } catch (Exception e) {
            throw e;
        }

        return gson.fromJson(secret, DbSecret.class);
    }
}
