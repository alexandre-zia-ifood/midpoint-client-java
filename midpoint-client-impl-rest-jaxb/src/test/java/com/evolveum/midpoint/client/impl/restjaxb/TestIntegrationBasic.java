package com.evolveum.midpoint.client.impl.restjaxb;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.client.api.ObjectReference;
import com.evolveum.midpoint.client.api.SearchResult;
import com.evolveum.midpoint.client.api.exception.ObjectNotFoundException;
import com.evolveum.midpoint.client.api.scripting.ObjectProcessingOutput;
import com.evolveum.midpoint.client.api.scripting.OperationSpecificData;
import com.evolveum.midpoint.client.api.scripting.ValueGenerationData;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteScriptResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

/**
 * This is integration test that requires running midPoint (e.g. from other project).
 * This midPoint must have clean midpoint.home, otherwise some tests fail on conflicts.
 * Because of these prerequisites it is NOT part of the Maven build.
 * TODO: Make it less brittle I guess.
 */
public class TestIntegrationBasic extends AbstractTest {

    private static final String ENDPOINT_ADDRESS = "http://localhost:8080/midpoint/ws/rest";

    private static final String TEST_DIR = "src/test/resources/integration";

    private static final String USER_JACK_OID = "229487cb-59b6-490b-879d-7a6d925dd08c";
    private static final File USER_JACK_FILE = new File(TEST_DIR, "user-jack.xml");
    private static final String REQUEST_DIR = "src/test/resources/request";
    private static final File SCRIPT_GENERATE_PASSWORD = new File(REQUEST_DIR, "request-script-generate-passwords.xml");
    private static final File SCRIPT_MODIFY_VALID_TO = new File(REQUEST_DIR, "request-script-modify-validTo.xml");

    private RestJaxbService service;

    @BeforeClass
    public void initService() throws Exception {
        service = getService();
    }

    @Test
    public void test210createUserjack() throws Exception {
        UserType userJack = unmarshallFromFile(UserType.class, USER_JACK_FILE);
        ObjectReference<UserType> userJackRef = service.users().add(userJack).post();
        UserType userJackAfter = userJackRef.get();

        assertNotNull("Unexpected null object", userJackAfter);
    }

    @Test
    public void test211jackResolvePhoto() throws Exception {
        service.users().oid(USER_JACK_OID).get(null, Collections.singletonList("jpegPhoto"), null);

        URI currentUri = service.getCurrentUri();
        String query = currentUri.getQuery();
        System.out.println("query: " + query);
    }

    @Test
    public void test211jackResolvePhotoAgain() throws Exception {
        service.users().oid(USER_JACK_OID).get(null, Collections.singletonList("jpegPhoto"), null);

        //Check service, if query params where correcly handled
        URI currentUri = service.getCurrentUri();
        String query = currentUri.getQuery();
        System.out.println("query: " + query);
    }

    // see analogous test 520 in midPoint TestAbstractRestService
    @Test
    public void test220GeneratePasswordsUsingScripting() throws Exception {
        // WHEN
        ExecuteScriptType request = unmarshallFromFile(ExecuteScriptType.class, SCRIPT_GENERATE_PASSWORD);
        ExecuteScriptResponseType response = service.rpc().executeScript(request).post();

        // THEN
        List<ObjectProcessingOutput<ValueGenerationData<String>>> outputs = service.scriptingUtil()
                .extractPasswordGenerationResults(response);
        System.out.println("extracted outputs:\n" + outputs);
        AssertJUnit.assertEquals("Wrong # of extracted outputs", 2, outputs.size());

        ObjectProcessingOutput<ValueGenerationData<String>> first = outputs.get(0);
        AssertJUnit.assertEquals("Wrong OID in first output", "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX", first.getOid());
        AssertJUnit.assertEquals("Wrong status in first output", OperationResultStatusType.FATAL_ERROR, first.getStatus());
        AssertJUnit.assertNull("Object present in first output", first.getObject());
        AssertJUnit.assertNotNull("Operation result missing in first output", first.getResult());

        ObjectProcessingOutput<ValueGenerationData<String>> second = outputs.get(1);
        AssertJUnit.assertEquals("Wrong OID in second output", USER_JACK_OID, second.getOid());
        AssertJUnit.assertEquals("Wrong name in second output", "jack", second.getName());
        AssertJUnit.assertTrue("Missing password in second output", StringUtils.isNotBlank(second.getData().getValue()));
        AssertJUnit.assertEquals("Wrong status in second output", OperationResultStatusType.SUCCESS, second.getStatus());
        AssertJUnit.assertNotNull("Object missing in second output", second.getObject());
        AssertJUnit.assertNotNull("Operation result missing in second output", second.getResult());

        assertNull("No query should be here", getQuery(service));
    }

    // see analogous test 530 in midPoint TestAbstractRestService
    @Test
    public void test230ModifyValidToUsingScripting() throws Exception {
        // WHEN
        ExecuteScriptType request = unmarshallFromFile(ExecuteScriptType.class, SCRIPT_MODIFY_VALID_TO);
        ExecuteScriptResponseType response = service.rpc().executeScript(request).post();

        // THEN
        List<ObjectProcessingOutput<OperationSpecificData>> outputs = service.scriptingUtil().extractObjectProcessingOutput(response);
        System.out.println("extracted outputs:\n" + outputs);
        AssertJUnit.assertEquals("Wrong # of extracted outputs", 2, outputs.size());

        ObjectProcessingOutput<OperationSpecificData> first = outputs.get(0);
        AssertJUnit.assertEquals("Wrong OID in first output", "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX", first.getOid());
        AssertJUnit.assertEquals("Wrong status in first output", OperationResultStatusType.FATAL_ERROR, first.getStatus());
        AssertJUnit.assertNull("Object present in first output", first.getObject());
        AssertJUnit.assertNotNull("Operation result missing in first output", first.getResult());

        ObjectProcessingOutput<OperationSpecificData> second = outputs.get(1);
        AssertJUnit.assertEquals("Wrong OID in second output", USER_JACK_OID, second.getOid());
        AssertJUnit.assertEquals("Wrong name in second output", "jack", second.getName());
        AssertJUnit.assertEquals("Wrong status in second output", OperationResultStatusType.SUCCESS, second.getStatus());
        AssertJUnit.assertNotNull("Object missing in second output", second.getObject());
        AssertJUnit.assertNotNull("Operation result missing in second output", second.getResult());

        assertNull("No query should be here", getQuery(service));
    }

    private String test300oid;

    @Test
    public void test300OrgAdd() throws Exception {
        OrgType orgBefore = new OrgType();
        orgBefore.setName(service.util().createPoly("test300"));

        // WHEN
        ObjectReference<OrgType> ref = service.orgs().add(orgBefore).post();

        // THEN
        test300oid = ref.getOid();
        assertNotNull("Null oid", test300oid);

        OrgType orgAfter = ref.get();
        Asserts.assertPoly(service, "Wrong name", "test300", orgAfter.getName());

        assertNull("No query should be here", getQuery(service));
    }

    private String test310oid;

    @Test
    public void test310SubOrgAdd() throws Exception {
        OrgType orgBefore = new OrgType();
        orgBefore.setName(service.util().createPoly("test310"));
        ObjectReferenceType parentRef = new ObjectReferenceType();
        parentRef.setOid(test300oid);
        parentRef.setType(new QName("OrgType"));
        AssignmentType assignment = new AssignmentType();
        assignment.setTargetRef(parentRef);
        orgBefore.getAssignment().add(assignment);

        // WHEN
        ObjectReference<OrgType> ref = service.orgs().add(orgBefore).post();

        // THEN
        test310oid = ref.getOid();
        assertNotNull("Null oid", test310oid);

        OrgType orgAfter = ref.get();
        Asserts.assertPoly(service, "Wrong name", "test310", orgAfter.getName());

        assertNull("No query should be here", getQuery(service));
    }

    private String test320oid;

    @Test
    public void test320SubOrgAdd() throws Exception {
        OrgType orgBefore = new OrgType();
        orgBefore.setName(service.util().createPoly("test320"));
        ObjectReferenceType parentRef = new ObjectReferenceType();
        parentRef.setOid(test310oid);
        parentRef.setType(new QName("OrgType"));
        AssignmentType assignment = new AssignmentType();
        assignment.setTargetRef(parentRef);
        orgBefore.getAssignment().add(assignment);

        // WHEN
        ObjectReference<OrgType> ref = service.orgs().add(orgBefore).post();

        // THEN
        test320oid = ref.getOid();
        assertNotNull("Null oid", test320oid);

        OrgType orgAfter = ref.get();
        Asserts.assertPoly(service, "Wrong name", "test320", orgAfter.getName());
        assertNull("No query should be here", getQuery(service));
    }

    @Test
    public void test330OrgDirectChildSearch() throws Exception {
        // WHEN
        SearchResult<OrgType> result = service.orgs().search()
                .queryFor(OrgType.class)
                .isDirectChildOf(test300oid)
                .get();

        // THEN
        assertEquals(result.size(), 1);
        Asserts.assertPoly(service, "Wrong name", "test310", result.get(0).getName());

        assertNull("No query should be here", getQuery(service));
    }

    @Test
    public void test340OrgChildSearch() throws Exception {
        // WHEN
        SearchResult<OrgType> result = service.orgs().search()
                .queryFor(OrgType.class)
                .isChildOf(test300oid)
                .get();

        // THEN
        assertEquals(result.size(), 2);
        Set<String> names = result.stream()
                .map(org -> service.util().getOrig(org.getName()))
                .collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList("test310", "test320")), names);

        assertNull("No query should be here", getQuery(service));
    }

    @Test
    public void test350RootSearch() throws Exception {
        // WHEN
        SearchResult<OrgType> result = service.orgs().search()
                .queryFor(OrgType.class)
                .isRoot()
                .get();

        // THEN
        Set<String> names = result.stream()
                .map(org -> service.util().getOrig(org.getName()))
                .collect(Collectors.toSet());
        assertTrue("test300 is not among roots", names.contains("test300"));

        assertNull("No query should be here", getQuery(service));
    }

    @Test
    public void test500deleteUserJack() throws Exception {
        service.users().oid(USER_JACK_OID).delete();

        try {
            service.users().oid(USER_JACK_OID).get();
            fail("Unexpected object found");
        } catch (ObjectNotFoundException e) {
            //expected
        }

        assertNull("No query should be here", getQuery(service));
    }

    @Test
    public void test510deteleOrg300() throws Exception {
        service.orgs().oid(test300oid).delete();

        try {
            service.orgs().oid(test300oid).get();
            fail("Unexpected object found");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        assertNull("No query should be here", getQuery(service));
    }

    @Test
    public void test510deteleOrg310() throws Exception {
        service.orgs().oid(test310oid).delete();

        try {
            service.orgs().oid(test310oid).get();
            fail("Unexpected object found");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        assertNull("No query should be here", getQuery(service));
    }

    @Test
    public void test510deteleOrg320() throws Exception {
        service.orgs().oid(test320oid).delete();

        try {
            service.orgs().oid(test320oid).get();
            fail("Unexpected object found");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        assertNull("No query should be here", getQuery(service));
    }

    private RestJaxbService getService() throws IOException {
        return (RestJaxbService) getService(ADMIN, ADMIN_PASS, ENDPOINT_ADDRESS);
    }

    private static String getQuery(RestJaxbService service) {
        URI currentUri = service.getCurrentUri();
        String query = currentUri.getQuery();
        System.out.println("query: " + query);
        return query;
    }

}
