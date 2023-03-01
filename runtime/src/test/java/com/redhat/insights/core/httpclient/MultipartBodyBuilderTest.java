/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.httpclient;

import static org.junit.jupiter.api.Assertions.*;

import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.FileUpload;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MultipartBodyBuilderTest {

  private static String payloadFromPublisher(MultipartBodyBuilder bodyBuilder) {
    StringBuilder body = new StringBuilder();
    bodyBuilder
        .bodyPublisher()
        .subscribe(
            new Flow.Subscriber<ByteBuffer>() {
              @Override
              public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
              }

              @Override
              public void onNext(ByteBuffer item) {
                body.append(StandardCharsets.UTF_8.decode(item).toString());
              }

              @Override
              public void onError(Throwable throwable) {
                fail(throwable);
              }

              @Override
              public void onComplete() {
                // Nothing to do
              }
            });
    return body.toString();
  }

  @Nested
  class Serverless {
    @Test
    void checkMultipartBody() {
      byte[] file1 = "Hello\nworld!".getBytes(StandardCharsets.UTF_8);
      byte[] file2 = new byte[] {1, 2, 3, 4, 5, 6};

      MultipartBodyBuilder bodyBuilder =
          new MultipartBodyBuilder()
              .addFormData("abc", "123")
              .addFormData("def", "456")
              .addFile("file1", "file.txt", "text/plain", file1)
              .addFile("file2", "file.bin", "application/octet-stream", file2)
              .addFormData("ghi", "789")
              .end();

      assertTrue(bodyBuilder.contentTypeHeaderValue().startsWith("multipart/form-data; boundary="));

      String boundary =
          bodyBuilder
              .contentTypeHeaderValue()
              .replace("multipart/form-data; boundary=", "")
              .strip();
      assertDoesNotThrow(() -> UUID.fromString(boundary));

      String bodyString = payloadFromPublisher(bodyBuilder);
      System.out.println(bodyString);

      assertTrue(bodyString.contains("--" + boundary + "\r\n"));

      assertTrue(bodyString.contains("Content-Disposition: form-data; name=\"abc\""));
      assertTrue(bodyString.contains("\r\n\r\n123\r\n--" + boundary + "\r\n"));

      assertTrue(bodyString.contains("Content-Disposition: form-data; name=\"def\""));
      assertTrue(bodyString.contains("\r\n\r\n456\r\n"));

      String filePart =
          "Content-Disposition: form-data; name=\"file1\"; filename=\"file.txt\"\r\n"
              + "Content-Type: text/plain\r\n"
              + "\r\n"
              + "Hello\n"
              + "world!";
      assertTrue(bodyString.contains(filePart + "\r\n--" + boundary + "\r\n"));

      assertTrue(bodyString.endsWith("--" + boundary + "--\r\n"));
    }

    @Test
    void checkPerSpecSanitization() {
      MultipartBodyBuilder bodyBuilder =
          new MultipartBodyBuilder()
              .addFormData("\"foo\r\n", "bar")
              .addFile(
                  "\r\nbar\"\"",
                  "\r\nbar\"\"",
                  "application/\njson",
                  "Yolo".getBytes(StandardCharsets.UTF_8))
              .end();

      String body = payloadFromPublisher(bodyBuilder);
      System.out.println(body);

      assertTrue(body.contains("Content-Disposition: form-data; name=\"%22foo%0D%0A\""));
      assertTrue(
          body.contains(
              "Content-Disposition: form-data; name=\"%0D%0Abar%22%22\";"
                  + " filename=\"%0D%0Abar%22%22\""));
      assertTrue(body.contains("Content-Type: application/%0Ajson"));
    }
  }

  @Nested
  class WithVertxBackend {

    int port;
    Vertx vertx;
    MultiMap headers;
    MultiMap params;
    List<FileUpload> fileUploads;

    @BeforeEach
    void prepare() throws IOException {
      vertx = Vertx.vertx();

      Router router = Router.router(vertx);
      String tempUploadDir = Files.createTempDirectory("vertx-tests").toString();
      router.route().handler(BodyHandler.create(tempUploadDir));
      router
          .post("/a/b/c")
          .handler(
              ctx -> {
                headers = ctx.request().headers();
                params = ctx.request().params();
                fileUploads = ctx.fileUploads();
                ctx.response().putHeader("Content-Type", "text/plain").endAndForget("LGTM");
              });

      HttpServer server = vertx.createHttpServer().requestHandler(router).listenAndAwait(0);
      port = server.actualPort();
    }

    @Test
    void checkMultipartAcceptance() throws IOException, InterruptedException {
      byte[] file1 = "Hello\nworld!".getBytes(StandardCharsets.UTF_8);
      byte[] file2 = new byte[] {1, 2, 3, 4, 5, 6};

      MultipartBodyBuilder bodyBuilder =
          new MultipartBodyBuilder()
              .addFormData("abc", "123")
              .addFormData("def", "456")
              .addFile("file1", "file.txt", "text/plain", file1)
              .addFile("file2", "file.bin", "application/octet-stream", file2)
              .addFormData("ghi", "789")
              .end();

      URI testUrl = URI.create("http://localhost:" + port + "/a/b/c");
      HttpRequest httpRequest =
          HttpRequest.newBuilder()
              .uri(testUrl)
              .timeout(Duration.ofSeconds(3))
              .setHeader(
                  MultipartBodyBuilder.CONTENT_TYPE_HEADER, bodyBuilder.contentTypeHeaderValue())
              .POST(bodyBuilder.bodyPublisher())
              .build();

      HttpClient httpClient = HttpClient.newHttpClient();
      HttpResponse<String> httpResponse =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      assertEquals(200, httpResponse.statusCode());
      assertEquals("LGTM", httpResponse.body());

      assertEquals("123", params.get("abc"));
      assertEquals("456", params.get("def"));
      assertEquals("789", params.get("ghi"));

      assertEquals(2, fileUploads.size());

      FileUpload upload = fileUploads.get(0);
      assertEquals("file.txt", upload.fileName());
      assertEquals("text/plain", upload.contentType());
      byte[] uploadData = Files.readAllBytes(Path.of(upload.uploadedFileName()));
      assertArrayEquals(file1, uploadData);

      upload = fileUploads.get(1);
      assertEquals("file.bin", upload.fileName());
      assertEquals("application/octet-stream", upload.contentType());
      uploadData = Files.readAllBytes(Path.of(upload.uploadedFileName()));
      assertArrayEquals(file2, uploadData);
    }

    @AfterEach
    void cleanup() {
      vertx.closeAndAwait();
    }
  }
}
