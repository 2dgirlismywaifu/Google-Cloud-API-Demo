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

package com.notelysia.gcp.util;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.bigquery.BigqueryScopes;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.driveactivity.v2.DriveActivityScopes;
import com.google.api.services.people.v1.PeopleServiceScopes;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Instance {
    private static final Properties properties = new Properties();

    static {
        try {
            properties.load(Instance.class.getClassLoader().getResourceAsStream("application.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String applicationName = properties.getProperty("application.name");
    public static String credentialsFilePath = properties.getProperty("credentials.file.path");
    public static String serviceAccountClientId = properties.getProperty("service.account.clientId");
    public static String serviceAccountEmail = properties.getProperty("service.account.email");
    public static String gcpProjectId = properties.getProperty("gcp.projectId");
    public static String p12FilePath = properties.getProperty("p12.file.path");
    public static String p12Secret = properties.getProperty("p12.secret.password");

    public static final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    /**
     * This is list of scopes use to access Google Cloud Platform resources.
     * API need enable manually in GCP console.
     */
    public static final List<String> serviceAccountScopes =
            Arrays.asList(
                    BigqueryScopes.BIGQUERY,
                    BigqueryScopes.BIGQUERY_INSERTDATA,
                    BigqueryScopes.CLOUD_PLATFORM,
                    BigqueryScopes.CLOUD_PLATFORM_READ_ONLY
            );
    /**
     * This is list of scopes use to access API enable in GCP project.
     * API need enable manually in GCP console.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    public static final List<String> apiScopes =
            Arrays.asList(
                    CalendarScopes.CALENDAR_READONLY,
                    DriveActivityScopes.DRIVE_ACTIVITY_READONLY,
                    PeopleServiceScopes.USERINFO_EMAIL,
                    PeopleServiceScopes.USERINFO_PROFILE
            );
}
