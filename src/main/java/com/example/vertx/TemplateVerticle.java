package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;

public class TemplateVerticle extends AbstractVerticle {

  Router router;
  // 1. 声明thymeleaf模板引擎
  ThymeleafTemplateEngine thymeleafTemplateEngine;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);
    // 2. 初始化模板引擎
    thymeleafTemplateEngine = ThymeleafTemplateEngine.create(vertx);

    router.route("/").handler(req -> {
      // 用模板引擎render，第一个参数是要往前端传的数据
      JsonObject jsonObj = new JsonObject();
      jsonObj.put("name", "Hello there");
      thymeleafTemplateEngine.render(jsonObj,
        "templates/index.html",
        bufferAsyncResult -> {
        // 4. 这里是固定的写法
        if (bufferAsyncResult.succeeded()) {
          req.response()
            .putHeader("content-type", "text/html")  // 这里要写text/html
            .end(bufferAsyncResult.result());
        }else{
          System.out.println("ERROR");
        }
      });

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
