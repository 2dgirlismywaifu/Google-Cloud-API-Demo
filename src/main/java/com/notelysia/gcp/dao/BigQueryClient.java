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

import com.google.api.gax.paging.Page;
import com.google.api.services.bigquery.model.DatasetList;
import com.google.cloud.bigquery.*;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import lombok.Getter;
import lombok.Setter;
import com.notelysia.gcp.model.CalendarEvent;
import com.notelysia.gcp.model.DriveActivity;
import com.notelysia.gcp.util.UtilsFunction;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
@Setter
public class BigQueryClient {
    private static final Logger logger = Logger.getLogger(BigQueryClient.class.getName());
    private final SqlExecute sqlExecute = new SqlExecute();
    private BigQuery bigquery;
    private HttpServletResponse response;
    private List<DatasetList.Datasets> datasets;

    /**
     * Method to list of datasets available in BigQuery.
     */
    public void getDatasetList() {
        try {
            Page<Dataset> datasets = this.bigquery.listDatasets(
                    this.bigquery.getOptions().getProjectId(),
                    BigQuery.DatasetListOption.pageSize(100));
            if (datasets == null) {
                logger.info("Dataset does not contain any models");
                return;
            }
            for (Dataset dataset : datasets
                    .iterateAll()) {
                logger.warning(String.format("Dataset ID: %s", dataset.getDatasetId().getDataset()));
                this.getDatasetInfo(dataset.getDatasetId().getDataset());
            }
        } catch (BigQueryException e) {

            logger.warning(String.format("Project does not contain any datasets \n%s", e));
        }
    }

    /**
     * Method get Dataset information: Friendly Name, Description, Location, Tables.
     *
     * @param datasetName BigQuery Dataset Name
     */
    private void getDatasetInfo(String datasetName) {
        try {
            DatasetId datasetId = DatasetId.of(
                    this.bigquery.getOptions().getProjectId(), datasetName);
            Dataset dataset = this.bigquery.getDataset(datasetId);

            // View dataset properties
            String friendlyName = dataset.getFriendlyName() != null ? dataset.getFriendlyName() : "No friendly name";
            String description = dataset.getDescription() != null ? dataset.getDescription() : "No description";
            String location = dataset.getLocation();
            this.response.getWriter().println("Friendly Name: " + friendlyName);
            this.response.getWriter().println("Description: " + description);
            this.response.getWriter().println("Location: " + location);
            Page<Table> tables = this.bigquery.listTables(datasetName, BigQuery.TableListOption.pageSize(100));
            for (Table table : tables.iterateAll()) {
                logger.info(String.format("Table: %s", table.getTableId().getTable()));
                this.getTableInfo(datasetName, table.getTableId().getTable());
            }
        } catch (BigQueryException e) {

            logger.warning(String.format("Dataset info not retrieved. \n%s", e));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method get Table information: Friendly Name, Description, Total Rows, Fields.
     * Each field get Name, Type, Mode (NULL or NOTNULL), Description.
     *
     * @param dataSetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     */
    private void getTableInfo(String dataSetName, String tableName) {
        try {
            TableId tableId = TableId.of(dataSetName, tableName);
            Table table = this.bigquery.getTable(tableId);
            // View table properties
            String friendlyName = table.getFriendlyName() != null ? table.getFriendlyName() : "No friendly name";
            String description = table.getDescription() != null ? table.getDescription() : "No description";
            String totalRows = table.getNumRows().toString();
            this.response.getWriter().println("Friendly Name: " + friendlyName);
            this.response.getWriter().println("Description: " + description);
            this.response.getWriter().println("Total Rows: " + totalRows);
            //List table fields
            if (table.getDefinition().getSchema() != null) {
                for (Field field : table.getDefinition().getSchema().getFields()) {
                    String descriptionField = field.getDescription() != null ?
                            field.getDescription() : "No description";
                    logger.info(String.format("Field: %s - %s - %s - %s",
                            field.getName(),
                            field.getType(),
                            field.getMode(),
                            descriptionField));
                }
            }
        } catch (BigQueryException | IOException e) {

            logger.warning(String.format("Table info not retrieved. \n%s", e));
        }
    }

    /**
     * Create new dataset.
     *
     * @param dataSetName BigQuery Dataset Name
     */
    public void createDataSet(String dataSetName) {
        try {
            if (!this.checkDatasetExists(dataSetName)) {
                DatasetInfo datasetInfo = DatasetInfo.newBuilder(dataSetName).build();
                Dataset newDataset = this.bigquery.create(datasetInfo);
                String newDatasetName = newDataset.getDatasetId().getDataset();
                logger.info(String.format("%s created successfully", newDatasetName));
            } else {
                logger.warning(String.format("Error: Dataset %s already exists!", dataSetName));
            }
        } catch (BigQueryException e) {

            logger.warning(String.format("Dataset was not created. \n%s", e));
        }
    }

    /**
     * Check if dataset available or not.
     *
     * @param dataSetName BigQuery Dataset Name
     */
    private boolean checkDatasetExists(String dataSetName) {
        Dataset dataset = this.bigquery.getDataset(DatasetId.of(dataSetName));
        return dataset != null;
    }

    /**
     * Create new empty table.
     *
     * @param dataSetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     * @param schema      BigQuery Schema
     */
    public void createTable(String dataSetName, String tableName, Schema schema) {
        try {
            if (!this.checkTableExists(dataSetName, tableName)) {
                TableId tableId = TableId.of(dataSetName, tableName);
                TableDefinition tableDefinition;
                tableDefinition = StandardTableDefinition.of(Objects.requireNonNullElseGet(schema, Schema::of));
                TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

                this.bigquery.create(tableInfo);
                logger.info(String.format("Table %s created successfully", tableName));
            } else {
                logger.warning(String.format("Error: Table %s already exists! Skipping...", tableName));
            }
        } catch (BigQueryException e) {
            logger.warning(String.format("Table was not created. \n%s", e));
        }
    }

    /**
     * Check table is available or not.
     *
     * @param dataSetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     */
    private boolean checkTableExists(String dataSetName, String tableName) {
        TableId tableId = TableId.of(dataSetName, tableName);
        Table table = this.bigquery.getTable(tableId);
        return table != null;
    }

    /**
     * Upload the CSV file to BigQuery (Local file).
     * For insert new row, the CSV file have header row must match with table fields.
     *
     * @param datasetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     * @param csvFile     Name of csv file
     * @param clazz       Class Model of CSV file
     */
    public <T> void loadLocalData2BigQuery(String datasetName,
                                           String tableName,
                                           String csvFile,
                                           Class<T> clazz) throws IOException, InterruptedException {
        //Method create dataset and table already check if dataset and table available or not
        Path sourceUri = Paths.get(csvFile);
        if (!sourceUri.toFile().exists()) {
            logger.warning(String.format("File %s not exists! Skip....", sourceUri));
            return;
        }
        this.createDataSet(datasetName);
        if (this.checkTableExists(datasetName, tableName)) {
            logger.warning(String.format("Table %s already exists. Update the table record", tableName));
            boolean pendingUpdate = this.updateTableRecords(datasetName, tableName, csvFile, clazz);
            if (pendingUpdate) {
                Path path = Paths.get("temp.csv");
                if (path.toFile().exists()) {
                    this.uploadToBigQuery(datasetName, tableName, path, false);
                    Files.delete(path);
                }
            }
        } else {
            this.uploadToBigQuery(datasetName, tableName, sourceUri, true);
        }
        Files.delete(sourceUri);
    }

    /**
     * Upload the CSV file to BigQuery.
     *
     * @param datasetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     * @param path        Path of the CSV file
     * @param autoDetect  Auto detect schema or not
     */
    private void uploadToBigQuery(String datasetName,
                                  String tableName,
                                  Path path,
                                  boolean autoDetect) {
        try {
            TableId tableId = TableId.of(datasetName, tableName);
            FormatOptions formatOptions = CsvOptions.newBuilder().setSkipLeadingRows(1).build();
            WriteChannelConfiguration writeChannelConfiguration = WriteChannelConfiguration.newBuilder(tableId)
                    .setFormatOptions(formatOptions)
                    .setAutodetect(autoDetect)
                    .build();
            JobId jobId = JobId.newBuilder()
                    .setJob(UUID.randomUUID().toString())
                    .setProject(this.bigquery.getOptions().getProjectId())
                    .setLocation(this.bigquery.getDataset(datasetName).getLocation()).build();
            TableDataWriteChannel writer = this.bigquery.writer(jobId, writeChannelConfiguration);
            // Write data to writer
            try (OutputStream stream = Channels.newOutputStream(writer)) {
                Files.copy(path, stream);
            }
            // Close the writer to finalize the job
            writer.close();
            Job loadJob = writer.getJob();
            if (loadJob == null) {
                logger.warning("Job no longer exists");
            } else {
                loadJob = loadJob.waitFor();
                if (loadJob.getStatus().getError() != null) {
                    logger.info(loadJob.getStatus().getError().toString());
                } else {
                    logger.info("CSV data loaded successfully");
                }
            }
        } catch (BigQueryException | InterruptedException | IOException e) {

            logger.warning("Error: CSV data was not loaded. \n" + e);
        }
    }

    /**
     * Update the Drive Activity table using SQL query (for get all records in exist Drive Activity table).
     * This method right now only work with CSV file.
     *
     * @param datasetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     * @param csvFile     Csv file name
     * @param clazz       Class Model of CSV file
     * @return status pending update (if true, the record will be updated)
     */
    private <T> boolean updateTableRecords(String datasetName,
                                           String tableName,
                                           String csvFile,
                                           Class<T> clazz)
            throws IOException, InterruptedException {

        //Collect all record in csv file
        List<T> csvRecords;
        List<String> headers;
        HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(clazz);

        FileReader fileReader = new FileReader(csvFile);
        CsvToBean<T> convertRecord2Bean = new CsvToBeanBuilder<T>(fileReader)
                .withType(clazz)
                .withMappingStrategy(strategy)
                .build();
        csvRecords = convertRecord2Bean.parse();
        fileReader.close();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(csvFile));
        String header = bufferedReader.readLine();
        headers = Arrays.asList(header.split(","));
        bufferedReader.close();
        // Run select query to get all record from table to filter the record
        TableResult result = this.sqlExecute.executeSelectQuery(this.bigquery,
                datasetName, tableName, null, null);
        return this.pendingUpdateRecords(datasetName, tableName, headers, csvRecords, result);
    }

    /**
     * This method will write new record in temporary CSV file to update the table later
     *
     * @param datasetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     * @param headers     List of headers in the CSV file
     * @param csvRecords  List of records in the CSV file
     * @param result      TableResult from the SQL query
     * @return true if the number count of record updated is more than 0, otherwise false
     */
    private <T> boolean pendingUpdateRecords(String datasetName,
                                             String tableName,
                                             List<String> headers,
                                             List<T> csvRecords,
                                             TableResult result) throws InterruptedException {
        int record_updated = 0;
        List<String> csvRecordsWrite = new ArrayList<>();
        // Write headers to the CSV file if they exist
        if (headers.isEmpty()) {
            logger.info("No headers found in the CSV file\nCheck your CSV file and try again");
            return false;
        }

        ListRecords listRecords = new ListRecords();
        List<DriveActivity> driveActivitiesQuery = listRecords.collectDriveActivityRecord(result);
        if (!driveActivitiesQuery.isEmpty()) {
            // Cast List<T> to List<DriveActivity>
            List<DriveActivity> driveActivityCsvRecords = csvRecords.stream()
                    .filter(record -> record instanceof DriveActivity)
                    .map(record -> (DriveActivity) record)
                    .collect(Collectors.toList());
            List<DriveActivity> duplicateActivities = new ArrayList<>();
            for (DriveActivity driveActivity : driveActivitiesQuery) {
                for (DriveActivity csvRecord : driveActivityCsvRecords) {
                    String csvRecordSubActionResult =
                            this.formattedJsonValue(csvRecord.getSubActionResult());
                    String driveActivitySubActionResult =
                            this.formattedJsonValue(driveActivity.getSubActionResult());
                    if (csvRecord.getTimeActivity().equals(driveActivity.getTimeActivity()) &&
                            csvRecord.getUserAction().equals(driveActivity.getUserAction()) &&
                            csvRecord.getPrimaryAction().equals(driveActivity.getPrimaryAction()) &&
                            csvRecord.getSubAction().equals(driveActivity.getSubAction()) &&
                            csvRecordSubActionResult.equals(driveActivitySubActionResult) &&
                            csvRecord.getSubActionResult().equals(driveActivity.getSubActionResult()) &&
                            csvRecord.getItem().equals(driveActivity.getItem())) {
                        //Collect all duplicate records
                        duplicateActivities.add(csvRecord);
                    }
                }
            }

            //Remove all duplicate records and write to the CSV file
            driveActivityCsvRecords.removeAll(duplicateActivities);
            if (!driveActivityCsvRecords.isEmpty()) {
                for (DriveActivity csvRecord : driveActivityCsvRecords) {
                    String subActionResult = this.formattedJsonValue(csvRecord.getSubActionResult());
                    csvRecordsWrite.add(
                            "\"" + csvRecord.getActivityId() + "\"," +
                                    "\"" + csvRecord.getTimeActivity() + "\"," +
                                    "\"" + csvRecord.getUserAction() + "\"," +
                                    "\"" + csvRecord.getPrimaryAction() + "\"," +
                                    "\"" + csvRecord.getSubAction() + "\"," +
                                    "\"" + csvRecord.getSubActionType() + "\"," +
                                    "\"" + subActionResult + "\"," +
                                    "\"" + csvRecord.getItem() + "\""
                    );
                    record_updated++;
                }
            }
        }

        List<CalendarEvent> calendarEventsQuery = listRecords.collectCalendarEventRecord(result);
        if (!calendarEventsQuery.isEmpty()) {
            // Cast List<T> to List<CalendarEvent>
            List<CalendarEvent> calendarEventsCsvRecords = csvRecords.stream()
                    .filter(record -> record instanceof CalendarEvent)
                    .map(record -> (CalendarEvent) record)
                    .collect(Collectors.toList());
            // duplicateEvents: List of duplicate event calendar record
            List<CalendarEvent> duplicateEvents = new ArrayList<>();
            /*
             calendarEventsUpdated: List of event calendar record that need
             update value using SQL statement
            */
            List<CalendarEvent> calendarEventsUpdated = new ArrayList<>();
            for (CalendarEvent calendarEvent : calendarEventsQuery) {
                for (CalendarEvent csvRecord : calendarEventsCsvRecords) {
                    if (csvRecord.getEventId().equals(calendarEvent.getEventId()) &&
                            csvRecord.getCreatedTime().equals(calendarEvent.getCreatedTime()) &&
                            csvRecord.getUpdatedTime().equals(calendarEvent.getUpdatedTime())) {
                        //Collect all duplicate records
                        duplicateEvents.add(csvRecord);
                    }
                    if (csvRecord.getEventId().equals(calendarEvent.getEventId()) &&
                            csvRecord.getCreatedTime().equals(calendarEvent.getCreatedTime()) &&
                            !csvRecord.getUpdatedTime().equals(calendarEvent.getUpdatedTime())) {
                        //Collect all updated records
                        calendarEventsUpdated.add(csvRecord);
                    }
                }
            }
            calendarEventsCsvRecords.removeAll(duplicateEvents);
            calendarEventsCsvRecords.removeAll(calendarEventsUpdated);
            // Add new records to the CSV file and add it in BigQuery Table as new record
            if (!calendarEventsCsvRecords.isEmpty()) {
                for (CalendarEvent csvRecord : calendarEventsCsvRecords) {
                    String attendees = this.formattedJsonValue(csvRecord.getAttendees());
                    String startTime = UtilsFunction.formatTimeStamp(csvRecord.getStartTime());
                    String endTime = UtilsFunction.formatTimeStamp(csvRecord.getEndTime());
                    csvRecordsWrite.add(
                            "\"" + csvRecord.getEventId() + "\"," +
                                    "\"" + csvRecord.getEventTitle() + "\"," +
                                    "\"" + csvRecord.getEventType() + "\"," +
                                    "\"" + csvRecord.getStatus() + "\"," +
                                    "\"" + csvRecord.getCreatedTime() + "\"," +
                                    "\"" + csvRecord.getUpdatedTime() + "\"," +
                                    "\"" + csvRecord.getCreator() + "\"," +
                                    "\"" + csvRecord.getOrganizer() + "\"," +
                                    "\"" + csvRecord.getEventLink() + "\"," +
                                    "\"" + attendees + "\"," +
                                    "\"" + startTime + "\"," +
                                    "\"" + endTime + "\"," +
                                    "\"" + csvRecord.getTotalTime() + "\""
                    );
                    record_updated++;
                }
            }
            // Update the record in BigQuery Table using SQL statement
            if (!calendarEventsUpdated.isEmpty()) {
                for (CalendarEvent updateRecord : calendarEventsUpdated) {
                    String startTime = UtilsFunction.formatTimeStamp(updateRecord.getStartTime());
                    String endTime = UtilsFunction.formatTimeStamp(updateRecord.getEndTime());
                    updateRecord.setStartTime(startTime);
                    updateRecord.setEndTime(endTime);
                    this.sqlExecute.updateTableRecord(this.bigquery, datasetName, tableName, updateRecord);
                    record_updated++;
                }
            }
        }
        /*
         Create a temporary CSV file for writing if list of record
         to write in CSV file is not empty
        */
        if (!csvRecordsWrite.isEmpty()) {
            String headerLine = String.join(",", headers);
            UtilsFunction.writeToCSV(csvRecordsWrite, headerLine, "temp.csv");
        }
        // Print a message if no records were updated
        if (record_updated == 0) {
            logger.info("No record updated. Stop update method");
            return false;
        } else {
            logger.info(String.format("Total record updated: %s", record_updated));
            return true;
        }
    }

    /**
     * Format the JSON value to be written in CSV file.
     *
     * @param value JSON value
     * @return formatted JSON value
     */
    private String formattedJsonValue(String value) {
        return UtilsFunction.isValidJson(value) ? value.replace("\"", "\"\"") : value;
    }

    /**
     * Method to execute delete dataset.
     *
     * @param dataSetName BigQuery Dataset Name
     */
    public void deleteDataset(String dataSetName) {
        DatasetId datasetId = DatasetId.of(
                this.bigquery.getOptions().getProjectId(), dataSetName);
        BigQuery.DatasetDeleteOption option = BigQuery.DatasetDeleteOption.deleteContents();
        boolean deletedSuccess = this.bigquery.delete(datasetId, option);
        if (deletedSuccess) {
            logger.info("Dataset " + dataSetName + " deleted successfully");
        } else {
            logger.info("Error: Dataset " + dataSetName + " was not deleted");
        }
    }

    /**
     * Method to delete specific table in dataset.
     * This method does not work if dataset in parameter already deleted.
     *
     * @param dataSetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     */
    public void deleteTable(String dataSetName, String tableName) {
        TableId tableId = TableId.of(dataSetName, tableName);
        boolean deletedSuccess = this.bigquery.delete(tableId);
        if (deletedSuccess) {
            logger.info(String.format("Table %s in DataSet %s deleted successfully", tableName, dataSetName));
        } else {
            logger.info(String.format("Error: Table %s in DataSet %s was not deleted", tableName, dataSetName));
        }
    }

    /**
     * Method to add empty column to table.
     *
     * @param dataSetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     * @param columnName  Column Name
     * @param type        Column Type (LegacySQLTypeName)
     * @param mode        Column Mode (NULLABLE, REQUIRED, REPEATED)
     * @param description Column Description
     */
    public void addEmptyColumn(String dataSetName, String tableName,
                               String columnName, StandardSQLTypeName type,
                               Field.Mode mode, String description) {
        try {

            if (!this.checkTableExists(dataSetName, tableName)) {
                logger.info("Error: Table " + tableName + " does not exist!");
            } else {
                Table table = this.bigquery.getTable(dataSetName, tableName);
                Schema schema = table.getDefinition().getSchema();
                FieldList fields = null;
                if (schema != null) {
                    fields = schema.getFields();
                }

                // Create the new field/column
                Field newField = Field.newBuilder(columnName, type)
                        .setDescription(description)
                        .setMode(mode).build();

                // Create a new schema adding the current fields, plus the new one
                List<Field> fieldList = new ArrayList<>();
                if (fields != null) {
                    if (!fields.isEmpty() && newField.getMode() == Field.Mode.REQUIRED) {
                        throw new BigQueryException(400,
                                "Error: Required field (" + columnName + ") cannot be added to an existing schema");
                    }
                    fieldList.addAll(fields);
                }
                fieldList.add(newField);
                Schema newSchema = Schema.of(fieldList);

                // Update the table with the new schema
                Table updatedTable =
                        table.toBuilder().setDefinition(StandardTableDefinition.of(newSchema)).build();
                updatedTable.update();
                logger.info(String.format("Column %s successfully added to table: %s", columnName, tableName));
            }
        } catch (BigQueryException e) {

            logger.info(String.format("Column %s was not added. \n%s", columnName, e));
        }
    }

    /**
     * Using to change column properties in table.
     * Change column name, type is not working with Google BigQuery API, Google Cloud Console, Google Cloud SDK.
     * Using DDL (Data Definition Language) to change column name, type.
     * Change column mode only support from REQUIRED to NULLABLE and use DDL is better choice than
     * Java Client because it required create new schema to update the table.
     *
     * @param dataSetName   BigQuery Dataset Name
     * @param tableName     BigQuery Table Name
     * @param columnName    Column Name
     * @param newColumnName New Column Name
     * @param type          Column Type (StandardSQLTypeName)
     * @param nullable      True if column is nullable, false if column is not nullable.
     *                      Only support change from REQUIRED to NULLABLE.
     * @param defaultValue  Default Value for column
     * @param description   Column Description
     */
    public void changeColumnProperties(String dataSetName, String tableName,
                                       String columnName,
                                       String newColumnName,
                                       StandardSQLTypeName type,
                                       boolean nullable,
                                       String defaultValue,
                                       String description) {
        try {
            if (!this.checkTableExists(dataSetName, tableName)) {
                logger.info("Error: Table " + tableName + " does not exist!");
            } else {
                String columnQuery = newColumnName != null && !newColumnName.isEmpty()
                        ? newColumnName : columnName;
                //Change column name only
                if (newColumnName != null && !newColumnName.isEmpty()) {
                    this.sqlExecute.changeColumnName(this.bigquery, dataSetName, tableName, columnName, newColumnName);
                }
                //Change description only
                if (description != null && !description.isEmpty()) {

                    this.sqlExecute.changeColumnDescription(this.bigquery, dataSetName, tableName, columnQuery, description);
                }
                /*
                Change column data type only
                Recommend change column data type in Google Cloud Console
                */
                if (type != null) {
                    this.sqlExecute.changeColumnDataType(this.bigquery, dataSetName, tableName, columnQuery, type);
                }
                //Change column mode only (Only support change from REQUIRED to NULLABLE)
                Field.Mode mode = this.getColumnMode(dataSetName, tableName, columnQuery);
                if (nullable) {
                    if (mode == Field.Mode.NULLABLE) {
                        logger.info(String.format("Error: Column %s is already NULLABLE. Skip....", columnQuery));
                    } else if (mode == Field.Mode.REPEATED) {
                        logger.info(String.format("Error: Column %s is REPEATED. Skip....", columnQuery));
                    } else {
                        this.sqlExecute.changeColumnNullable(this.bigquery, dataSetName, tableName, columnQuery);
                    }
                }

                if (defaultValue != null && !defaultValue.isEmpty()) {
                    this.sqlExecute.changeColumnDefaultValue(this.bigquery, dataSetName, tableName,
                            columnQuery, mode, defaultValue);
                }
            }
        } catch (InterruptedException e) {

            logger.warning(String.format("Column %s was not updated. \n%s", columnName, e));
        }
    }

    /**
     * Get column mode information.
     *
     * @param dataSetName BigQuery Dataset Name
     * @param tableName   BigQuery Table Name
     * @param columnName  Column Name
     */
    private Field.Mode getColumnMode(String dataSetName, String tableName, String columnName) {
        Table table = this.bigquery.getTable(dataSetName, tableName);
        Schema schema = table.getDefinition().getSchema();
        if (schema != null) {
            FieldList fields = schema.getFields();
            for (Field field : fields) {
                if (field.getName().equals(columnName)) {
                    return field.getMode();
                }
            }
        }
        return null;
    }
}
