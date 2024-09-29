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

package com.notelysia.gcp;

import com.google.api.client.auth.oauth2.Credential;
import com.notelysia.gcp.controller.CalendarAction;
import com.notelysia.gcp.logic.ServiceCredential;
import com.notelysia.gcp.model.CalendarEvent;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "CalendarAppEngine", urlPatterns = "/calendar-collection")
public class CalendarAppEngine extends HttpServlet {

    private ServiceCredential serviceCredential;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.serviceCredential = (ServiceCredential) this.getServletContext().getAttribute("googleService");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        StringBuilder htmlResponse = new StringBuilder();
        htmlResponse.append("<html><head><style>")
                .append("table { width: 100%; border-collapse: collapse; }")
                .append("th, td { border: 1px solid black; padding: 8px; text-align: left; }")
                .append("th { background-color: #f2f2f2; }")
                .append("td { max-height: 100px; overflow-y: auto; }")
                .append("</style>")
                .append("<meta http-equiv=\"content-type\" content=\"application/xhtml+xml; charset=UTF-8\" />")
                .append("</head><body>");

        try {
            CalendarAction calendarAction = new CalendarAction();
            String userId = (String) request.getSession().getAttribute("userId");
            Credential credential;
            if (userId == null) {
                credential = null;
            } else {
                credential = this.serviceCredential.googleAccountAuthorize().loadCredential(userId);
            }
            if (credential == null) {
                throw new ServletException("User is not authenticated");
            }
            calendarAction.setServiceCredential(this.serviceCredential);
            calendarAction.setCredential(credential);
            String googleAccountEmail = (String) request.getSession().getAttribute("googleAccountEmail");
            String googleAccountId = (String) request.getSession().getAttribute("googleAccountId");
            List<CalendarEvent> calendarEvents = calendarAction.execute(googleAccountId, googleAccountEmail);
            htmlResponse.append("<h2>Google Calendar Event Collection for ")
                    .append(request.getSession().getAttribute("googleAccountEmail"))
                    .append("</h2>");
            htmlResponse.append("<table border='1'>");
            htmlResponse.append("<tr>")
                    .append("<th>Event ID</th>")
                    .append("<th>Event Title</th>")
                    .append("<th>Event Type</th>")
                    .append("<th>Status</th>")
                    .append("<th>Created Time</th>")
                    .append("<th>Updated Time</th>")
                    .append("<th>Creator</th>")
                    .append("<th>Organizer</th>")
                    .append("<th>Event Link</th>")
                    .append("<th>Attendees</th>")
                    .append("<th>Start Time</th>")
                    .append("<th>End Time</th>")
                    .append("<th>Total Time</th>")
                    .append("</tr>");

            for (CalendarEvent event : calendarEvents) {
                htmlResponse.append("<tr>")
                        .append("<td>").append(event.getEventId()).append("</td>")
                        .append("<td>").append(event.getEventTitle()).append("</td>")
                        .append("<td>").append(event.getEventType()).append("</td>")
                        .append("<td>").append(event.getStatus()).append("</td>")
                        .append("<td>").append(event.getCreatedTime()).append("</td>")
                        .append("<td>").append(event.getUpdatedTime()).append("</td>")
                        .append("<td>").append(event.getCreator()).append("</td>")
                        .append("<td>").append(event.getOrganizer()).append("</td>")
                        .append("<td>").append(event.getEventLink()).append("</td>")
                        .append("<td>").append(event.getAttendees()).append("</td>")
                        .append("<td>").append(event.getStartTime()).append("</td>")
                        .append("<td>").append(event.getEndTime()).append("</td>")
                        .append("<td>").append(event.getTotalTime()).append("</td>")
                        .append("</tr>");
            }


        } catch (Exception e) {
            throw new ServletException("Failed to collect Google Calendar event", e);
        }
        htmlResponse.append("</table>");
        htmlResponse.append("</body></html>");
        response.getWriter().write(htmlResponse.toString());
    }
}
