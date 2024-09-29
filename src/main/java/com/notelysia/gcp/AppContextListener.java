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

import com.notelysia.gcp.logic.ServiceCredential;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.security.GeneralSecurityException;

@WebListener
public class AppContextListener implements ServletContextListener {
    private final String datasetName = "demo_data_set1";
    private final String driveActivityTableName = "drive_activity";
    private final String calendarTableName = "calendar_collection";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {

            ServiceCredential serviceCredential = new ServiceCredential();
            serviceCredential.preparedBigQueryTable(this.datasetName, this.driveActivityTableName, this.calendarTableName);
            sce.getServletContext().setAttribute("googleService", serviceCredential);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
