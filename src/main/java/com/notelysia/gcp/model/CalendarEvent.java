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

package com.notelysia.gcp.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalendarEvent {
    @CsvBindByName(column = "eventId")
    private String eventId;

    @CsvBindByName(column = "eventTitle")
    private String eventTitle;

    @CsvBindByName(column = "eventType")
    private String eventType;

    @CsvBindByName(column = "status")
    private String status;

    @CsvBindByName(column = "createdTime")
    private String createdTime;

    @CsvBindByName(column = "updatedTime")
    private String updatedTime;

    @CsvBindByName(column = "creator")
    private String creator;

    @CsvBindByName(column = "organizer")
    private String organizer;

    @CsvBindByName(column = "eventLink")
    private String eventLink;

    @CsvBindByName(column = "attendees")
    private String attendees;

    @CsvBindByName(column = "startTime")
    private String startTime;

    @CsvBindByName(column = "endTime")
    private String endTime;

    @CsvBindByName(column = "totalTime")
    private String totalTime;
}
