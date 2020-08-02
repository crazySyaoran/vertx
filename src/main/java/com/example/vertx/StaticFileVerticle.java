package com.example.vertx;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.Log4JLoggerFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;

public class StaticFileVerticle extends AbstractVerticle {

  Router router;
  ThymeleafTemplateEngine thymeleafTemplateEngine;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // 日志初始化
    final InternalLogger logger = Log4JLoggerFactory.getInstance(StaticFileVerticle.class);

    router = Router.router(vertx);
    thymeleafTemplateEngine = ThymeleafTemplateEngine.create(vertx);

    // 这样所有来自路径例如 “/static/css/xx.css” 的静态资源都会到路径 “webroot/css/xx.css”中去寻找
    // > 详见 Web - Serving static resources
    router.route("/*").handler(StaticHandler.create());

    router.route("/").handler(req -> {
      logger.error("测试 - log4j - error - '/'");
      JsonObject jsonObj = new JsonObject();
      jsonObj.put("name", "Hello there");
      thymeleafTemplateEngine.render(jsonObj,
        "templates/index.html",
        bufferAsyncResult -> {
          if (bufferAsyncResult.succeeded()) {
            req.response()
              .putHeader("content-type", "text/html")
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
