package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class RouterVerticle extends AbstractVerticle {

  // 1. 声明全局router，vertx是AbstractVerticle的成员变量，因为vertx在start之后才能初始化，所以必须到start里面初始化
  Router router;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // 2. 初始化router
    router = Router.router(vertx);

    // 3. 配置router解析url
    router.route("/").handler(req -> {
      req.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("Hello", "from Vertx").toString());
    });
    router.get("/qwq").handler(req -> {
      req.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("Hello", "QWQ").toString());
    });

    // 4. 将router与vertx httpserver绑定
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
