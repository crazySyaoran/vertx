package com.example.vertx;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;

public class ConfigVerticle extends AbstractVerticle {

  Router router;
  MySQLConnectOptions connectOptions;
  PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
  MySQLPool client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);
    // 首先初始化外部参数配置器
    ConfigRetriever retriever = ConfigRetriever.create(vertx);

    retriever.getConfig(ar -> {
      if (ar.failed()) {
        // Failed to retrieve the configuration
        System.out.println("Config ERROR");
      } else {
        JsonObject config = ar.result();

        connectOptions = new MySQLConnectOptions()
          .setPort(Integer.parseInt(config.getValue("port").toString()))
          .setHost(config.getString("host"))
          .setDatabase(config.getString("database"))
          .setUser(config.getString("user"))
          .setPassword(config.getString("password"));

        client = MySQLPool.pool(vertx,connectOptions, poolOptions);
        router.route("/sql").handler(req -> {
          // Get a connection from the pool
          client.getConnection(ar1 -> {
            if (ar1.succeeded()) {
              System.out.println("Connected");
              SqlConnection conn = ar1.result();

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
    });

  }
}

