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
import lombok.SneakyThrows;
import com.notelysia.gcp.logic.ServiceCredential;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@WebServlet(name = "LogoutWebApp", urlPatterns = "/logout")
public class LogoutClient extends HttpServlet {

    private ServiceCredential serviceCredential;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.serviceCredential = (ServiceCredential) this.getServletContext().getAttribute("googleService");
    }

    @SneakyThrows
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = (String) req.getSession().getAttribute("userId");
        Credential credential = null;
        if (userId == null) {
            resp.sendRedirect("/");
        } else {
            credential = this.serviceCredential.googleAccountAuthorize().loadCredential(userId);
        }
        if (credential == null) {
            resp.sendRedirect("/");
        }
        URL url = new URL("https://accounts.google.com/o/oauth2/revoke?token=" + credential.getAccessToken());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.getResponseCode();
        this.serviceCredential.clearDataStore(userId);
        req.getSession().invalidate();
        resp.sendRedirect("/");
    }
}
