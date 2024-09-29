<%@ page import="com.notelysia.gcp.util.UtilsFunction" %>
<%--
  ~ Copyright @2024 by 2dgirlismywaifu
  ~
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <link href='//fonts.googleapis.com/css?family=Marmelad' rel='stylesheet' type='text/css'>
  <title>Hello App Engine Standard Java 17</title>
</head>
<body>
    <h1>Big Query Client Demo!</h1>
  <table>
    <tr>
      <td colspan="2" style="font-weight:bold;">Available Servlets:</td>
    </tr>
      <% if (UtilsFunction.isCredentialValid(session)) { %>
      <tr>
          <td>Login as: <%= session.getAttribute("googleAccountEmail") %></td>
      </tr>
      <tr>
          <td><a href='${pageContext.request.contextPath}/logout'>Sign out</a></td>
      </tr>
      <% } else { %>
      <tr>
        <td><a href='${pageContext.request.contextPath}/login'>Sign In Google Account</a></td>
      </tr>
      <% } %>
    <tr>
      <td><a href='${pageContext.request.contextPath}/drive-activity'>Collect Drive Activity</a></td>
    </tr>
    <tr>
      <td><a href='${pageContext.request.contextPath}/calendar-collection'>Collect Calendar Event</a></td>
    </tr>
  </table>

</body>
</html>