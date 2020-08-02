package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;

public class MySqlVerticle extends AbstractVerticle {

  Router router;

  // 1. （全局下）配置连接参数
  MySQLConnectOptions connectOptions = new MySQLConnectOptions()
    .setPort(3306)
    .setHost("127.0.0.1")
    .setDatabase("test")
    .setUser("root")
    .setPassword("sakura");

  // 2. 配置连接池
  // Pool options
  PoolOptions poolOptions = new PoolOptions()
    .setMaxSize(5);

  // 3. 声明数据库连接client
  // > 这里要注意client的初始化需要传一个vertx进去，所以client不能在全局下初始化，只能到start里面初始化
  MySQLPool client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);
    // 初始化连接client，注意这里要把vertx传进去
    client = MySQLPool.pool(vertx,connectOptions, poolOptions);

    // > 详见官网Data - MySQL - connecting to MySQL
    router.route("/sql").handler(req -> {
      // Get a connection from the pool
      client.getConnection(ar1 -> {
        if (ar1.succeeded()) {
          System.out.println("Connected");

          // Obtain our connection
          SqlConnection conn = ar1.result();

          // All operations execute on the same connection
          conn
            .query("SELECT user_id, user_name FROM users")
            .execute(ar2 -> {
              // 注意这里每次执行完一定要手动释放连接池里的连接
              conn.close();
              if (ar2.succeeded()) {
                ArrayList<JsonObject> list = new ArrayList<JsonObject>();
                ar2.result().forEach(item ->{
                    JsonObject json = new JsonObject();
                    json.put("user_id", item.getValue("user_id"));
                    json.put("user_name", item.getValue("user_name"));
                    list.add(json);
                });
                req.response()
                  // 这里要注意是"content-type"不是"context-type"，并且后面要配置成application/json
                  .putHeader("content-type", "application/json")
                  .end(list.toString());
              } else {
                req.response()
                  .putHeader("content-type", "text/plain")
                  .end(ar2.cause().toString());
              }
            });
        } else {
          System.out.println("Could not connect: " + ar1.cause().getMessage());
        }
      });
    });

    router.route("/sql/list").handler(req -> {
      // Get a connection from the pool
      client.getConnection(ar1 -> {
        if (ar1.succeeded()) {
          System.out.println("Connected");
          // Obtain our connection
          SqlConnection conn = ar1.result();

          // 每页5条
          Integer offset = (Integer.parseInt(req.request().getParam("page"))-1)*5;

          // All operations execute on the same connection
          conn
            // 这里注意要使用preparedQuery来处理可变参数，待传递的参数用?表示
            .preparedQuery("SELECT user_id, user_name FROM users limit 5 offset ?")
            // 通过在execute中加入Tuple.of(变量名)来传参
            .execute(Tuple.of(offset), ar2 -> {
              // 注意这里每次执行完一定要手动释放连接池里的连接
              conn.close();
              if (ar2.succeeded()) {
                ArrayList<JsonObject> list = new ArrayList<JsonObject>();
                ar2.result().forEach(item ->{
                  JsonObject json = new JsonObject();
                  json.put("user_id", item.getValue("user_id"));
                  json.put("user_name", item.getValue("user_name"));
                  list.add(json);
                });
                req.response()
                  // 这里要注意是"content-type"不是"context-type"，并且后面要配置成application/json
                  .putHeader("content-type", "application/json")
                  .end(list.toString());
              } else {
                req.response()
                  .putHeader("content-type", "text/plain")
                  .end(ar2.cause().toString());
              }
            });
        } else {
          System.out.println("Could not connect: " + ar1.cause().getMessage());
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
