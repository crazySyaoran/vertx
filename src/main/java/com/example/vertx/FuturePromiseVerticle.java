package com.example.vertx;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;

import java.util.ArrayList;

// 通过Future+Promise的方法避免回调地狱
// 注意只有包装了Async的handler才能使用Future+Promise的方法
public class FuturePromiseVerticle extends AbstractVerticle {

  Router router;
  MySQLConnectOptions connectOptions;
  PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
  MySQLPool client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    router = Router.router(vertx);
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
        router.route("/sql/list").handler(req -> {

          Integer offset = (Integer.parseInt(req.request().getParam("page"))-1)*5;

          // ************************ 链式调用 ***************************
          this.getCon().compose(con->this.getRows(con, offset))
            .onSuccess(rows -> {
              ArrayList<JsonObject> list = new ArrayList<JsonObject>();
              rows.forEach(item ->{
                JsonObject json = new JsonObject();
                json.put("user_id", item.getValue("user_id"));
                json.put("user_name", item.getValue("user_name"));
                list.add(json);
              });
              req.response()
                // 这里要注意是"content-type"不是"context-type"，并且后面要配置成application/json
                .putHeader("content-type", "application/json")
                .end(list.toString());
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

  // 获取数据库连接部分
  private Future<SqlConnection> getCon() {
    Promise<SqlConnection> promise = Promise.promise();
    // 上面两行是固定用法，要保证的是<XX>括号里的泛型与要替换的handler类型的Handler<Async<XX>>的XX保持一致
    client.getConnection(ar1 -> {
      if (ar1.succeeded()) {
        System.out.println("Connected");
        SqlConnection conn = ar1.result();
        // 固定写法，成功则用promise包装结果
        promise.complete(conn);
      } else {
        // 固定写法，失败则调用promise.fail()
        promise.fail(ar1.cause());
      }
    });
    // 固定写法，用promise生成future并返回
    return promise.future();
  }

  // 用获取的连接查询数据部分
  private Future<RowSet<Row>> getRows(SqlConnection conn, Integer offset){
    Promise<RowSet<Row>> promise = Promise.promise();
    conn
      .preparedQuery("SELECT user_id, user_name FROM users limit 5 offset ?")
      .execute(Tuple.of(offset), ar2 -> {
        conn.close();
        if (ar2.succeeded()) {
          promise.complete(ar2.result());
        } else {
          promise.fail(ar2.cause());
        }
      });
    return promise.future();
  }

}
