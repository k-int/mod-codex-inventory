package org.folio.rest.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Instance.Type;

public class InstanceConvert {
  private InstanceConvert() {
    throw new IllegalStateException("Instance");
  }

  public static Instance invToCodex(JsonObject j) {
    Instance instance = new Instance();
    {
      final String id = j.getString("id");
      if (id != null) {
        instance.setId(id);
      } else {
        throw (new IllegalArgumentException("id missing"));
      }
    }
    {
      final String title = j.getString("title");
      if (title != null) {
        instance.setTitle(title);
      } else {
        throw (new IllegalArgumentException("title missing"));
      }
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
    instance.setType(Type.BOOKS);
    instance.setSource("local");
    return instance;
  }

}
