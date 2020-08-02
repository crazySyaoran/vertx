package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class UrlParamsVerticle extends AbstractVerticle {

  Router router;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);

    // 经典模式 xx?yy=zz&aa==bb
    router.route("/").handler(req -> {
      // 获取参数方法
      String page = req.request().getParam("page");
      req.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("Page ", page).toString());
    });

    // rest模式 xx/yy/zz
    router.route("/:page").handler(req -> {
      // 获取参数方法与经典模式是一致的
      System.out.println("INNNN");
      String page = req.request().getParam("page");
      req.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("Page: ", page).toString());
    });



    vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });


  }
}
