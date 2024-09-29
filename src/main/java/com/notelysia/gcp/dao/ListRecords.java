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

package com.notelysia.gcp.dao;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.notelysia.gcp.model.CalendarEvent;
import com.notelysia.gcp.model.DriveActivity;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ListRecords {

    /**
     * This class is used to collect and print all result records from the "select" query
     */
    public ListRecords() {
    }

    /**
     * Collect all Drive Activity records after executing SELECT query
     *
     * @param result TableResult object
     */
    public List<DriveActivity> collectDriveActivityRecord(TableResult result) {
        List<DriveActivity> driveActivities = new ArrayList<>();
        for (FieldValueList row : result.iterateAll()) {
            DriveActivity activity = new DriveActivity();
            try {
                activity.setActivityId(row.get("activityId").getStringValue());
                activity.setTimeActivity(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                        .withZone(ZoneOffset.UTC).format(this.convertMicrosecondsToInstant(
                                row.get("timeActivity").getTimestampValue())));
                activity.setUserAction(row.get("userAction").getStringValue());
                activity.setPrimaryAction(row.get("primaryAction").getStringValue());
                activity.setSubAction(row.get("subAction").getStringValue());
                activity.setSubActionType(row.get("subActionType").getStringValue());
                FieldValue subActionResult = row.get("subActionResult");
                activity.setSubActionResult(subActionResult.getValue() != null ? subActionResult.getStringValue() : "");
                activity.setItem(row.get("item").getStringValue());
                driveActivities.add(activity);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return driveActivities;
    }

    public List<CalendarEvent> collectCalendarEventRecord(TableResult result) {
        List<CalendarEvent> calendarEvents = new ArrayList<>();
        for (FieldValueList row : result.iterateAll()) {
            CalendarEvent event = new CalendarEvent();
            try {
                event.setEventId(row.get("eventId").getStringValue());
                event.setEventTitle(row.get("eventTitle").getStringValue());
                event.setEventType(row.get("eventType").getStringValue());
                event.setStatus(row.get("status").getStringValue());
                event.setCreatedTime(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                        .withZone(ZoneOffset.UTC).format(this.convertMicrosecondsToInstant(
                                row.get("createdTime").getTimestampValue())));
                event.setUpdatedTime(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                        .withZone(ZoneOffset.UTC).format(this.convertMicrosecondsToInstant(
                                row.get("updatedTime").getTimestampValue())));
                event.setCreator(row.get("creator").getStringValue());
                event.setOrganizer(row.get("organizer").getStringValue());
                event.setEventLink(row.get("eventLink").getStringValue());
                event.setAttendees(row.get("attendees").getStringValue());
                event.setStartTime(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                        .withZone(ZoneOffset.UTC).format(this.convertMicrosecondsToInstant(
                                row.get("startTime").getTimestampValue())));
                event.setEndTime(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                        .withZone(ZoneOffset.UTC).format(this.convertMicrosecondsToInstant(
                                row.get("endTime").getTimestampValue())));
                event.setTotalTime(row.get("totalTime").getStringValue());
                calendarEvents.add(event);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return calendarEvents;
    }

    /**
     * Convert microseconds to Instant
     *
     * @param microseconds Time in microseconds
     */
    private Instant convertMicrosecondsToInstant(long microseconds) {
        return Instant.ofEpochSecond(microseconds / 1_000_000,
                (microseconds % 1_000_000) * 1_000);
    }
}
