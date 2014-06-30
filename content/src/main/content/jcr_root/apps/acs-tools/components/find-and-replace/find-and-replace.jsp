<%--
  #%L
  ACS AEM Tools Package
  %%
  Copyright (C) 2013 Adobe
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
       http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  --%>
<%@page session="false"%><%
%><%@include file="/libs/foundation/global.jsp"%><%

%><!doctype html>
<html ng-app="findAndReplace">
    <head>
        <meta charset="UTF-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

        <title> Find &amp; Replace | ACS AEM Tools</title>

        <cq:includeClientLib css="acs-tools.find-and-replace.app" />
    </head>


    <body id="acs-tools-find-and-replace-app"
          ng-controller="MainCtrl"
          ng-init="app.uri='${resource.path}.find-and-replace.json';">

        <header class="top">

            <div class="logo">
                <span ng-hide="app.running"><a href="/"><i class="icon-marketingcloud medium"></i></a></span>
                <span ng-show="app.running"><span class="spinner"></span></span>
            </div>

            <nav class="crumbs">
                <a href="/miscadmin">Tools</a>
                <a href="${currentPage.path}.html">Find &amp; Replace</a>
            </nav>

        </header>

        <div class="page" role="main">

            <div class="content">

                <div ng-show="notifications.length > 0">
                    <div ng-repeat="notification in notifications">
                        <div class="alert {{ notification.type }}">
                            <button class="close" data-dismiss="alert">&times;</button>
                            <strong>{{ notification.title }}</strong>

                            <div>{{ notification.message }}</div>
                        </div>
                    </div>
                </div>

                <div class="app-running notice large alert" ng-show="app.running">
                    <strong>Find &amp; Replace is running...</strong>
                    <div>Please be patient. If your repository is large it may take
                        some time for Find &amp; Replace to complete.</div>
                </div>

                <div class="result" ng-show="result.status">
                    <div class="alert large {{ result.status }}">
                        <strong ng-show="result.status === 'success'">
                            Find & Replace completed successfully!
                        </strong>
                        <strong ng-show="result.status === 'notice'">
                            Find & Replace finished with mixed results
                        </strong>
                        <strong ng-show="result.status === 'error'">
                            Find & Replace encountered errors
                        </strong>

                        <div>
                            <p ng-show="result.message">{{ result.message }}</p>


                            <p>A total of {{ result.total }} resources were inspected.</p>

                            <div ng-show="result.status === 'success' || result.status === 'notice'" >
                                {{ (result.successList || []).length }} resources were successfully updated:

                                <ul>
                                    <li ng-repeat="path in result.successList">{{ path }}</li>
                                </ul>

                            </div>

                            <div ng-show="result.status === 'error' || result.status === 'notice'">">
                                {{ (result.errorList || []).length }} resources could NOT be updated:

                                <ul>
                                    <li ng-repeat="path in result.successList">{{ path }}</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>

                <form class="vertical" ng-submit="findAndReplace()" >
                    <section class="fieldset">

                        <div class="form-row">
                            <label class="field-label" for="searchPath" >Search path</label>
                            <input  class="text-field"
                                    type="text"
                                    id="searchPath"
                                    placeholder="/content/geometrixx"
                                    min="1"
                                    ng-required="true"
                                    ng-model="form.searchPath"/>
                        </div>

                        <div class="form-row">
                            <label class="field-label" for="searchComponent">Component type</label>
                            <input class="text-field"
                                   type="text"
                                   id="searchComponent"
                                   placeholder="foundation/components/text"
                                   ng-model="form.searchComponent" />
                        </div>

                        <div class="form-row">
                            <label class="field-label" for="searchElement">Node type</label>
                            <input class="text-field"
                                   type="text"
                                   id="searchElement"
                                   placeholder="nt:unstructured"
                                   ng-required="true"
                                   ng-model="form.searchElement" />
                        </div>

                        <div class="form-row">
                            <label class="field-label" for="searchString">Search for</label>
                            <input class="text-field"
                                   type="text"
                                   id="searchString"
                                   ng-required="true"
                                   ng-model="form.searchString" />
                            <p class="field-instructions">
                                Dynamic example: sab=\&quot;[0-9]*\&quot;
                            </p>
                        </div>

                        <div class="form-row">
                            <label class="field-label" for="replaceString">Replace with</label>
                            <input class="text-field"
                                   type="text"
                                   id="replaceString"
                                   ng-required="true"
                                   ng-model="form.replaceString" />
                        </div>

                        <div class="radio-row">
                            <label><input class="field"
                                      name="updateReferences"
                                      value="replace"
                                      checked=""
                                      type="radio"
                                      ng-model="form.updateReferences"><span>Replace</span></label>

                            <label><input class="field"
                                      name="updateReferences"
                                      value="dryrun"
                                      type="radio"
                                      ng-model="form.updateReferences"><span>Dry run</span></label>
                            <%--
                            <label><input class="field"
                                          name="updateReferences"
                                          value="package"
                                          type="radio"
                                          ng-model="form.updateReferences"><span>Create Backup package of replacing nodes?</span></label>
                            --%>
                        </div>

                        <button class="primary">Find &amp; Replace</button>
                    </section>
                </form>

            </div>
        </div>

        <cq:includeClientLib js="acs-tools.find-and-replace.app" />
    </body>
</html>