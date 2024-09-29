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

import com.google.api.services.driveactivity.v2.model.*;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Person;
import lombok.Getter;
import lombok.Setter;
import com.notelysia.gcp.util.UtilsFunction;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
@Setter
public class DriveActivityLogic {
    private static final Logger logger = Logger.getLogger(DriveActivityLogic.class.getName());
    private com.google.api.services.driveactivity.v2.DriveActivity driveActivityService;
    private PeopleService peopleService;
    private Map<String, String> userEmailMap = new HashMap<>();

    public DriveActivityLogic() {
    }


    /**
     * Download the Google Drive activity log.
     * Credentials login from user using Oauth2 flow so no need to use Domain-wide Delegation.
     *
     * @param folderId The folder ID to filter the activities (can be empty String and NULL)
     * @param filter   Filter to apply to the query (can be empty String and NULL)
     * @param csvFile  The name of the csv file to write the activities
     */
    public void DownloadActivity(String folderId,
                                 String filter,
                                 String csvFile) throws IOException {
        //Create an empty list to store the activities and write to the new file
        List<String> activitiesList = new ArrayList<>();
        String csvHeader = "activityId," +
                "timeActivity," +
                "userAction," +
                "primaryAction," +
                "subAction," +
                "subActionType," +
                "subActionResult," +
                "item";

        String nextPageToken = null;
        List<DriveActivity> activities = new ArrayList<>();
        QueryDriveActivityRequest queryRequest = new QueryDriveActivityRequest();
        if (folderId != null && !folderId.isEmpty()) {
            queryRequest.setAncestorName("items/" + folderId);
        }
        queryRequest.setPageSize(10);
        queryRequest.setFilter(filter);
        logger.info("Collect Activity:");
        do {
            queryRequest.setPageToken(nextPageToken);
            QueryDriveActivityResponse result = this.driveActivityService.activity().query(queryRequest).execute();
            if (result.getActivities() != null) {
                activities.addAll(result.getActivities());
            }
            nextPageToken = result.getNextPageToken();
        } while (nextPageToken != null);
        if (!activities.isEmpty()) {
            activities.sort(Comparator.comparing(this::getTimeInfo));
            for (DriveActivity activity : activities) {
                String activityId = UUID.randomUUID().toString();
                String time = this.getTimeInfo(activity);
                String primaryAction = this.getActionInfo(activity.getPrimaryActionDetail());
                List<String> actors =
                        activity.getActors().stream()
                                .map(this::getActorInfo)
                                .collect(Collectors.toList());
                List<String> targets =
                        activity.getTargets().stream()
                                .map(this::getTargetInfo)
                                .collect(Collectors.toList());
                for (Action action : activity.getActions()) {
                    ActionDetail actionDetail = action.getDetail();
                    String subAction = this.getActionInfo(actionDetail);
                    String subActionType = this.getActivityType(actionDetail);
                    String subActionResult = this.getActionResult(actionDetail);
                    activitiesList.add(
                            "\"" + activityId + "\"" + "," +
                                    "\"" + time + "\"" + "," +
                                    "\"" + UtilsFunction.truncated(actors, 2) + "\"" + "," +
                                    "\"" + primaryAction + "\"" + "," +
                                    "\"" + subAction + "\"" + "," +
                                    "\"" + subActionType + "\"" + "," +
                                    "\"" + subActionResult + "\"" + "," +
                                    "\"" + UtilsFunction.truncated(targets, 2) + "\""
                    );
                }
            }
        }

        if (!activitiesList.isEmpty()) {
            UtilsFunction.writeToCSV(activitiesList, csvHeader, csvFile);
        } else {
            logger.info("No activity collected from Google Drive!");
        }
    }

    /**
     * Returns the name of a set property in an object, or else "unknown".
     */
    private <T> String getOneOf(AbstractMap<String, T> obj) {
        Iterator<String> iterator = obj.keySet().iterator();
        return iterator.hasNext() ? iterator.next() : "unknown";
    }

    /**
     * Returns a time associated with an activity.
     */
    private String getTimeInfo(DriveActivity activity) {
        if (activity.getTimestamp() != null) {
            return activity.getTimestamp();
        }
        if (activity.getTimeRange() != null) {
            return activity.getTimeRange().getEndTime();
        }
        return "unknown";
    }

    /**
     * Returns the type of action.
     */
    private String getActionInfo(ActionDetail actionDetail) {
        return this.getOneOf(actionDetail);
    }

    private List<String> getPermissionChange(PermissionChange permissionChange) {
        if (permissionChange != null) {
            if (permissionChange.getAddedPermissions() != null &&
                    !permissionChange.getAddedPermissions().isEmpty()) {
                return permissionChange.getAddedPermissions()
                        .stream()
                        .map(Permission -> {
                            String role = Permission.getRole();
                            String user = "unknown";
                            if (Permission.getUser() != null) {
                                user = Permission.getUser().getKnownUser().getPersonName();
                            }
                            if (Permission.getGroup() != null) {
                                user = Permission.getGroup().getEmail();
                            }
                            if (Permission.getDomain() != null) {
                                user = Permission.getDomain().getName();
                            }
                            if (Permission.getAnyone() != null) {
                                user = "anyone";
                            }
                            return "Role:'" + role + "', User:'" + this.readUserEmail(user) + "'";
                        })
                        .collect(Collectors.toList());
            }

            if (permissionChange.getRemovedPermissions() != null &&
                    !permissionChange.getRemovedPermissions().isEmpty()) {
                return permissionChange.getRemovedPermissions()
                        .stream()
                        .map(Permission -> {
                            String role = Permission.getRole();
                            String user = "unknown";
                            if (Permission.getUser() != null) {
                                user = Permission.getUser().getKnownUser().getPersonName();
                            }
                            if (Permission.getGroup() != null) {
                                user = Permission.getGroup().getEmail();
                            }
                            if (Permission.getDomain() != null) {
                                user = Permission.getDomain().getName();
                            }
                            if (Permission.getAnyone() != null) {
                                user = "anyone";
                            }
                            return "Role:'" + role + "', User:'" + this.readUserEmail(user) + "'";
                        })
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    /**
     * Returns the type of activity.
     *
     * @param actionDetail The action detail
     * @return The type of activity
     */
    private String getActivityType(ActionDetail actionDetail) {
        Comment comment = actionDetail.getComment();
        Create create = actionDetail.getCreate();
        Delete delete = actionDetail.getDelete();
        Edit edit = actionDetail.getEdit();
        Move move = actionDetail.getMove();
        Rename rename = actionDetail.getRename();
        Restore restore = actionDetail.getRestore();
        PermissionChange permissionChange = actionDetail.getPermissionChange();

        if (comment != null) {
            return "commented";
        }
        if (create != null) {
            if (create.getCopy() != null) {
                return "copy";
            }
            if (create.getNew() != null) {
                return "new";
            }
            if (create.getUpload() != null) {
                return "upload";
            }
        }
        if (delete != null) {
            return delete.getType();
        }
        if (edit != null) {
            /*
             If edit not null, it just returns an empty message indicating an object was edited.
             Because message return like this "{}", so the result set to "edit"
            */
            return "edit";
        }
        if (move != null) {
            if (move.getAddedParents() != null && !move.getAddedParents().isEmpty()) {
                return "addedParents";
            }
            if (move.getRemovedParents() != null && !move.getRemovedParents().isEmpty()) {
                return "removedParents";
            }
        }
        if (rename != null) {
            return "rename";
        }
        if (restore != null) {
            return restore.getType();
        }
        if (permissionChange != null) {
            if (permissionChange.getAddedPermissions() != null &&
                    !permissionChange.getAddedPermissions().isEmpty()) {
                return "addedPermissions";
            }

            if (permissionChange.getRemovedPermissions() != null &&
                    !permissionChange.getRemovedPermissions().isEmpty()) {
                return "removedPermissions";
            }
        }
        return "";
    }

    /**
     * Returns the result of action detail.
     *
     * @param actionDetail The action detail
     */
    private String getActionResult(ActionDetail actionDetail) {
        Comment comment = actionDetail.getComment();
        Create create = actionDetail.getCreate();
        Move move = actionDetail.getMove();
        Rename rename = actionDetail.getRename();
        PermissionChange permissionChange = actionDetail.getPermissionChange();
        List<String> permissionChangeList = this.getPermissionChange(permissionChange);
        if (comment != null) {
            StringBuilder commentResult = new StringBuilder("{");
            if (comment.getMentionedUsers() != null && !comment.getMentionedUsers().isEmpty()) {
                commentResult.append("\"\"mentionedUsers\"\": [");
                for (User mentionedUser : comment.getMentionedUsers()) {
                    commentResult.append("\"\"").append(this.getUserInfo(mentionedUser)).append("\"\",");
                }
                commentResult = new StringBuilder(commentResult.substring(0, commentResult.length() - 1) + "]");
            }
            if (comment.getAssignment() != null) {
                if (String.valueOf(commentResult).equals("{")) {
                    commentResult.append("\"\"assignment\"\": {");
                } else {
                    commentResult.append(",\"\"assignment\"\": {");
                }
                commentResult.append("\"\"assignedUser\"\": \"\"")
                        .append(this.getUserInfo(comment.getAssignment().getAssignedUser()))
                        .append("\"\",");
                commentResult.append("\"\"subtype\"\": \"\"")
                        .append(comment.getAssignment().getSubtype())
                        .append("\"\"}");
            }
            if (comment.getPost() != null) {
                if (String.valueOf(commentResult).equals("{")) {
                    commentResult.append("\"\"post\"\": {");
                } else {
                    commentResult.append(",\"\"post\"\": {");
                }
                commentResult.append("\"\"subtype\"\": \"\"")
                        .append(comment.getPost().getSubtype())
                        .append("\"\"}");
            }
            if (comment.getSuggestion() != null) {
                if (String.valueOf(commentResult).equals("{")) {
                    commentResult.append("\"\"suggestion\"\": {");
                } else {
                    commentResult.append(",\"\"suggestion\"\": {");
                }
                commentResult.append("\"\"subtype\"\": \"\"")
                        .append(comment.getSuggestion().getSubtype())
                        .append("\"\"}");
            }
            return commentResult + "}";
        }
        if (create != null) {
            if (create.getCopy() != null) {
                TargetReference targetReference = create.getCopy().getOriginalObject();
                return this.getTargetReference(targetReference);
            }
        }
        if (move != null) {
            if (move.getAddedParents() != null && !move.getAddedParents().isEmpty()) {
                return UtilsFunction.truncated(move.getAddedParents().stream()
                        .map(this::getTargetReference)
                        .collect(Collectors.toList()), 2);
            }
            if (move.getRemovedParents() != null && !move.getRemovedParents().isEmpty()) {
                return UtilsFunction.truncated(move.getRemovedParents().stream()
                        .map(this::getTargetReference)
                        .collect(Collectors.toList()), 2);
            }
        }
        if (rename != null) {
            return "[oldTitle:'" + rename.getOldTitle() + "', " +
                    "newTitle:'" + rename.getNewTitle() + "']";
        }
        if (permissionChangeList != null) {
            return UtilsFunction.truncated(permissionChangeList, 2);
        }
        return "";
    }

    /**
     * Returns user information, or the type of user if not a known user.
     */
    private String getUserInfo(User user) {
        if (user.getKnownUser() != null) {
            KnownUser knownUser = user.getKnownUser();
            return this.readUserEmail(knownUser.getPersonName());
        }
        return this.getOneOf(user);
    }

    /**
     * Returns actor information, or the type of actor if not a user.
     */
    private String getActorInfo(Actor actor) {
        if (actor.getUser() != null) {
            return this.getUserInfo(actor.getUser());
        }
        return this.getOneOf(actor);
    }

    /**
     * Returns the type of target and an associated title.
     */
    private String getTargetInfo(Target target) {
        if (target.getDriveItem() != null) {
            return "driveItem:'" + target.getDriveItem().getTitle() + "'";
        }
        if (target.getDrive() != null) {
            return "drive:'" + target.getDrive().getTitle() + "'";
        }
        if (target.getFileComment() != null) {
            DriveItem parent = target.getFileComment().getParent();
            if (parent != null) {
                return "fileComment:'" + parent.getTitle() + "'";
            }
            return "fileComment:unknown";
        }
        return this.getOneOf(target);
    }

    private String getTargetReference(TargetReference targetReference) {
        if (targetReference.getDriveItem() != null) {
            return "driveItem:'" + targetReference.getDriveItem().getTitle() + "'";
        }
        if (targetReference.getDrive() != null) {
            return "drive:'" + targetReference.getDrive().getTitle() + "'";
        }
        return this.getOneOf(targetReference);
    }

    /**
     * User has display in format people/USER_ID.
     * This method will get the email address from user id.
     * If credential is Service Account, People API will not work.
     * <a href="https://stackoverflow.com/questions/49064826/using-google-people-api-with-service-account">
     * Using Google People API with Service account</a>.
     * <br/>
     * By default, request to People API to read user profile only accept 60 requests per minute.
     * For more than 60 requests per minute, submit form "Apply for higher quota" in Quotas page.
     *
     * @return email address of the user
     */
    private String readUserEmail(String personalName) {
        /*
        Personal ID format: "people/USER_ID" always never change so use a map to
        store the email address get from People API.
        When call this method, the map will check first before call People API to get email address.
        */
        if (!personalName.matches("^people/[^/]+$")) {
            return personalName;
        }
        if (this.userEmailMap.containsKey(personalName)) {
            return this.userEmailMap.get(personalName);
        } else {
            Person profile;
            try {
                profile = this.peopleService.people().get(personalName)
                        .setPersonFields("names,emailAddresses")
                        .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<EmailAddress> emailAddresses = profile.getEmailAddresses();
            if (emailAddresses != null && !emailAddresses.isEmpty()) {
            /*
            Dummy account or Service Account without Domain-wide Delegation enabled
             still can return own email
            */
                this.userEmailMap.put(personalName, emailAddresses.get(0).getValue());
                return emailAddresses.get(0).getValue();
            } else {
            /*
             Return back to "people/USER_ID"
             if credential is Service Account without Domain-wide Delegation enabled
            */
                this.userEmailMap.put(personalName, personalName);
                return personalName;
            }
        }
    }
}
