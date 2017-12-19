package org.folio.rest.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.folio.rest.jaxrs.model.Contributor;
import org.folio.rest.jaxrs.model.Identifier;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Instance.Type;
import org.folio.rest.jaxrs.model.InstanceCollection;

public class InstanceConvert {
  private InstanceConvert() {
    throw new IllegalStateException("Instance");
  }
  private static final Logger logger = LoggerFactory.getLogger("codex.inventory");

  public static void invToCollection(JsonObject j, InstanceCollection col,
    Map<String, String> contributorTypeMap,
    Map<String, String> instanceTypeMap,
    Map<String, String> instanceFormatMap,
    Map<String, String> identifierTypeMap) {

    logger.info("invToCollection");
    JsonArray a = j.getJsonArray("instances");
    if (a == null) {
      throw (new IllegalArgumentException("instances"));
    }
    List<Instance> l = new LinkedList<>();
    for (int i = 0; i < a.size(); i++) {
      Instance instance = new Instance();
      invToCodex(a.getJsonObject(i), instance, contributorTypeMap,
        instanceTypeMap, instanceFormatMap, identifierTypeMap);
      l.add(instance);
    }
    col.setInstances(l);
    Integer cnt = j.getInteger("totalRecords");
    if (cnt == null) {
      throw (new IllegalArgumentException("totalRecords missing"));
    }
    col.setTotalRecords(cnt);
  }

  public static void invToCodex(JsonObject j, Instance instance,
    Map<String, String> contributorTypeMap,
    Map<String, String> instanceTypeMap,
    Map<String, String> instanceFormatMap,
    Map<String, String> identifierTypeMap) {

    logger.info("invToCodex");
    { // required in codex
      final String id = j.getString("id");
      if (id == null) {
        throw (new IllegalArgumentException("id missing"));
      }
      instance.setId(id);
    }

    { // required in codex
      final String title = j.getString("title");
      if (title == null) {
        throw (new IllegalArgumentException("title missing"));
      }
      instance.setTitle(title);
    }
    {
      JsonArray ar = j.getJsonArray("alternativeTitles");
      if (ar != null && ar.size() > 0) {
        instance.setAltTitle(ar.getString(0));
      }
    }
    {
      JsonArray ar = j.getJsonArray("series");
      if (ar != null && ar.size() > 0) {
        instance.setSeries(ar.getString(0));
      }
    }
    // creators

    {
      JsonArray ar = j.getJsonArray("contributors");
      if (ar != null) {
        Set<Contributor> cl = new HashSet<>();
        for (int i = 0; i < ar.size(); i++) {
          JsonObject ji = ar.getJsonObject(i);
          final String type = contributorTypeMap.get(ji.getString("contributorTypeId"));
          if (type != null) {
            Contributor c = new Contributor();
            c.setName(ji.getString("name"));
            c.setType(type);
            cl.add(c);
          }
        }
        instance.setContributor(cl);
      }
    }

    {
      JsonArray ar = j.getJsonArray("publication");
      if (ar != null && ar.size() > 0) {
        JsonObject ji = ar.getJsonObject(0);
        final String publisher = ji.getString("publisher");
        if (publisher != null) {
          instance.setPublisher(publisher);
        }
        final String date = ji.getString("dateOfPublication");
        if (date != null) {
          instance.setDate(date);
        }
      }
    }

    {
      // required in codex
      final String id = j.getString("instanceTypeId");
      if (id == null) {
        throw (new IllegalArgumentException("instanceTypeId missing"));
      }
      final String name = instanceTypeMap.get(id);
      if (name == null) {
        throw (new IllegalArgumentException("instanceTypeId " + id + " does not exist"));
      }
      final String nName = name.toUpperCase().replaceAll("\\s", "");
      try {
        instance.setType(Type.valueOf(nName));
      } catch (IllegalArgumentException ex) {
        instance.setType(Type.UNSPECIFIED);
      }
    }

    {
      final String id = j.getString("instanceFormatId");
      if (id != null) {
        final String format = instanceFormatMap.get(id);
        if (format == null) {
          throw (new IllegalArgumentException("instanceFormatId " + id + " does not exist"));
        }
        instance.setFormat(format);
      }
    }

    {
      JsonArray ar = j.getJsonArray("identifiers");
      if (ar != null) {
        Set<Identifier> il = new HashSet<>();
        for (int i = 0; i < ar.size(); i++) {
          JsonObject ji = ar.getJsonObject(i);
          final String type = identifierTypeMap.get(ji.getString("identifierTypeId"));
          if (type != null) {
            Identifier identifier = new Identifier();
            identifier.setType(type);
            identifier.setValue(ji.getString("value"));
            il.add(identifier);
          }
        }
        instance.setIdentifier(il);
      }
    }

    { // required in codex
      final String source = j.getString("source");
      if (j == null) {
        throw (new IllegalArgumentException("source missing"));
      }
      instance.setSource(source);
    }

    {
      JsonArray ar = j.getJsonArray("languages");
      if (ar != null) {
        List<String> il = new LinkedList<>();
        for (int i = 0; i < ar.size(); i++) {
          final String lang = ar.getString(i);
          il.add(lang);
        }
        instance.setLanguage(il);
      }
    }
    // Element rights does not exist in inventory
    {
      final String edition = j.getString("edition");
      if (edition != null) {
        instance.setVersion(edition);
      }
    }

    { // TODO: modifiedDate not found in inventory
      final String l = j.getString("modifiedDate");
      if (l != null) {
        instance.setLastModified(l);
      }
    }
  }
}
