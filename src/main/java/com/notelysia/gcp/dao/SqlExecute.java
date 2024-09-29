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


import com.google.cloud.bigquery.*;
import com.notelysia.gcp.model.CalendarEvent;
import com.notelysia.gcp.util.UtilsFunction;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class SqlExecute {

    private static final Logger logger = Logger.getLogger(SqlExecute.class.getName());

    /**
     * Class to handle BigQuery SQL.
     * If exception happen with problem about BigQuery quota,
     * try using BigQuery full version instead of
     * BigQuery sandbox in Google Cloud Console.
     */
    public SqlExecute() {
    }

    /**
     * Get result after executing SELECT query
     *
     * @param bigquery       BigQuery Service
     * @param dataSetName    Name of the dataset
     * @param tableName      Name of the table
     * @param columnOrder    List of column names to order
     * @param whereCondition WHERE condition
     */
    public TableResult executeSelectQuery(BigQuery bigquery,
                                          String dataSetName,
                                          String tableName,
                                          List<String> columnOrder,
                                          String whereCondition) throws InterruptedException {
        String sqlQuery = String.format("SELECT * FROM `%s.%s.%s`",
                bigquery.getOptions().getProjectId(),
                dataSetName,
                tableName
        );

        if (whereCondition != null && !whereCondition.isEmpty()) {
            sqlQuery = sqlQuery + " WHERE " + whereCondition;
        }

        if (columnOrder != null && !columnOrder.isEmpty()) {
            StringBuilder order = new StringBuilder();
            for (String column : columnOrder) {
                order.append(column).append(",");
            }
            sqlQuery = sqlQuery + " ORDER BY " + order.substring(0, order.length() - 1) + " DESC";
        }

        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(sqlQuery)
                        .setUseLegacySql(false)
                        .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            throw new RuntimeException(queryJob.getStatus().getExecutionErrors().toString());
        }

        // Get the results.
        return queryJob.getQueryResults();

    }

    /**
     * Change the column name in the table using DDL statement
     * because BigQuery Console, BigQuery API does not support this feature.
     *
     * @param bigquery    BigQuery Service
     * @param dataSetName Name of the dataset
     * @param tableName   Name of the table
     * @param oldName     Old name of the column
     * @param newName     New name of the column
     */
    public void changeColumnName(BigQuery bigquery,
                                 String dataSetName,
                                 String tableName,
                                 String oldName,
                                 String newName) throws InterruptedException {
        String ddlStatement = String.format("ALTER TABLE `%s.%s.%s` RENAME COLUMN `%s` TO `%s`",
                bigquery.getOptions().getProjectId(),
                dataSetName, tableName, oldName, newName);

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(ddlStatement)
                .setUseLegacySql(false)
                .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();
        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            throw new RuntimeException(queryJob.getStatus().getExecutionErrors().toString());
        }
        logger.info(String.format("Column: %s renamed to %s successfully", oldName, newName));
    }

    /**
     * Change the column description in the table using DDL statement
     *
     * @param bigquery    BigQuery Service
     * @param dataSetName Name of the dataset
     * @param tableName   Name of the table
     * @param columnName  Name of the column
     * @param description Description of the column
     */
    public void changeColumnDescription(BigQuery bigquery,
                                        String dataSetName,
                                        String tableName,
                                        String columnName,
                                        String description) throws InterruptedException {
        String ddlStatement = String.format("ALTER TABLE `%s.%s.%s` ALTER COLUMN `%s` SET OPTIONS(description='%s')",
                bigquery.getOptions().getProjectId(),
                dataSetName, tableName, columnName, description);

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(ddlStatement)
                .setUseLegacySql(false)
                .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();
        // Check for errors
        if (queryJob == null) {
            logger.info("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            logger.warning(queryJob.getStatus().getExecutionErrors().toString());
        }
        logger.info(String.format("Column: %s description updated successfully", columnName));
    }

    /**
     * Change the column data type in the table using DDL statement.
     * Not recommend to use this method, please use Google Cloud Console for better result
     *
     * @param bigquery    BigQuery Service
     * @param dataSetName Name of the dataset
     * @param tableName   Name of the table
     * @param columnName  Name of the column
     * @param newType     New data type of the column
     */
    public void changeColumnDataType(BigQuery bigquery,
                                     String dataSetName,
                                     String tableName,
                                     String columnName,
                                     StandardSQLTypeName newType) throws InterruptedException {
        String ddlStatement = String.format("ALTER TABLE `%s.%s.%s` ALTER COLUMN `%s` SET DATA TYPE %s",
                bigquery.getOptions().getProjectId(),
                dataSetName, tableName, columnName, newType.toString());

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(ddlStatement)
                .setUseLegacySql(false)
                .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();
        // Check for errors
        if (queryJob == null) {
            logger.info("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            logger.warning(queryJob.getStatus().getExecutionErrors().toString());
        }

        logger.info(String.format("Column: %s data type updated successfully", columnName));
    }

    /**
     * Change the column nullable in the table using DDL statement.
     * This method only work if column mode is REQUIRED and only support from REQUIRED to NULLABLE
     *
     * @param bigquery    BigQuery Service
     * @param dataSetName Name of the dataset
     * @param tableName   Name of the table
     * @param columnName  Name of the column
     */
    public void changeColumnNullable(BigQuery bigquery,
                                     String dataSetName,
                                     String tableName,
                                     String columnName) throws InterruptedException {
        String ddlStatement = String.format("ALTER TABLE `%s.%s.%s` ALTER COLUMN `%s` DROP NOT NULL",
                bigquery.getOptions().getProjectId(),
                dataSetName, tableName, columnName);

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(ddlStatement)
                .setUseLegacySql(false)
                .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();
        // Check for errors
        if (queryJob == null) {
            logger.info("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            logger.warning(queryJob.getStatus().getExecutionErrors().toString());
        }
        logger.info(String.format("Column: %s update to nullable successfully", columnName));
    }

    /**
     * Change the column default value in the table using DDL statement.
     *
     * @param bigquery     BigQuery Service
     * @param dataSetName  Name of the dataset
     * @param tableName    Name of the table
     * @param columnName   Name of the column
     * @param mode         If field mode is REPEATED, default value must be arrayed
     * @param defaultValue Default value of the column.
     *                     If field mode is REPEATED, default value must be arrayed
     *                     and each element is quoted with ","
     */
    public void changeColumnDefaultValue(BigQuery bigquery,
                                         String dataSetName,
                                         String tableName,
                                         String columnName,
                                         Field.Mode mode,
                                         String defaultValue) throws InterruptedException {
        if (mode == Field.Mode.REPEATED) {
            //Check default value is arrayed and each element is quoted with ","
            if (!defaultValue.startsWith("[") || !defaultValue.endsWith("]")) {
                logger.warning("Error: Default value must be arrayed and each element is quoted with \",\"");
                return;
            }
        } else {
            defaultValue = "'" + defaultValue + "'";
        }

        String ddlStatement = String.format("ALTER TABLE `%s.%s.%s` ALTER COLUMN `%s` SET DEFAULT %s",
                bigquery.getOptions().getProjectId(),
                dataSetName, tableName, columnName, defaultValue);

        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(ddlStatement)
                .setUseLegacySql(false)
                .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();
        // Check for errors
        if (queryJob == null) {
            logger.info("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            logger.warning(queryJob.getStatus().getExecutionErrors().toString());
        }
        logger.info(String.format("Column: %s default value updated successfully", columnName));
    }

    /**
     * Update a record value to the table using DDL statement.
     *
     * @param dataSetName Name of the dataset
     * @param tableName   Name of the table
     * @param record      Record to update
     */
    public <T> void updateTableRecord(BigQuery bigquery,
                                      String dataSetName,
                                      String tableName,
                                      T record) throws InterruptedException {
        StringBuilder sqlQuery = new StringBuilder(String.format(
                "UPDATE `%s.%s.%s` ",
                bigquery.getOptions().getProjectId(), dataSetName, tableName
        ));

        if (record instanceof CalendarEvent) {
            String whereAndSet = this.getWhereAndSetForCalendarEvent((CalendarEvent) record);
            sqlQuery.append(whereAndSet);
        }
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(sqlQuery.toString())
                .setUseLegacySql(false)
                .build();
        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
        // Wait for the query to complete.
        queryJob = queryJob.waitFor();
        // Check for errors
        if (queryJob == null) {
            logger.info("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            logger.warning(queryJob.getStatus().getExecutionErrors().toString());
        }
    }

    /**
     * Get the WHERE and SET statement for CalendarEvent record
     *
     * @param record CalendarEvent record
     * @return WHERE and SET statement for UPDATE query
     */
    private String getWhereAndSetForCalendarEvent(CalendarEvent record) {
        String formatAttendees;
        if (UtilsFunction.isValidJson(record.getAttendees())) {
            formatAttendees = "'%s'";
        } else {
            formatAttendees = "\"%s\"";
        }
        return String.format(
                "SET event_title = \"%s\", " +
                        "event_type = \"%s\", " +
                        "status = \"%s\", " +
                        "created_time = \"%s\", " +
                        "updated_time = \"%s\", " +
                        "creator = \"%s\", " +
                        "organizer = \"%s\", " +
                        "event_link = \"%s\", " +
                        "attendees = " + formatAttendees + ", " +
                        "start_time = \"%s\", " +
                        "end_time = \"%s\", " +
                        "total_time = \"%s\" " +
                        "WHERE event_id = \"%s\"",
                record.getEventTitle(),
                record.getEventType(),
                record.getStatus(),
                record.getCreatedTime(),
                record.getUpdatedTime(),
                record.getCreator(),
                record.getOrganizer(),
                record.getEventLink(),
                record.getAttendees(),
                record.getStartTime(),
                record.getEndTime(),
                record.getTotalTime(),
                record.getEventId()
        );
    }
}