package org.folio.rest.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.LinkedList;
import java.util.List;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Instance.Type;
import org.folio.rest.jaxrs.model.InstanceCollection;

public class InstanceConvert {
  private InstanceConvert() {
    throw new IllegalStateException("Instance");
  }

  public static void invToCollection(JsonObject j, InstanceCollection col) {
    JsonArray a = j.getJsonArray("instances");
    if (a == null) {
      throw (new IllegalArgumentException("instances"));
    }
    List<Instance> l = new LinkedList<>();
    for (int i = 0; i < a.size(); i++) {
      Instance instance = new Instance();
      invToCodex(a.getJsonObject(i), instance);
      l.add(instance);
    }
    col.setInstances(l);
    Integer cnt = j.getInteger("totalRecords");
    if (cnt == null) {
      throw (new IllegalArgumentException("totalRecords missing"));
    }
    col.setTotalRecords(cnt);
  }

  public static void invToCodex(JsonObject j, Instance instance) {
    {
      final String id = j.getString("id");
      if (id == null) {
        throw (new IllegalArgumentException("id missing"));
      }
      instance.setId(id);
    }
    {
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
    instance.setType(Type.BOOKS); // TODO: instanceTypeId
    {
      final String source = j.getString("source");
      if (j == null) {
        throw (new IllegalArgumentException("source missing"));
      }
      instance.setSource(source);
    }
    // TODO: More conversions
  }
}
