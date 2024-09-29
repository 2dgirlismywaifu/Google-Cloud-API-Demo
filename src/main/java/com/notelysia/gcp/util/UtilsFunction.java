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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.notelysia.gcp.logic.ServiceCredential;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UtilsFunction {

    private static final ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
    private static final DateTimeFormatter rfc3339Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final Logger logger = Logger.getLogger(UtilsFunction.class.getName());

    /**
     * Returns a string representation of the first elements in a list.
     */
    public static String truncated(List<String> array, int limit) {
        return truncatedTo(array, limit);
    }

    /**
     * Returns a string representation of the first elements in a list.
     */
    private static String truncatedTo(List<String> array, int limit) {
        String contents = array.stream().limit(limit).collect(Collectors.joining(", "));
        String more = array.size() > limit ? ", ..." : "";
        return !array.isEmpty() ? "[" + contents + more + "]" : "";
    }

    /**
     * Write the csv file after collecting the records
     *
     * @param collection Collection will be written to a file
     * @param csvHeader  Header of the csv file
     * @param fileName   Name of the file to write the activities
     */
    public static void writeToCSV(List<String> collection, String csvHeader, String fileName) {
        try {
            File file = new File(fileName);
            FileWriter fileWriter = new FileWriter(file);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            // Convert the csvHeader to string and write to file
            printWriter.println(csvHeader);
            for (String record : collection) {
                printWriter.println(record);
            }
            printWriter.close();
            fileWriter.close();
            logger.info(String.format("Write to file: %s", fileName));
        } catch (IOException e) {
            logger.info("Exception happen: " + e);
        }
    }

    public static boolean isValidJson(String json) {
        try {
            JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            return false;
        }
        return true;
    }

    public static DateTime getFirstDayOfMonth() {
        ZonedDateTime firstDayOfMonth = currentDateTime.withDayOfMonth(1)
                .with(LocalTime.MIN)
                .withZoneSameInstant(ZoneOffset.UTC);
        return DateTime.parseRfc3339(firstDayOfMonth.format(rfc3339Formatter));
    }

    public static DateTime getLastDayOfMonth() {
        ZonedDateTime lastDayOfMonth = currentDateTime.withDayOfMonth(
                        currentDateTime.toLocalDate().lengthOfMonth())
                .with(LocalTime.MAX)
                .withZoneSameInstant(ZoneOffset.UTC);
        return DateTime.parseRfc3339(lastDayOfMonth.format(rfc3339Formatter));
    }

    /**
     * Format the timestamp to ISO 8601 format
     *
     * @param value The timestamp value
     * @return The formatted timestamp
     */
    public static String formatTimeStamp(String value) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        //Check the value is in ISO 8601 format or not
        if (isValidDateTime(value, iso8601Formatter)) {
            return value;
        }
        if (isValidDateTime(value, rfc3339Formatter)) {
            return ZonedDateTime.parse(value).format(iso8601Formatter);
        }
        // Parse the input date
        LocalDate localDate = LocalDate.parse(value, inputFormatter);
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneOffset.UTC);
        return zonedDateTime.format(iso8601Formatter);
    }

    private static boolean isValidDateTime(String value, DateTimeFormatter formatter) {
        try {
            ZonedDateTime.parse(value, formatter);
        } catch (DateTimeException e) {
            return false;
        }
        return true;
    }

    public static boolean isCredentialValid(HttpSession session) {
        ServiceCredential serviceCredential = (ServiceCredential) session.getServletContext().getAttribute("googleService");
        String userId = (String) session.getAttribute("userId");
        try {
            Credential credential = serviceCredential.googleAccountAuthorize().loadCredential(userId);
            return credential != null;
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
