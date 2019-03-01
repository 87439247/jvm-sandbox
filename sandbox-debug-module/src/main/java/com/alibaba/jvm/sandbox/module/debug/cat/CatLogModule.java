package com.alibaba.jvm.sandbox.module.debug.cat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.jvm.sandbox.module.debug.util.IpUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;


import java.beans.Transient;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class CatLogModule extends CatModule {

    private static DefaultMQProducer mqProducer;

    /**
     * app 访问日志 topic
     */
    private static String rocketmqAppLogTopic;

    /**
     * tomcat 访问日志 topic
     */
    private static String rocketmqTomcatLogTopic;


    /**
     * 应用程序日志队列
     */
    private ArrayBlockingQueue<AppLogMessage> appLogQueue = new ArrayBlockingQueue<AppLogMessage>(50000);

    /**
     * tomcat 日志队列
     */
    private ArrayBlockingQueue<String> tomcatLogQueue = new ArrayBlockingQueue<String>(50000);

    static {
        String rocketmqNameServerAddr = CatModule.getConfigFromEnv("log_rocketmq_addr", "10.4.63.103:9876;10.4.63.104:9876");
        rocketmqAppLogTopic = CatModule.getConfigFromEnv("app_log_rocketmq_topic", "FILEBEAT-APP");
        rocketmqTomcatLogTopic = CatModule.getConfigFromEnv("tomcat_log_rocketmq_topic", "FILEBEAT-TOMCAT-ACCESS");
        mqProducer = new DefaultMQProducer();
        mqProducer.setNamesrvAddr(rocketmqNameServerAddr);
        mqProducer.setVipChannelEnabled(false);
        mqProducer.setProducerGroup("PG-SANDBOX");
        mqProducer.setSendMsgTimeout(10000);
        mqProducer.setRetryTimesWhenSendFailed(1);

        mqProducer.setClientIP(IpUtils.getIp());
        try {
            mqProducer.start();
            internalLogger.info("ROCKETMQ Producer start , IP = {}", IpUtils.getIp());
        } catch (MQClientException e) {
            internalLogger.error("rocketmq init failed", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                internalLogger.info("## mqProducer closing");
                mqProducer.shutdown();
                internalLogger.info("## mqProducer closed");
            }
        });
    }

    /**
     * {"@timestamp":"2018-12-12T01:51:23.145Z","ip":"10.0.150.159","message":"{\"@timestamp\":\"2018-12-12T09:50:59.635+08:00\",\"@version\":\"1\",\"message\":\"Registering transaction synchronization for JDBC Connection\",\"logger_name\":\"org.springframework.jdbc.datasource.DataSourceUtils\",\"thread_name\":\"catalina-exec-121\",\"level\":\"DEBUG\",\"level_value\":10000}","offset":79276966,"source":"/opt/tomcat/logs/px_all.log","tags":"px-apilesson","type":"log"}
     */
    public CatLogModule() {
        // app 日志消费进程
        new Thread(new Runnable() {
            @Override
            public void run() {
                internalLogger.info("应用程序日志消费进程启动...");
                while (true) {
                    try {
                        AppLogMessage log = appLogQueue.take();
                        LogWrapper wrapper = new LogWrapper();
                        wrapper.setTimestamp(log.getTimestamp());
                        if (log.getThrowable() != null) {
                            StringWriter writer = new StringWriter();
                            log.getThrowable().printStackTrace(new PrintWriter(writer));
                            log.setStackTrace(writer.toString());
                        }
                        String message = JSON.toJSONString(log);
                        wrapper.setMessage(message);
                        sendToMq(rocketmqAppLogTopic, wrapper);
                    } catch (Exception e) {
                        stLogger.error("send log to rocketmq error", e);
                    }
                }
            }
        }).start();

        // tomcat 日志消费
        new Thread(new Runnable() {
            @Override
            public void run() {
                internalLogger.info("tomcat日志消费进程启动...");
                while (true) {
                    try {
                        String tomcatLogMessage = tomcatLogQueue.take();
                        LogWrapper wrapper = new LogWrapper();
                        wrapper.setTimestamp(new Date());
                        wrapper.setMessage(tomcatLogMessage);
                        sendToMq(rocketmqTomcatLogTopic, wrapper);
                    } catch (Exception e) {
                        stLogger.error("send log to rocketmq error", e);
                    }
                }
            }
        }).start();
    }

    /**
     * 发送至rocket mq
     *
     * @param topic   topic
     * @param wrapper 数据信息
     * @return 发送是否成功
     */
    private boolean sendToMq(String topic, LogWrapper wrapper) {
        try {
            Message rocketmqMessage = new Message();
            rocketmqMessage.setTags(CatModule.CAT_DOMAIN);
            rocketmqMessage.setBody(JSON.toJSONBytes(wrapper));
            rocketmqMessage.setTopic(topic);
            SendResult sendResult = mqProducer.send(rocketmqMessage);
            if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
                stLogger.error("send log to rocketmq error {}", sendResult.getSendStatus().toString());
                return false;
            }
        } catch (InterruptedException | MQClientException | RemotingException | MQBrokerException e) {
            stLogger.error("send log to rocketmq error", e);
            return false;
        }
        return true;
    }

    /**
     * 消息入队列
     *
     * @param timestamp
     * @param message
     * @param levelValue
     * @param loggerName
     * @param threadName
     * @param throwable
     * @return
     */
    protected boolean offerAppLog(long timestamp, String message, int levelValue, String loggerName, String threadName, Throwable throwable) {
        AppLogMessage appLogMessage = new AppLogMessage();
        appLogMessage.setTimestamp(new Date(timestamp));
        appLogMessage.setMessage(message);
        appLogMessage.setLevelValue(levelValue);
        if (levelValue <= 10000) {
            appLogMessage.setLevel("DEBUG");
        } else if (levelValue <= 20000) {
            appLogMessage.setLevel("INFO");
        } else if (levelValue <= 30000) {
            appLogMessage.setLevel("WARN");
        } else {
            appLogMessage.setLevel("ERROR");
        }
        appLogMessage.setLoggerName(loggerName);
        appLogMessage.setThreadName(threadName);
        appLogMessage.setThrowable(throwable);
        return appLogQueue.offer(appLogMessage);
    }


    /**
     * 消息入队列
     *
     * @param message 消息体
     * @return 是否入队成功
     */
    protected boolean offerTomcatLog(String message) {
        return tomcatLogQueue.offer(message);
    }


    public class LogWrapper {
        @JSONField(name = "@timestamp", format = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
        private Date timestamp;
        private String message;
        private String ip = IpUtils.getIp();
        private String tags = CatModule.CAT_DOMAIN;

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }
    }

    /**
     * java 应用程序日志消息
     */
    public class AppLogMessage {
        @JSONField(name = "@timestamp", format = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
        private Date timestamp;

        private String message;

        @JSONField(name = "logger_name")
        private String loggerName;

        @JSONField(name = "thread_name")
        private String threadName;

        private String level;

        @JSONField(name = "level_value")
        private int levelValue;


        private Throwable throwable;

        @JSONField(name = "stack_trace")
        private String stackTrace;

        private String tags;

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getLoggerName() {
            return loggerName;
        }

        public void setLoggerName(String loggerName) {
            this.loggerName = loggerName;
        }

        public String getThreadName() {
            return threadName;
        }

        public void setThreadName(String threadName) {
            this.threadName = threadName;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public int getLevelValue() {
            return levelValue;
        }

        public void setLevelValue(int levelValue) {
            this.levelValue = levelValue;
        }

        @Transient
        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public void setStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

    }

}
