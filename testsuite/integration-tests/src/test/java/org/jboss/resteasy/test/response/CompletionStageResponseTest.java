package org.jboss.resteasy.test.response;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.category.ExpectedFailing;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.test.response.resource.AsyncResponseCallback;
import org.jboss.resteasy.test.response.resource.CompletionStageResponseMessageBodyWriter;
import org.jboss.resteasy.test.response.resource.CompletionStageResponseResource;
import org.jboss.resteasy.test.response.resource.CompletionStageResponseTestClass;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * @tpSubChapter CompletionStage response type
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CompletionStageResponseTest {

   static Client client;

   @Deployment
   public static Archive<?> deploy() {
      WebArchive war = TestUtil.prepareArchive(CompletionStageResponseTest.class.getSimpleName());
      war.addClass(CompletionStageResponseTestClass.class);
      war.addAsLibrary(TestUtil.resolveDependency("io.reactivex.rxjava2:rxjava:2.1.3"));
      return TestUtil.finishContainerPrepare(war, null, CompletionStageResponseMessageBodyWriter.class, 
            CompletionStageResponseResource.class, SingleProvider.class,
            AsyncResponseCallback.class);
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, CompletionStageResponseTest.class.getSimpleName());
   }

   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void close() {
      client.close();
      client = null;
   }


   /**
    * @tpTestDetails Resource method returns CompletionStage<String>.
    * @tpSince RESTEasy 4.0
    */
   @Test
   public void testText() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/text")).request();
      Response response = request.get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals(CompletionStageResponseResource.HELLO, entity);

      // make sure the completion callback was called with no error
      request = client.target(generateURL("/callback-called-no-error")).request();
      response = request.get();
      Assert.assertEquals(200, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Resource method returns CompletionStage<Response>.
    * Response has MediaType "text/plain" overriding @Produces("text/xxx").
    * @tpSince RESTEasy 4.0
    */
   @Test
   public void testResponse() throws Exception
   {
      ResteasyClient client = new ResteasyClientBuilder().build();
      Invocation.Builder request = client.target(generateURL("/response")).request();
      Response response = request.get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("text/plain;charset=UTF-8", response.getHeaderString("Content-Type"));
      Assert.assertEquals(CompletionStageResponseResource.HELLO, entity);
   }

   /**
    * @tpTestDetails Resource method returns CompletionStage<CompletionStageResponseTestClass>,
    * where CompletionStageResponseTestClass is handled by CompletionStageResponseMessageBodyWriter,
    * which has annotation @Produces("abc/xyz").
    * @tpSince RESTEasy 4.0
    */
   @Test
   public void testTestClass() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/testclass")).request();
      Response response = request.get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("abc/xyz", response.getHeaderString("Content-Type"));
      Assert.assertEquals("pdq", entity);
   }

   /**
    * @tpTestDetails Resource method returns CompletionStage<Response>, where the Response 
    * emtity is a CompletionStageResponseTestClass, and where
    * CompletionStageResponseTestClass is handled by CompletionStageResponseMessageBodyWriter,
    * which has annotation @Produces("abc/xyz").
    * @tpSince RESTEasy 4.0
    */
   @Test
   public void testResponseTestClass() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/responsetestclass")).request();
      Response response = request.get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("abc/xyz", response.getHeaderString("Content-Type"));
      Assert.assertEquals("pdq", entity);
   }

   /**
    * @tpTestDetails Resource method return type is CompletionStage<String>, and it passes
    * null to CompleteableFuture.complete().
    * @tpSince RESTEasy 4.0
    */
   @Test
   public void testNull() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/null")).request();
      Response response = request.get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(204, response.getStatus());
      Assert.assertEquals(null, entity);
   }

   /**
    * @tpTestDetails Resource method passes a WebApplicationException to
    * to CompleteableFuture.completeExceptionally().
    * @tpSince RESTEasy 4.0
    */
   @Test
   @Category({ExpectedFailing.class})
   public void testExceptionDelay() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/exception/delay")).request();
      Response response = request.get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(444, response.getStatus());
      Assert.assertEquals(CompletionStageResponseResource.EXCEPTION, entity);

      // make sure the completion callback was called with with an error
      request = client.target(generateURL("/callback-called-with-error")).request();
      response = request.get();
      Assert.assertEquals(200, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Resource method return type is CompletionStage<String>, but it
    * throws a RuntimeException without creating a CompletionStage.
    * @tpSince RESTEasy 4.0
    */
   @Test
   public void testExceptionImmediateRuntime() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/exception/immediate/runtime")).request();
      Response response = request.get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(500, response.getStatus());
      Assert.assertTrue(entity.contains(CompletionStageResponseResource.EXCEPTION));

      // make sure the completion callback was called with with an error
      request = client.target(generateURL("/callback-called-with-error")).request();
      response = request.get();
      Assert.assertEquals(200, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Resource method return type is CompletionStage<String>, but it
    * throws an Exception without creating a CompletionStage.
    * @tpSince RESTEasy 4.0
    */
   @Test
   public void testExceptionImmediateNotRuntime() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/exception/immediate/notruntime")).request();
      Response response = request.get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(500, response.getStatus());
      Assert.assertTrue(entity.contains(CompletionStageResponseResource.EXCEPTION));
      response.close();
      
      // make sure the completion callback was called with with an error
      request = client.target(generateURL("/callback-called-with-error")).request();
      response = request.get();
      Assert.assertEquals(200, response.getStatus());
      response.close();
   }

   /**
    * @tpTestDetails Resource method returns CompletionStage<String>.
    * @tpSince RESTEasy 4.0
    */
   @Test
   public void testTextSingle() throws Exception
   {
      Invocation.Builder request = client.target(generateURL("/textSingle")).request();
      Response response = request.get();
      String entity = response.readEntity(String.class);
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals(CompletionStageResponseResource.HELLO, entity);
   }
}
