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

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import lombok.Getter;
import lombok.Setter;
import com.notelysia.gcp.util.UtilsFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
@Setter
public class CalendarLogic {
    private static final Logger logger = Logger.getLogger(CalendarLogic.class.getName());
    private Calendar calendarService;

    public CalendarLogic() {
    }

    /**
     * Download the calendar event list from the primary calendar.
     * If service account not enable Domain-wide Delegation, the service account must be an attendee in the event.
     *
     * @throws IOException for request return error message
     */
    public void downloadCalendarList(String csvFile) throws IOException {
        //Create an empty list to store the activities and write to the new file
        List<String> calendarList = new ArrayList<>();
        String csvHeader = "eventId," +
                "eventTitle," +
                "eventType," +
                "status," +
                "createdTime," +
                "updatedTime," +
                "creator," +
                "organizer," +
                "eventLink," +
                "attendees," +
                "startTime," +
                "endTime," +
                "totalTime";
        logger.info("Download calendar list");
        String nextPageToken = null;
        do {
            CalendarList calendarIdList = this.calendarService.calendarList()
                    .list()
                    .setPageToken(nextPageToken)
                    .execute();
            List<CalendarListEntry> items = calendarIdList.getItems();
            if (items != null && !items.isEmpty()) {
                for (CalendarListEntry entry : items) {
                    this.collectAllEventList(calendarList, entry.getId());
                }
            } else {
                this.collectAllEventList(calendarList, "primary");
            }
            nextPageToken = calendarIdList.getNextPageToken();
        } while (nextPageToken != null);

        if (!calendarList.isEmpty()) {
            UtilsFunction.writeToCSV(calendarList, csvHeader, csvFile);
        } else {
            logger.info("No upcoming events found.");
        }
    }

    /**
     * Collect all events from the calendar
     * To collect all events in one week, one month, use the timeMin and timeMax parameter
     *
     * @param calendarList List to store all events
     * @param calendarId   Calendar ID use to get the event list
     * @throws IOException for request return error message
     */
    private void collectAllEventList(List<String> calendarList,
                                     String calendarId) throws IOException {
        String nextPageToken = null;
        do {
            // Use setTimeMax and setTimeMin to get the event list in a specific time range
            Events events = this.calendarService.events().list(calendarId)
                    .setMaxResults(10)
                    .setSingleEvents(false)
                    .setShowDeleted(true)
                    .setShowHiddenInvitations(true)
                    .setOrderBy("updated")
                    .setTimeMin(UtilsFunction.getFirstDayOfMonth())
                    .setTimeMax(UtilsFunction.getLastDayOfMonth())
                    .setPageToken(nextPageToken)
                    .execute();
            List<Event> eventItems = this.removeUnknownEvent(events.getItems());
            if (eventItems != null && !eventItems.isEmpty()) {
                for (Event event : eventItems) {
                    String eventId = event.getId();
                    String eventTitle = event.getSummary();
                    String evenType = event.getEventType();
                    String eventStatus = event.getStatus();
                    DateTime createdTime = event.getCreated();
                    DateTime updatedTime = event.getUpdated() != null ? event.getUpdated() : createdTime;
                    String creator = event.getCreator() != null ? event.getCreator().getEmail() : "";
                    String organizerEmail = event.getOrganizer() != null ? event.getOrganizer().getEmail() : "";
                    String eventLink = event.getHtmlLink();
                    List<String> attendees = event.getAttendees() != null ?
                            event.getAttendees().stream()
                                    .map(this::getEventAttendee)
                                    .collect(Collectors.toList()) : new ArrayList<>();
                    DateTime startTime = event.getStart().getDateTime();
                    DateTime endTime = event.getEnd().getDateTime();
                    if (startTime == null) {
                        startTime = event.getStart().getDate();
                    }
                    if (endTime == null) {
                        endTime = event.getEnd().getDate();
                    }
                    String totalTime = (endTime.getValue() - startTime.getValue()) / 1000.0 / 60.0 + " minutes";
                    calendarList.add(
                            "\"" + eventId + "\"," +
                                    "\"" + eventTitle + "\"," +
                                    "\"" + evenType + "\"," +
                                    "\"" + eventStatus + "\"," +
                                    "\"" + createdTime + "\"," +
                                    "\"" + updatedTime + "\"," +
                                    "\"" + creator + "\"," +
                                    "\"" + organizerEmail + "\"," +
                                    "\"" + eventLink + "\"," +
                                    "\"" + UtilsFunction.truncated(attendees, 5) + "\"," +
                                    "\"" + startTime + "\"," +
                                    "\"" + endTime + "\"," +
                                    "\"" + totalTime + "\""
                    );
                }
            }
            nextPageToken = events.getNextPageToken();
        } while (nextPageToken != null);
    }

    /**
     * Remove the event with unknown information
     *
     * @param eventItems List of events
     * @return List of events without unknown information
     */
    private List<Event> removeUnknownEvent(List<Event> eventItems) {
        return eventItems.stream()
                .filter(event -> event.getSummary() != null &&
                        event.getEventType() != null &&
                        event.getStatus() != null &&
                        event.getCreated() != null &&
                        event.getCreator() != null &&
                        event.getOrganizer() != null)
                .collect(Collectors.toList());
    }

    /**
     * Get the event attendee information
     *
     * @param attendee EventAttendee object
     * @return String representation of the event attendee
     */
    private String getEventAttendee(EventAttendee attendee) {
        String email = attendee.getEmail();
        String responseStatus = attendee.getResponseStatus();
        return String.format("{\"\"Email\"\": \"\"%s\"\", \"\"Response Status\"\": \"\"%s\"\"}",
                email, responseStatus);
    }
}
