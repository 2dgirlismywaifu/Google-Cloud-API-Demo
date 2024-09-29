/*
 * Copyright @2024 by 2dgirlismywaifu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.notelysia.gcp.logic;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.driveactivity.v2.DriveActivity;
import com.google.api.services.people.v1.PeopleService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.*;
import com.notelysia.gcp.dao.BigQueryClient;
import com.notelysia.gcp.util.Instance;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class ServiceCredential {

    private final DataStoreFactory dataStoreFactory = new MemoryDataStoreFactory();

    public ServiceCredential() {
    }

    /**
     * Creates a Service Account credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private GoogleCredentials serviceAccountAuthorize()
            throws IOException, GeneralSecurityException {
        // Load client secrets.
        InputStream in = GoogleCredentials.class.getResourceAsStream("/" + Instance.p12FilePath);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + Instance.p12FilePath);
        }
        // Load the keystore from the P12 file
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(in, Instance.p12Secret.toCharArray());

        // Extract the private key and certificate
        String alias = keystore.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, Instance.p12Secret.toCharArray());

        // Convert the private key to PKCS#8 format
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        PrivateKey pkcs8PrivateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        /*
         Create ServiceAccountCredentials using the PKCS#8 key
         For access to another google account in Google Workspace Domain,
         use setServiceAccountUser if Service Account Credentials have Domain-Wide Delegation enabled
        */
        return ServiceAccountCredentials.newBuilder()
                //.setServiceAccountUser("<enter-email-in-google-workspace>")
                .setClientEmail(Instance.serviceAccountEmail)
                .setClientId(Instance.serviceAccountClientId)
                .setPrivateKey(pkcs8PrivateKey)
                .setQuotaProjectId(Instance.gcpProjectId)
                .setProjectId(Instance.gcpProjectId)
                .setScopes(Instance.serviceAccountScopes).build();
    }


    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    public GoogleAuthorizationCodeFlow googleAccountAuthorize() throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        // Load client secrets.
        InputStream in = GoogleCredentials.class.getResourceAsStream("/" + Instance.credentialsFilePath);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + Instance.credentialsFilePath);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(Instance.jsonFactory, new InputStreamReader(in));
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                Instance.jsonFactory,
                clientSecrets, Instance.apiScopes)
                .setDataStoreFactory(this.dataStoreFactory)
                .setCredentialDataStore(this.dataStoreFactory.getDataStore("StoredCredential"))
                .setAccessType("offline")
                .build();
    }


    /**
     * Initialize BigQuery client that will be used to send requests. This client only needs to be created
     * once, and can be reused for multiple requests.
     *
     * @return BigQuery
     * @throws IOException If ADC from Google Cloud CLI cannot be found.
     */
    public BigQuery initializeBigQuery() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = this.serviceAccountAuthorize();
        return BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(credentials.getQuotaProjectId())
                .build()
                .getService();
    }

    /**
     * Create PeopleService to get user email from UserId
     * This method requires the People API to be enabled in the Google Cloud Console
     *
     * @return PeopleService to get user email from UserId
     */
    public PeopleService createPeopleService(Credential credential) throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new PeopleService.Builder(
                httpTransport,
                Instance.jsonFactory,
                credential)
                .setApplicationName(Instance.applicationName)
                .build();
    }

    /**
     * Build and return an authorized Calendar client service.
     *
     * @return an authorized Calendar client service
     */
    public Calendar createCalendarService(Credential credential) throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(
                httpTransport,
                Instance.jsonFactory, credential)
                .setApplicationName(Instance.applicationName)
                .build();
    }

    /**
     * Build and return an authorized Drive Activity client service.
     *
     * @return an authorized DriveActivity client service
     */
    public DriveActivity createDriveActivityService(Credential credential) throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new DriveActivity.Builder(
                httpTransport,
                Instance.jsonFactory,
                credential)
                .setApplicationName(Instance.applicationName)
                .build();
    }

    /**
     * Clears the data store.
     */
    public void clearDataStore(String userId) throws IOException {
        DataStore<Serializable> credentialDataStored = this.dataStoreFactory.getDataStore("StoredCredential");
        credentialDataStored.delete(userId);
    }

    /**
     * Prepare a BigQuery dataset and table for storing Drive Activity and Calendar data.
     *
     * @param dataset           The name of the dataset to create.
     * @param driveActivityTale The name of the table to create for Drive Activity data.
     * @param calendarTable     The name of the table to create for Calendar data.
     */
    public void preparedBigQueryTable(String dataset,
                                      String driveActivityTale,
                                      String calendarTable) throws GeneralSecurityException, IOException {
        BigQuery bigQuery = this.initializeBigQuery();
        BigQueryClient bigQueryClient = new BigQueryClient();
        bigQueryClient.setBigquery(bigQuery);
        bigQueryClient.createDataSet(dataset);
        Schema driveActivitySchema = Schema.of(
                Field.newBuilder("activityId", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("timeActivity", StandardSQLTypeName.TIMESTAMP).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("userAction", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("primaryAction", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("subAction", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("subActionType", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("subActionResult", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("item", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build()
        );
        Schema calendarSchema = Schema.of(
                Field.newBuilder("eventId", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("eventTitle", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("eventType", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("status", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("createdTime", StandardSQLTypeName.TIMESTAMP).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("updatedTime", StandardSQLTypeName.TIMESTAMP).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("creator", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("organizer", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("eventLink", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("attendees", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("startTime", StandardSQLTypeName.TIMESTAMP).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("endTime", StandardSQLTypeName.TIMESTAMP).setMode(Field.Mode.NULLABLE).build(),
                Field.newBuilder("totalTime", StandardSQLTypeName.STRING).setMode(Field.Mode.NULLABLE).build());

        bigQueryClient.createTable(dataset, driveActivityTale, driveActivitySchema);
        bigQueryClient.createTable(dataset, calendarTable, calendarSchema);
    }
}
