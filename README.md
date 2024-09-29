# Google Cloud Demo instruction
<a name="readme-top"></a>

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/2dgirlismywaifu/Google-Cloud-API-Demo">
    <img src="manual/google-cloud.png" alt="Logo" width="200" height="200">
  </a>
<h3 align="center", style="font-size:25px">Google Cloud Demo</h3>
<p1 align="center", style="font-size:20px">This project for demonstration</p1><br />
<hr>

![Contributors][contributors-shield]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
![Reposize][size-shield]
![Lastcommit][commit-shield]
[![Issues][issues-shield]][issues-url]
[![APACHE License][license-shield]][license-url]

</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#1-uris-usage">URIs usage</a></li>
    <li>
        <a href="#2-prepare-the-program">Prepare the program</a>
        <ul>
            <li><a href="#21-enable-google-api">Enable Google API</a></li>
            <li><a href="#22-prepare-oauth2-consent-screen">Prepare OAuth2 consent-screen</a></li>
            <li><a href="#23-settings-credential">Settings Credential</a></li>
            <li><a href="#24-settings-applicationproperties">Settings application.properties</a></li>
        </ul>
    </li>
    <li><a href="#3-run-the-program">Run the program</a></li>
  </ol>
</details>
<br />

### 1. URIs usage
| URI                    | Description                         |
|------------------------|-------------------------------------|
| "/"                    | Home Page                           |
| "/login"               | Login Google Account using Oauth2   |
| "/logout"              | Logout the program                  |
| "/drive-activity"      | Collection Google Drive Activity    |
| "/calendar-collection" | Collection event in Google Calendar |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### 2. Prepare the program
- Using Google Account have full permission in Google Cloud Console
- Create an empty project in Google Cloud Console or use existing project
- Install Google Cloud SDK and component `app-engine-java` after install and setup SDK complete
    ```sh
    gcloud components install app-engine-java
    ```
#### 2.1. Enable Google API
- This source use API bellow:

    | Google API         | Description                                                              |
    |--------------------|--------------------------------------------------------------------------|
    | Drive Activity API | Collect Drive Activity Log in Google Drive                               |
    | Calendar API       | Collect event in Google Calendar                                         |
    | Big Query API      | Execute SQL, control job in BigQuery                                     |
    | People API         | Convert Google Account ID have format `People/ID` to another information |
    | App Engine         | Deploy project to App Engine to run like a servlet                       |

#### 2.2. Prepare OAuth2 consent screen
- Go to `APIs & Services` -> `OAuth consent screen`
    - Select `Internal` if project only work with Google Workspace
    - Select `External` if project work outside Google Workspace (it does not mean Google Workspace Account not working)

    ![OAuth Application Type][select-application-type]
- The `OAuth consent screen` section display, the following items are required
    - App information: App name, User support email

        ![OAuth Information][oauth-screen-appinformation]
    - Developer contact information: Email addresses

        ![OAuth Developer Contact][oauth-screen-develop-contact]
- The `Scopes` section display, this item can skip and setup later
- The `Test users` section display, this item only show if selection is `External`
    - A maximum of 100 Google accounts can only be added to log in
    - These additional accounts can be personal Google account or Google Workspace accounts

    ![OAuth Test Users][oauth-screen-test-user]
- The `Summary` section displays the results of the setup process. If there are missing settings or want to change, press `Edit` on each item
- These items can be adjusted later if necessary
#### 2.3. Settings Credential
- Go to APIs & Services -> Credentials
- First select `OAuth client ID` and set `Application Type`, `Name` and `Authorized redirect URIs` information

    ![OAuth Credential Setup][oauth-credential-setup]
- Once created, download that credential and save it at `<source-folder>/src/main/resources`

    ![OAuth Credential Download][oauth-credential-download]
- Next, still in the `Credentials` section, create `Service Account`
    - At `Service Account Details` section, fill in the information as shown below

        ![Create Service Account][service-account-create]
    - At `Grant this service account access to project` section, sets the following permissions

        | Permission                              | Descriptions                          |
        |-----------------------------------------|---------------------------------------|
        | Artifact Registry Create-on-Push Writer | Required for deployment on App Engine |
        | BigQuery Admin                          | Working with BigQuery Tables          |
        | Cloud Datastore User                    | Required for deployment on App Engine |
        | Cloud Datastore Viewer                  | Required for deployment on App Engine |
        | Service Usage Admin                     | Execute jobs created under JobID      |
        | Storage Admin                           | Required for deployment on App Engine |

        ![Add Scopes for Service Account][service-account-create-add-scopes]
    - `Grant users access to this service account` section can skip and setup later
- Select the newly created Service Account in the `Credentials` section, save the information as shown below

    ![Service Account Details][service-account-details]
- In the Service Account details page, go to Keys -> Add Key -> Create new key

    ![P12 Key for Service Account][service-account-p12-key]
- The key will be automatically downloaded, and GCP will return a `secret key` message that defaults to `notasecret`. The secret code needs to be changed
    - On Windows use Git Bash. macOS, Linux use the available Terminal. Type the command
        ```sh
          keytool -importkeystore -srckeystore <p12-file>.p12 -srcstoretype PKCS12 -srcstorepass notasecret -destkeystore <new-p12-file>.p12 -deststoretype PKCS12 -deststorepass <new-secret-key>
        ```
    - Save that new key at `<source-folder>/src/main/resources`
#### 2.4. Settings application.properties
- Go to `<source-folder>/src/main/resources/application.properties`
- Set the information according to the table below

    | Property Name            | Description                                            |
    |--------------------------|--------------------------------------------------------|
    | application.name         | Name according to OAuth consent screen                 |
    | credentials.file.path    | The OAuth Client ID json file is stored in `resources` |
    | service.account.clientId | Service Account ID                                     |
    | service.account.email    | Service Account email                                  |
    | gcp.projectId            | Project ID on GCP                                      |
    | p12.file.path            | File p12 of Service Account                            |
    | p12.secret.password      | Secret code of file p12                                |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### 3. Run the program
- Before running, access the `build.gradle` file
- In the appengine -> tools, change `cloudSdkHome` to the path to the `google-cloud-sdk` directory
- To run the program, use the command `gradle appengineRun`
- If the run results return the results as shown below and there are no errors, the program has been tested successfully

    ![App Engine Local][app-engine-start]
- To deploy the program to App Engine, you first need to modify the appengine -> deploy section
    - Replace the value in `projectId` with the GCP project ID
    - Use the command `gradle appengineDeploy`
    ![App Engine Deploy][app-engine-deploy]
    - If you access the returned link, you will receive the following error when logging in
    ![URI Error][sign-in-uri-error]
    - To fix the error:
        - Go back to the Credentials page on GCP under `APIs & Services`
        - Select the OAuth Client ID being used for the source
        - Add the URI highlighted in red in the Deployed results image. For example
            - https://rosy-embassy-433202-n7.uc.r.appspot.com
            - https://rosy-embassy-433202-n7.uc.r.appspot.com/oauth2callback
        - Save the results and reload those Credentials and save them to the project.
        - Re-implement the program

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->

[contributors-shield]: https://img.shields.io/github/contributors/2dgirlismywaifu/Google-Cloud-API-Demo.svg?style=for-the-badge&color=C9CBFF&logoColor=D9E0EE&labelColor=302D41

[contributors-url]: https://github.com/2dgirlismywaifu/Google-Cloud-API-Demo/graphs/contributors

[forks-shield]: https://img.shields.io/github/forks/2dgirlismywaifu/Google-Cloud-API-Demo.svg?style=for-the-badge&color=C9CBFF&logoColor=D9E0EE&labelColor=302D41

[forks-url]: https://github.com/2dgirlismywaifu/Google-Cloud-API-Demo/network/members

[stars-shield]: https://img.shields.io/github/stars/2dgirlismywaifu/Google-Cloud-API-Demo.svg?style=for-the-badge&color=C9CBFF&logoColor=D9E0EE&labelColor=302D41

[size-shield]: https://img.shields.io/github/repo-size/2dgirlismywaifu/Google-Cloud-API-Demo.svg?style=for-the-badge&color=C9CBFF&logoColor=D9E0EE&labelColor=302D41

[linecount-shield]: https://img.shields.io/tokei/lines/github/2dgirlismywaifu/Google-Cloud-API-Demo?color=C9CBFF&labelColor=302D41&style=for-the-badge

[commit-shield]: https://img.shields.io/github/last-commit/2dgirlismywaifu/Google-Cloud-API-Demo.svg?style=for-the-badge&color=C9CBFF&logoColor=D9E0EE&labelColor=302D41

[stars-url]: https://github.com/2dgirlismywaifu/Google-Cloud-API-Demo/stargazers

[issues-shield]: https://img.shields.io/github/issues/2dgirlismywaifu/Google-Cloud-API-Demo.svg?style=for-the-badge&color=C9CBFF&logoColor=D9E0EE&labelColor=302D41

[issues-url]: https://github.com/2dgirlismywaifu/Google-Cloud-API-Demo/issues

[license-shield]: https://img.shields.io/github/license/2dgirlismywaifu/Google-Cloud-API-Demo.svg?style=for-the-badge&color=C9CBFF&logoColor=D9E0EE&labelColor=302D41

[license-url]: https://github.com/2dgirlismywaifu/Google-Cloud-API-Demo/blob/main/LICENSE

[select-application-type]: manual/select-oauth-application-type.png
[oauth-screen-appinformation]: manual/oauth-screen-appinformation.png
[oauth-screen-develop-contact]: manual/oauth-screen-develop-contact.png
[oauth-screen-test-user]: manual/oauth-screen-test-users.png
[oauth-credential-setup]: manual/oauth-credential-setup.png
[oauth-credential-download]: manual/oauth-credential-download.png
[service-account-create]: manual/service-account-create.png
[service-account-create-add-scopes]: manual/service-account-create-add-scopes.png
[service-account-details]: manual/service-account-details.png
[service-account-p12-key]: manual/service-account-p12-key.png
[app-engine-start]: manual/app-engine-start.png
[app-engine-deploy]: manual/app-engine-deploy.png
[sign-in-uri-error]: manual/sign-in-uri-error.png