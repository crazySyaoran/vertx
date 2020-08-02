package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class BodyVerticle extends AbstractVerticle {

  Router router;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);

    // 获取body参数，要在router初始化好之后立刻添加这一句
    // > 详见Web - Request body handling
    router.route().handler(BodyHandler.create());

    // form-date模式
    // > 请求头中content-type为application/x-www-form-urlencoded
    router.route("/formData").handler(req -> {
      // 获取FormData参数方法: getFormAttribute
      String page = req.request().getFormAttribute("page");
      req.response()
        .putHeader("content-type", "application/plain")
        .end(page);
    });

    // json模式
    // > 请求头中content-type为application/json
    router.route("/json").handler(req -> {
      // 获取json参数方法
      JsonObject page = req.getBodyAsJson();
      req.response()
        .putHeader("content-type", "application/json")
        .end(page.toString());
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
