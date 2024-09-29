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
public class DriveActivity {
    @CsvBindByName(column = "activityId")
    private String activityId;

    @CsvBindByName(column = "timeActivity")
    private String timeActivity;

    @CsvBindByName(column = "userAction")
    private String userAction;

    @CsvBindByName(column = "primaryAction")
    private String primaryAction;

    @CsvBindByName(column = "subAction")
    private String subAction;

    @CsvBindByName(column = "subActionType")
    private String subActionType;

    @CsvBindByName(column = "subActionResult")
    private String subActionResult;

    @CsvBindByName(column = "item")
    private String item;
}
