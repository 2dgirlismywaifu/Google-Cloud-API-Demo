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
import com.google.api.services.calendar.Calendar;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.TableResult;
import lombok.Getter;
import lombok.Setter;
import com.notelysia.gcp.dao.BigQueryClient;
import com.notelysia.gcp.dao.SqlExecute;
import com.notelysia.gcp.dao.ListRecords;
import com.notelysia.gcp.logic.CalendarLogic;
import com.notelysia.gcp.logic.ServiceCredential;
import com.notelysia.gcp.model.CalendarEvent;

import javax.servlet.ServletException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CalendarAction {
    private final CalendarLogic calendarLogicLogic = new CalendarLogic();
    private final BigQueryClient bigQueryLogic = new BigQueryClient();
    private final String dataSetName = "demo_data_set1";
    private final String tableName = "calendar_collection";
    private ServiceCredential serviceCredential;
    private Credential credential;


    public List<CalendarEvent> execute(String googleAccountId, String googleAccountEmail) throws Exception {
        if (this.credential == null) {
            throw new ServletException("User is not authenticated");
        }

        BigQuery bigQuery = this.serviceCredential.initializeBigQuery();
        Calendar calendarService = this.serviceCredential.createCalendarService(this.credential);
        LocalDate todayDateTime = LocalDate.now();
        String calendarEventToday =
                todayDateTime.format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) +
                        "_calendar_event_" +
                        googleAccountId +
                        ".csv";

        this.calendarLogicLogic.setCalendarService(calendarService);
        this.bigQueryLogic.setBigquery(bigQuery);

        this.calendarLogicLogic.downloadCalendarList(calendarEventToday);
        this.bigQueryLogic.loadLocalData2BigQuery(
                this.dataSetName,
                this.tableName,
                calendarEventToday,
                CalendarEvent.class);
        List<String> columnOrder = new ArrayList<>();
        columnOrder.add("createdTime");
        String whereCondition = "lower(attendees) like lower('%" +
                googleAccountEmail + "%') " +
                "OR lower(attendees) like lower('%" +
                googleAccountId + "%')";
        SqlExecute bigQuerySqlLogic = new SqlExecute();
        TableResult result = bigQuerySqlLogic.executeSelectQuery(
                bigQuery,
                this.dataSetName,
                this.tableName,
                columnOrder,
                whereCondition);
        ListRecords listRecords = new ListRecords();
        return listRecords.collectCalendarEventRecord(result);
    }
}
