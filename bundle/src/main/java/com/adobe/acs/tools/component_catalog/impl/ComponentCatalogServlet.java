/*
 * #%L
 * ACS AEM Tools Bundle
 * %%
 * Copyright (C) 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.tools.component_catalog.impl;

import com.adobe.acs.tools.component_catalog.ComponentCatalogCreator;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
@SlingServlet(
        label = "ACS AEM Tools - Component Catalog Creator Servlet",
        description = "...",
        methods = { "GET", "POST" },
        resourceTypes = { "acs-tools/components/component-catalog" },
        extensions = { "json" }
)  */
@org.apache.felix.scr.annotations.Component
@Service
public class ComponentCatalogServlet  extends SlingAllMethodsServlet implements ComponentCatalogCreator {
    private static final Logger log = LoggerFactory.getLogger(ComponentCatalogServlet.class);

    private static final String PN_CQ_TEMPLATE = "cq:template";


    @Reference
    private QueryBuilder queryBuilder;

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");


    }


    @Override
    protected final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        final RequestParameter templateParam = request.getRequestParameter("template");
        final RequestParameter parResourceNameParam = request.getRequestParameter("parResourceName");
        final RequestParameter rootPagePathParam = request.getRequestParameter("rootPagePath");
        final RequestParameter[] componentsParams = request.getRequestParameters("template");


        String template = "";
        if (templateParam != null) {
            template = templateParam.getString();
        }

        String parResourceName = "";
        if (parResourceNameParam != null) {
            parResourceName = parResourceNameParam.getString();
        }

        String rootPagePath = "";
        if (rootPagePathParam != null) {
            rootPagePath = rootPagePathParam.getString();
        }

        List<String> components = new ArrayList<String>();
        if (componentsParams != null) {
            for (RequestParameter componentsParam : componentsParams) {
                if (componentsParam != null) {
                    components.add(componentsParam.getString());
                }
            }
        }

        try {
            createComponentCatalog(request.getResourceResolver(), rootPagePath, parResourceName, template,
                    components.toArray(new String[components.size()]));
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

    public final Map<String, List<Component>> getComponentsByGroup(final ResourceResolver resourceResolver,
                                                                   final String... paths) throws RepositoryException {
        final ComponentManager componentManager = resourceResolver.adaptTo(ComponentManager.class);
        final Map<String, List<Component>> components = new HashMap<String, List<Component>>();
        final Map<String, String> params = new HashMap<String, String>();

        int i = 1;
        for (String path : paths) {
            params.put(i++ + "_path", path);
        }

        params.put("type", "cq:Component");

        final Query query = queryBuilder.createQuery(PredicateGroup.create(params),
                resourceResolver.adaptTo(Session.class));

        final SearchResult result = query.getResult();

        for (final Hit hit : result.getHits()) {
            final Component component = componentManager.getComponent(hit.getPath());
            final String group = component.getComponentGroup();

            List<Component> list = components.get(group);
            if (list == null) {
                list = new ArrayList<Component>();
            }

            list.add(component);

            components.put(group, list);
        }

        return components;
    }


    public final List<String> createComponentCatalog(ResourceResolver resourceResolver,
                                                     String rootPagePath,
                                                     String parResourceName,
                                                     String template,
                                                     String... componentPaths) throws WCMException,
            RepositoryException, PersistenceException {
        log.info("Creating CCC");

        final ComponentManager componentManager = resourceResolver.adaptTo(ComponentManager.class);

        final List<String> pages = new ArrayList<String>();

        final Session session = resourceResolver.adaptTo(Session.class);
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        for (final String componentPath : componentPaths) {
            log.error("Processing component path: ", componentPath);

            final Component component = componentManager.getComponent(componentPath);

            if (component != null) {
                Page componentGroupPage = getOrCreateContainerPage(pageManager, rootPagePath,
                        template, component.getComponentGroup());

                Page page = getOrCreateComponentPage(template, pageManager, component, componentGroupPage);

                addComponentContentResource(parResourceName, session, component, page);

                pages.add(page.getPath());
            }  else {
                log.warn("Component is null @ [ {} ]", componentPath);
            }
        }

        resourceResolver.commit();

        return pages;
    }

    private void addComponentContentResource(final String parResourceName, final Session session,
                                             final Component component, final Page page) throws RepositoryException {

        Node contentNode = session.getNode(page.getContentResource().getPath());

        // Parsys Node

        Node parNode = JcrUtils.getOrCreateByPath(contentNode, parResourceName, false, JcrConstants.NT_UNSTRUCTURED,
                JcrConstants.NT_UNSTRUCTURED, false);
        parNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "foundation/components/parsys");


        if (session.nodeExists(component.getPath() + "/" + PN_CQ_TEMPLATE)) {
            final Node cqTemplateNode = session.getNode(component.getPath() + "/" + PN_CQ_TEMPLATE);
            JcrUtil.copy(cqTemplateNode, parNode, component.getName());
        } else {
            final Node componentNode = parNode.addNode(component.getName());
            componentNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, component.getResourceType());
        }
    }

    private Page getOrCreateComponentPage(final String template, final PageManager pageManager, final Component component, final Page componentGroupPage) throws WCMException {
        Page page = pageManager.create(componentGroupPage.getPath(), component.getName(), template,
                component.getTitle());

        ModifiableValueMap pageProperties = page.getContentResource().adaptTo(ModifiableValueMap.class);

        if (StringUtils.isNotBlank(component.getDescription())) {
            pageProperties.put(JcrConstants.JCR_DESCRIPTION, component.getDescription());
        }
        return page;
    }


    private Page getOrCreateContainerPage(PageManager pageManager, String path, String template,
                                          String title) throws WCMException {

        String name = JcrUtil.createValidName(title, JcrUtil.HYPHEN_LABEL_CHAR_MAPPING);
        String pagePath = path + "/" + name;

        Page page = pageManager.getPage(pagePath);
        if (page == null) {
            page = pageManager.create(path, name, template, title);
        }

        return page;
    }
}
