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

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeCallbackServlet;
import com.google.api.client.http.GenericUrl;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.Person;
import lombok.SneakyThrows;
import com.notelysia.gcp.logic.ServiceCredential;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.GeneralSecurityException;

@WebServlet(name = "LoginGoogleCallBack", urlPatterns = "/oauth2callback")
public class SignInGoogleCallBack extends AbstractAuthorizationCodeCallbackServlet {

    private ServiceCredential serviceCredential;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.serviceCredential = (ServiceCredential) this.getServletContext().getAttribute("googleService");
    }


    @SneakyThrows
    @Override
    protected AuthorizationCodeFlow initializeFlow() throws ServletException, IOException {
        return this.serviceCredential.googleAccountAuthorize();
    }

    @Override
    protected String getRedirectUri(HttpServletRequest req) throws ServletException, IOException {
        GenericUrl url = new GenericUrl(req.getRequestURL().toString());
        url.setRawPath("/oauth2callback");
        return url.build();
    }

    @Override
    protected String getUserId(HttpServletRequest req) throws ServletException, IOException {
        HttpSession session = req.getSession();
        return (String) session.getAttribute("userId");
    }

    @Override
    protected void onSuccess(HttpServletRequest req, HttpServletResponse resp, Credential credential)
            throws ServletException, IOException {
        try {
            PeopleService peopleService = this.serviceCredential.createPeopleService(credential);
            Person profile = peopleService.people().get("people/me")
                    .setPersonFields("emailAddresses,names")
                    .execute();
            String googleAccountId = profile.getResourceName().replace("people/", "");
            String googleAccountEmail = profile.getEmailAddresses().get(0).getValue();
            req.getSession().setAttribute("googleAccountId", googleAccountId);
            req.getSession().setAttribute("googleAccountEmail", googleAccountEmail);
            resp.sendRedirect("/");
        } catch (GeneralSecurityException e) {
            throw new ServletException(e);
        }
    }

}
