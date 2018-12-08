package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

import static com.alibaba.jvm.sandbox.module.debug.util.CatFinishUtil.finish;

@MetaInfServices(Module.class)
@Information(id = "cat-jedis", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatJedisModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorJedisMethod();
    }
    /**
     *
     */
    private void monitorJedisMethod() {

        new EventWatchBuilder(moduleEventWatcher)
                .onClass("redis.clients.jedis.JedisCommands").includeSubClasses()
                .onBehavior("zcount").onBehavior("sunionstore").onBehavior("zunionstore")
                .onBehavior("del").onBehavior("zinterstore").onBehavior("echo")
                .onBehavior("hscan").onBehavior("psubscribe").onBehavior("type")
                .onBehavior("sinterstore").onBehavior("setex").onBehavior("zlexcount")
                .onBehavior("brpoplpush").onBehavior("bitcount").onBehavior("llen")
                .onBehavior("zscan").onBehavior("lpushx").onBehavior("bitpos")
                .onBehavior("setnx").onBehavior("hvals").onBehavior("evalsha")
                .onBehavior("substr").onBehavior("geodist").onBehavior("zrangeByLex")
                .onBehavior("geoadd").onBehavior("expire").onBehavior("bitop")
                .onBehavior("zrangeByScore").onBehavior("smove").onBehavior("lset")
                .onBehavior("decrBy").onBehavior("pttl").onBehavior("scan")
                .onBehavior("zrank").onBehavior("blpop").onBehavior("rpoplpush")
                .onBehavior("zremrangeByLex").onBehavior("get").onBehavior("lpop")
                .onBehavior("persist").onBehavior("scriptExists").onBehavior("georadius")
                .onBehavior("set").onBehavior("srandmember").onBehavior("incr").onBehavior("setbit")
                .onBehavior("hexists").onBehavior("expireAt").onBehavior("pexpire").onBehavior("zcard")
                .onBehavior("bitfield").onBehavior("zrevrangeByLex").onBehavior("sinter").onBehavior("srem")
                .onBehavior("getrange").onBehavior("rename").onBehavior("zrevrank").onBehavior("exists")
                .onBehavior("setrange").onBehavior("zremrangeByRank").onBehavior("sadd").onBehavior("sdiff")
                .onBehavior("zrevrange").onBehavior("getbit").onBehavior("scard").onBehavior("sdiffstore")
                .onBehavior("zrevrangeByScore").onBehavior("zincrby").onBehavior("rpushx").onBehavior("psetex")
                .onBehavior("zrevrangeWithScores").onBehavior("strlen").onBehavior("hdel").onBehavior("zremrangeByScore")
                .onBehavior("geohash").onBehavior("brpop").onBehavior("lrem").onBehavior("hlen").onBehavior("decr")
                .onBehavior("scriptLoad").onBehavior("lpush").onBehavior("lindex").onBehavior("zrange").onBehavior("incrBy")
                .onBehavior("getSet").onBehavior("ltrim").onBehavior("incrByFloat").onBehavior("rpop").onBehavior("sort")
                .onBehavior("zrevrangeByScoreWithScores").onBehavior("pfadd").onBehavior("eval").onBehavior("linsert")
                .onBehavior("pfcount").onBehavior("hkeys").onBehavior("hsetnx").onBehavior("hincrBy").onBehavior("hgetAll")
                .onBehavior("hset").onBehavior("spop").onBehavior("zrangeWithScores").onBehavior("hincrByFloat")
                .onBehavior("hmset").onBehavior("renamenx").onBehavior("zrem").onBehavior("msetnx").onBehavior("hmget")
                .onBehavior("sunion").onBehavior("hget").onBehavior("zadd").onBehavior("move").onBehavior("subscribe")
                .onBehavior("geopos").onBehavior("mset").onBehavior("zrangeByScoreWithScores").onBehavior("zscore")
                .onBehavior("pexpireAt").onBehavior("georadiusByMember").onBehavior("ttl").onBehavior("lrange")
                .onBehavior("smembers").onBehavior("pfmerge").onBehavior("rpush").onBehavior("publish")
                .onBehavior("mget").onBehavior("sscan").onBehavior("append").onBehavior("sismember")
                .withParameterTypes(String.class)
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        String methodName = advice.getBehavior().getName();
                        String key = "";
                        if (advice.getParameterArray() != null && advice.getParameterArray().length >= 1) {
                            key = (String) advice.getParameterArray()[0];
                        }
                        Transaction t = Cat.newTransaction(getCatType(), methodName + "(" + key + ")");
                        advice.attach(t);
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        finish(advice);
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        finish(advice);
                    }
                });
    }


    @Override
    String getCatType() {
        return "Cache";
    }
}
