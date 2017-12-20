package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.InstanceCollection;
import org.folio.rest.jaxrs.resource.CodexInstancesResource;

public class CodexInvImpl implements CodexInstancesResource {

  private static final Logger logger = LoggerFactory.getLogger("codex.inventory");

  private void getUrl(String url, HttpClient client,
    LHeaders okapiHeaders, Handler<AsyncResult<Buffer>> fut) {

    HttpClientRequest req = client.getAbs(url, res -> {
      Buffer b = Buffer.buffer();
      res.handler(b::appendBuffer);
      logger.info("getUrl " + url + " returned " + res.statusCode());
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
      if (!e.getKey().equalsIgnoreCase(XOkapiHeaders.URL)) {
        req.putHeader(e.getKey(), e.getValue());
      }
    }
    req.putHeader("Accept", "application/json");
    req.exceptionHandler(r -> {
      client.close();
      fut.handle(Future.failedFuture(r.getMessage()));
    });
    req.end();
  }

  static Map<String, String> contributorNameTypeIdMap = new LinkedHashMap<>();
  static Map<String, String> instanceTypeMap = new LinkedHashMap<>();
  static Map<String, String> instanceFormatMap = new LinkedHashMap<>();
  static Map<String, String> identifierTypeMap = new LinkedHashMap<>();

  private void getMap(Context vertxContext, LHeaders headers, Map<String, String> map,
    String path, String rootElement, Handler<AsyncResult<Void>> fut) {

    HttpClient client = vertxContext.owner().createHttpClient();
    int offset = map.size();
    int chunk = 10;
    final String url = headers.get(XOkapiHeaders.URL) + path
      + "?limit=" + Integer.toString(chunk)
      + "&offset=" + Integer.toString(offset);
    logger.info("GetMap " + url);
    getUrl(url, client, headers, res -> {
      if (res.succeeded()) {
        try {
          JsonObject j = new JsonObject(res.result().toString());
          JsonArray a = j.getJsonArray(rootElement);
          if (a == null) {
            fut.handle(Future.failedFuture("missing " + rootElement));
            return;
          }
          logger.info(rootElement);
          for (int i = 0; i < a.size(); i++) {
            JsonObject ji = a.getJsonObject(i);
            final String id = ji.getString("id");
            final String name = ji.getString("name");
            logger.info(" " + id + "=" + name);
            map.put(id, name);
          }
          if (map.isEmpty()) {
            fut.handle(Future.failedFuture(rootElement + " is empty"));
          } else if (a.size() == chunk) {
            getMap(vertxContext, headers, map, path, rootElement, fut);
          } else {
            fut.handle(Future.succeededFuture());
          }
        } catch (Exception e) {
          fut.handle(Future.failedFuture(e));
        }
      } else {
        fut.handle(Future.failedFuture(res.cause()));
      }
    });
  }

  private void getMaps(Context context, LHeaders headers, Handler<AsyncResult<Void>> fut) {
    if (contributorNameTypeIdMap.isEmpty()) {
      getMap(context, headers, contributorNameTypeIdMap, "/contributor-name-types", "contributorNameTypes",
        res -> {
          if (res.succeeded()) {
            getMaps(context, headers, fut);
          } else {
            fut.handle(Future.failedFuture(res.cause()));
          }
        });
    } else if (instanceTypeMap.isEmpty()) {
      getMap(context, headers, instanceTypeMap, "/instance-types", "instanceTypes",
        res -> {
          if (res.succeeded()) {
            getMaps(context, headers, fut);
          } else {
            fut.handle(Future.failedFuture(res.cause()));
          }
        });
    } else if (instanceFormatMap.isEmpty()) {
      getMap(context, headers, instanceFormatMap, "/instance-formats", "instanceFormats",
        res -> {
          if (res.succeeded()) {
            getMaps(context, headers, fut);
          } else {
            fut.handle(Future.failedFuture(res.cause()));
          }
        });
    } else if (identifierTypeMap.isEmpty()) {
      getMap(context, headers, identifierTypeMap, "/identifier-types", "identifierTypes",
        res -> {
          if (res.succeeded()) {
            getMaps(context, headers, fut);
          } else {
            fut.handle(Future.failedFuture(res.cause()));
          }
        });
    } else {
      logger.info("All maps fetched");
      fut.handle(Future.succeededFuture());
    }
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
        logger.info("getByQuery succeeded. Analyzing results");
        try {
          InstanceConvert.invToCollection(new JsonObject(b.toString()), col,
            contributorNameTypeIdMap, instanceTypeMap,
            instanceFormatMap, identifierTypeMap);
        } catch (Exception e) {
          logger.warn(e);
          fut.handle(Future.failedFuture(e.getMessage()));
          return;
        }
        fut.handle(Future.succeededFuture());
      }
    });
  }

  private void getById(String id, Context vertxContext, LHeaders okapiHeaders,
    Instance instance, Handler<AsyncResult<Void>> fut) {

    HttpClient client = vertxContext.owner().createHttpClient();
    final String url = okapiHeaders.get(XOkapiHeaders.URL) + "/instance-storage/instances/" + id;
    logger.info("getById url=" + url);
    getUrl(url, client, okapiHeaders, res -> {
      if (res.failed()) {
        logger.warn("getById. getUrl failed " + res.cause());
        fut.handle(Future.failedFuture(res.cause()));
      } else {
        try {
          if (res.result().length() > 0) {
            JsonObject j = new JsonObject(res.result().toString());
            InstanceConvert.invToCodex(j, instance,
              contributorNameTypeIdMap, instanceTypeMap,
              instanceFormatMap, identifierTypeMap);
          }
        } catch (Exception e) {
          logger.warn(e);
          fut.handle(Future.failedFuture(e.getMessage()));
          return;
        }
        fut.handle(Future.succeededFuture());
      }
    });
  }

  @Override
  public void getCodexInstances(String query, int offset, int limit, String lang,
          Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> handler,
          Context vertxContext) throws Exception {

    logger.info("GetCodexInstances");
    LHeaders lHeaders = new LHeaders(okapiHeaders);

    getMaps(vertxContext, lHeaders, res1 -> {
      if (res1.failed()) {
        handler.handle(Future.succeededFuture(
          CodexInstancesResource.GetCodexInstancesResponse.withPlainInternalServerError(res1.cause().getMessage())));
      } else {
        InstanceCollection col = new InstanceCollection();
        getByQuery(vertxContext, query, offset, limit, lHeaders, col, res2 -> {
          if (res2.failed()) {
            handler.handle(Future.succeededFuture(
              CodexInstancesResource.GetCodexInstancesResponse.withPlainInternalServerError(res2.cause().getMessage())));
          } else {
            handler.handle(Future.succeededFuture(
              CodexInstancesResource.GetCodexInstancesResponse.withJsonOK(col)));
          }
        });
      }
    });
  }

  @Override
  public void getCodexInstancesById(String id, String lang,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> handler,
    Context vertxContext) throws Exception {

    logger.info("GetCodexInstancesById");
    if (!id.matches("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$")) {
      handler.handle(Future.succeededFuture(
        CodexInstancesResource.GetCodexInstancesByIdResponse.withPlainNotFound(id)));
      return;
    }
    LHeaders lHeaders = new LHeaders(okapiHeaders);
    getMaps(vertxContext, lHeaders, res1 -> {
      if (res1.failed()) {
        handler.handle(Future.succeededFuture(
          CodexInstancesResource.GetCodexInstancesResponse.withPlainInternalServerError(res1.cause().getMessage())));
      } else {
        Instance instance = new Instance();
        getById(id, vertxContext, lHeaders, instance, res2 -> {
          if (res2.failed()) {
            handler.handle(Future.succeededFuture(
              CodexInstancesResource.GetCodexInstancesByIdResponse.withPlainInternalServerError(res2.cause().getMessage())));
          } else {
            if (instance.getId() == null) {
              handler.handle(Future.succeededFuture(
                CodexInstancesResource.GetCodexInstancesByIdResponse.withPlainNotFound(id)));
            } else {
              handler.handle(Future.succeededFuture(
                CodexInstancesResource.GetCodexInstancesByIdResponse.withJsonOK(instance)));
            }
          }
        });
      }
    });
  }
}
