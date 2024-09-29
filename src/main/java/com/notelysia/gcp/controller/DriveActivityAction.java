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

package com.notelysia.gcp.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.people.v1.PeopleService;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.TableResult;
import lombok.Getter;
import lombok.Setter;
import com.notelysia.gcp.dao.BigQueryClient;
import com.notelysia.gcp.dao.SqlExecute;
import com.notelysia.gcp.dao.ListRecords;
import com.notelysia.gcp.logic.DriveActivityLogic;
import com.notelysia.gcp.logic.ServiceCredential;
import com.notelysia.gcp.model.DriveActivity;

import javax.servlet.ServletException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DriveActivityAction {
    private final DriveActivityLogic driveActivity = new DriveActivityLogic();
    private final BigQueryClient bigQueryClient = new BigQueryClient();
    private final String dataSetName = "demo_data_set1";
    private final String tableName = "drive_activity";
    private ServiceCredential serviceCredential;
    private Credential credential;


    public List<DriveActivity> execute(String googleAccountId, String googleAccountEmail) throws Exception {
        if (this.credential == null) {
            throw new ServletException("User is not authenticated");
        }

        com.google.api.services.driveactivity.v2.DriveActivity driveActivityService =
                this.serviceCredential.createDriveActivityService(this.credential);
        PeopleService peopleService = this.serviceCredential.createPeopleService(this.credential);
        BigQuery bigQuery = this.serviceCredential.initializeBigQuery();

        this.driveActivity.setDriveActivityService(driveActivityService);
        this.driveActivity.setPeopleService(peopleService);
        this.bigQueryClient.setBigquery(bigQuery);

        LocalDate yesterdayDate = LocalDate.now().minusDays(1);
        long yesterdayTime = yesterdayDate.atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli();
        String yesterday = yesterdayDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String yesterdayCSVFile = yesterday + "_activities_" +
                googleAccountId + ".csv";
        this.driveActivity.DownloadActivity(
                null,
                "time > " + yesterdayTime + " AND time <= " + Instant.now().toEpochMilli(),
                yesterdayCSVFile);

        this.bigQueryClient.loadLocalData2BigQuery(
                this.dataSetName,
                this.tableName,
                yesterdayCSVFile,
                DriveActivity.class);
        List<String> columnOrder = new ArrayList<>();
        columnOrder.add("timeActivity");
        columnOrder.add("activityId");
        String whereCondition = "lower(userAction) like lower('%" +
                googleAccountEmail + "%') " +
                "OR lower(userAction) like lower('%" +
                googleAccountId + "%')";
        SqlExecute bigQuerySqlLogic = new SqlExecute();
        TableResult result = bigQuerySqlLogic.executeSelectQuery(
                bigQuery,
                this.dataSetName,
                this.tableName,
                columnOrder,
                whereCondition);
        ListRecords listRecords = new ListRecords();
        return listRecords.collectDriveActivityRecord(result);
    }
}
