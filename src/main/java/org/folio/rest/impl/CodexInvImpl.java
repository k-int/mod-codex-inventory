package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.InstanceCollection;
import org.folio.rest.jaxrs.resource.CodexInstancesResource;

public class CodexInvImpl implements CodexInstancesResource {

  private static final Logger logger = LoggerFactory.getLogger("codex.inventory");

  static int init() {
    logger.info("CodexInvImpl started");
    return 0;
  }
  static int x = init();

  private void getUrl(String url, HttpClient client,
    LHeaders okapiHeaders, Handler<AsyncResult<Buffer>> fut) {

    HttpClientRequest req = client.getAbs(url, res -> {
      Buffer b = Buffer.buffer();
      res.handler(b::appendBuffer);
      res.endHandler(r -> {
        client.close();
        if (res.statusCode() == 200) {
          fut.handle(Future.succeededFuture(b));
        } else if (res.statusCode() == 404) {
          fut.handle(Future.succeededFuture(Buffer.buffer())); // empty buffer
        } else {
          fut.handle(Future.failedFuture("Get url " + url + " returned " + res.statusCode()));
        }
      });
    });
    req.setChunked(true);
    for (Map.Entry<String, String> e : okapiHeaders.entrySet()) {
      req.putHeader(e.getKey(), e.getValue());
    }
    req.putHeader("Accept", "application/json");
    req.exceptionHandler(r -> {
      client.close();
      fut.handle(Future.failedFuture(r.getMessage()));
    });
    req.end();
  }

  private void getByQuery(Context vertxContext, String query,
    int offset, int limit, LHeaders okapiHeaders, InstanceCollection col,
    Handler<AsyncResult<Void>> fut) {

    HttpClient client = vertxContext.owner().createHttpClient();
    String url = okapiHeaders.get(XOkapiHeaders.URL) + "/instance-storage/instances?"
      + "offset=" + offset + "&limit=" + limit;
    try {
      if (query != null) {
        url += "&query=" + URLEncoder.encode(query, "UTF-8");
      }
    } catch (UnsupportedEncodingException ex) {
      fut.handle(Future.failedFuture(ex.getMessage()));
      return;
    }
    logger.info("getByQuery url=" + url);
    getUrl(url, client, okapiHeaders, res -> {
      if (res.failed()) {
        logger.warn("getByQuery. getUrl failed " + res.cause());
        fut.handle(Future.failedFuture(res.cause()));
      } else {
        Buffer b = res.result();
        if (b.length() > 0) {
          try {
            // TODO: mapping
            // Json.decodeValue(b.toString(), InstanceCollection.class);
          } catch (Exception e) {
            fut.handle(Future.failedFuture(e));
            return;
          }
        }
        fut.handle(Future.succeededFuture());
      }
    });
  }

  private void getById(String id, Context vertxContext, LHeaders okapiHeaders,
    List<Instance> instances, Handler<AsyncResult<Void>> fut) {

    HttpClient client = vertxContext.owner().createHttpClient();
    final String url = okapiHeaders.get(XOkapiHeaders.URL) + "/instance-storage/instances/" + id;
    logger.info("getById url=" + url);
    getUrl(url, client, okapiHeaders, res -> {
      if (res.failed()) {
        logger.warn("getById. getUrl failed " + res.cause());
        fut.handle(Future.failedFuture(res.cause()));
      } else {
        Instance instance = null;
        try {
          if (res.result().length() > 0) {
            // TODO: mapping
            instance = Json.decodeValue(res.result().toString(), Instance.class);
          }
        } catch (Exception e) {
          fut.handle(Future.failedFuture(e));
          return;
        }
        instances.add(instance);
        fut.handle(Future.succeededFuture());
      }
    });
  }

  @Override
  public void getCodexInstances(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    logger.info("GetCodexInstances");
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCodexInstancesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    logger.info("GetCodexInstancesById");

    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
