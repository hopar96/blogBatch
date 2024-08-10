package com.hj.blogBatch.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Auth {

        /**
         * Define a global instance of the HTTP transport.
         */
        public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

        /**
         * Define a global instance of the JSON factory.
         */
        public static final JacksonFactory JSON_FACTORY = new JacksonFactory();

        /**
         * This is the directory that will be used under the user's home directory where
         * OAuth tokens will be stored.
         */
        private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";

        /**
         * Authorizes the installed application to access user's protected data.
         *
         * @param scopes              list of scopes needed to run youtube upload.
         * @param credentialDatastore name of the credential datastore to cache OAuth
         *                            tokens
         */
        public static Credential authorize(List<String> scopes, String credentialDatastore, String clientKeyPath) throws IOException {

                // Load client secrets.
                String secretKeyPath = "/Users/hojun/my/work/blogBatch/src/main/resources/client_secrets.json";
                // load client secrets
                GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                                new InputStreamReader(Auth.class.getResourceAsStream(clientKeyPath)));

                FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(
                                new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
                DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore(credentialDatastore);

                // set up authorization code flow
                GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes).setCredentialDataStore(datastore)
                                .build();

                LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8080).build();
                // authorize
                return new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");
        }
}
