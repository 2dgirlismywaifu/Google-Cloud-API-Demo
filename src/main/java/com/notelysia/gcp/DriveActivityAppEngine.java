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
import com.notelysia.gcp.controller.DriveActivityAction;
import com.notelysia.gcp.logic.ServiceCredential;
import com.notelysia.gcp.model.DriveActivity;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "DriveActivityAppEngine", urlPatterns = "/drive-activity")
public class DriveActivityAppEngine extends HttpServlet {

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
            DriveActivityAction driveActivityAction = new DriveActivityAction();
            driveActivityAction.setServiceCredential(this.serviceCredential);
            driveActivityAction.setCredential(credential);
            String googleAccountEmail = (String) request.getSession().getAttribute("googleAccountEmail");
            String googleAccountId = (String) request.getSession().getAttribute("googleAccountId");
            List<DriveActivity> driveActivities = driveActivityAction.execute(googleAccountId, googleAccountEmail);
            htmlResponse.append("<h2>Drive Activity Log Collection for ")
                    .append(request.getSession().getAttribute("googleAccountEmail"))
                    .append("</h2>");
            htmlResponse.append("<table border='1'>");
            htmlResponse.append("<tr>")
                    .append("<th>Activity ID</th>")
                    .append("<th>Time Activity</th>")
                    .append("<th>User Action</th>")
                    .append("<th>Primary Action</th>")
                    .append("<th>Sub Action</th>")
                    .append("<th>Sub Action Type</th>")
                    .append("<th>Sub Action Result</th>")
                    .append("<th>Item</th>")
                    .append("</tr>");

            for (DriveActivity activity : driveActivities) {
                htmlResponse.append("<tr>")
                        .append("<td>").append(activity.getActivityId()).append("</td>")
                        .append("<td>").append(activity.getTimeActivity()).append("</td>")
                        .append("<td>").append(activity.getUserAction()).append("</td>")
                        .append("<td>").append(activity.getPrimaryAction()).append("</td>")
                        .append("<td>").append(activity.getSubAction()).append("</td>")
                        .append("<td>").append(activity.getSubActionType()).append("</td>")
                        .append("<td>").append(activity.getSubActionResult()).append("</td>")
                        .append("<td>").append(activity.getItem()).append("</td>")
                        .append("</tr>");
            }

            htmlResponse.append("</table>");
        } catch (Exception e) {
            throw new ServletException("Failed to collect Google Drive Activity Log", e);
        }
        htmlResponse.append("</body></html>");
        response.getWriter().write(htmlResponse.toString());
    }

}
