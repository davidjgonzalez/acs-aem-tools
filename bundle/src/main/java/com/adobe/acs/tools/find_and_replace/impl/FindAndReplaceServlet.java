package com.adobe.acs.tools.find_and_replace.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
@SlingServlet(
        label = "ACS AEM Tools - Find and Replace Servlet",
        resourceTypes = "acs-tools/components/find-and-replace",
        methods = "POST",
        selectors = "find-and-replace",
        extensions = "json"
)
public class FindAndReplaceServlet extends SlingAllMethodsServlet {
    //@Reference
    //private PackageHelper packageHelper;

    @Reference
    private QueryBuilder queryBuilder;

    private static final int SAVE_THRESHOLD = 1000;

    @Override
    protected final void doPost(SlingHttpServletRequest request,
                                SlingHttpServletResponse response) throws ServletException, IOException {

        try {
            String pathToSearch = request.getParameter("search_path");
            String searchString = request.getParameter("search_string");
            String replaceString = request.getParameter("replace_string");
            String searchComponent = request.getParameter("search_component");
            String searchElement = request.getParameter("search_element");
            String updateReferences = request.getParameter("update_references");

            boolean dryRun = !updateReferences.equalsIgnoreCase("replace");
            int dirtyCount = 0;

            final Set<String> successList = new HashSet<String>();
            final Set<String> errorList = new HashSet<String>();

            final ResourceResolver resourceResolver = request.getResourceResolver();
            final Session session = resourceResolver.adaptTo(Session.class);

            final Map<String, String> map = new HashMap<String, String>();

            map.put("path", pathToSearch);
            map.put("type", searchElement);

            if (StringUtils.isNotBlank(searchComponent)) {
                map.put("property", "sling:resourceType");
                map.put("property.value", searchComponent);
            }

            map.put("p.limit", "-1");

            final Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
            final SearchResult queryResult = query.getResult();

            int totalProcessed = 0;
            for (final Hit hit : queryResult.getHits()) {
                try {
                    final Resource resource = resourceResolver.resolve(hit.getPath());
                    final Node node = resource.adaptTo(Node.class);

                    if (node.isNodeType(NameConstants.NT_PAGE)) {
                        // Do not attempt to modify cq:Page nodes
                        continue;
                    }

                    boolean dirty = false;
                    final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

                    for (final Map.Entry<String, Object> entry : mvm.entrySet()) {

                        if (entry.getValue() instanceof String) {

                            /** String Value **/

                            final String originalValue = (String) entry.getValue();
                            final String newValue = originalValue.replaceAll(searchString, replaceString);

                            if (!StringUtils.equals(originalValue, newValue)) {
                                mvm.put(entry.getKey(), newValue);
                                dirty = true;
                            }

                        } else if (entry.getValue() instanceof String[]) {

                            /** String Array Value **/

                            final String[] values = (String[]) entry.getValue();

                            boolean dirtyArray = false;

                            for (int i = 0; i < values.length; i++) {

                                final String originalValue = values[i];
                                final String newValue = originalValue.replaceAll(searchString, replaceString);

                                if (!StringUtils.equals(originalValue, newValue)) {
                                    dirtyArray = true;
                                    values[i] = newValue;
                                }
                            }

                            if (dirtyArray) {
                                // If any element in the Array is dirty (was replaced) then update the entire
                                // property and mark the overall process as being dirty
                                mvm.put(entry.getKey(), values);
                                dirty = true;
                            }

                        } // End property updating

                    } // End for loop over each property on matching node

                    if (dirty) {
                        dirtyCount++;
                        successList.add(resource.getPath());
                    }

                    /** Handle batch saves based on the SAVE_THRESHOLD **/

                    if (dirtyCount == SAVE_THRESHOLD) {
                        if (!dryRun) {
                            session.save();
                        }

                        dirtyCount = 0;
                    }

                    totalProcessed++;


                } catch (Exception e) {
                    errorList.add(hit.getPath());
                }
            }

            /** For loop completed; Save and stragglers */

            if (!dryRun && dirtyCount > 0) {
                session.save();
            } // End result for loop

            //message.println(String.format("Completed the action %s.", updateReferences));
            //message.println(String.format("Found %d matches out of %d total resources.", updatedResources.size(),
            //                        totalProcessed));


                /*

                // Packaging should happen BEFORE the replacements happen

                if (updateReferences.equalsIgnoreCase("package")
                        && !packageResources.isEmpty()) {
                    final Map<String, String> packageDefinitionProperties = new HashMap<String, String>();
                    // Package Description
                    packageDefinitionProperties.put(
                            JcrPackageDefinition.PN_DESCRIPTION,
                            "ACS AEM Tools - Find and Replace - Backup content package");

                    final JcrPackage jcrPackage = packageHelper.createPackage(
                            packageResources, request.getResourceResolver()
                                    .adaptTo(Session.class), "backup",
                            "content replace", "1",
                            PackageHelper.ConflictResolution.IncrementVersion,
                            packageDefinitionProperties);

                    message.append("A package has been created at: "
                            + jcrPackage.getNode()
                            + ". Go to the CRX Package manager to build "
                            + "and download this package before doing "
                            + "the replace." + "\n");

                    message.append("Json is "
                            + packageHelper.getSuccessJSON(jcrPackage));
                }
                */

            response.setContentType("application/json");
            response.getWriter().print(this.getSuccessJSON(successList, errorList, totalProcessed).toString());

        } catch (Exception e) {
            response.setContentType("application/json");
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            try {
                response.getWriter().print(this.getErrorJSON(e).toString());
            } catch (JSONException e1) {

            }
        }
    }

    private JSONObject getSuccessJSON(Collection<String> success, Collection<String> errors, int total)
            throws JSONException {
        final JSONObject result = new JSONObject();

        if(success.size() > 0 && errors.size() > 0) {
            result.put("status", "notice");
        } else if(success.size() > 0) {
            result.put("status", "success");
        } else {
            result.put("status", "error");
        }

        result.put("total", total);

        for (final String path : success) {
            result.accumulate("successList", path);
        }

        for (final String path : errors) {
            result.accumulate("errorList", path);
        }

        return result;
    }

    private JSONObject getErrorJSON(Exception e) throws JSONException {
        final JSONObject result = new JSONObject();

        result.put("status", "error");
        result.put("message", e.getMessage());

        return result;
    }
}
