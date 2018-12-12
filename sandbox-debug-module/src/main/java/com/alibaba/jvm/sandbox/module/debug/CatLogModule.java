package com.alibaba.jvm.sandbox.module.debug;

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

    static DefaultMQProducer mqProducer;

    static String rocketmqTopic;

    static {
        String rocketmqNameServerAddr = CatModule.getConfigFromEnv("log_rocketmq_addr", "10.4.63.103:9876;10.4.63.104:9876");
        rocketmqTopic = CatModule.getConfigFromEnv("log_rocketmq_topic", "FILEBEAT-APP");
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        LogMessage log = logQueue.take();
                        LogWrapper wrapper = new LogWrapper();
                        wrapper.setTimestamp(log.getTimestamp());

                        if (log.getThrowable() != null) {
                            StringWriter writer = new StringWriter();
                            log.getThrowable().printStackTrace(new PrintWriter(writer));
                            log.setStackTrace(writer.toString());
                        }
                        String message = JSON.toJSONString(log);
                        wrapper.setMessage(message);
                        Message rocketmqMessage = new Message();
                        rocketmqMessage.setTags(CatModule.CAT_DOMAIN);
                        rocketmqMessage.setTopic(rocketmqTopic);
                        rocketmqMessage.setBody(JSON.toJSONBytes(wrapper));
                        SendResult sendResult = mqProducer.send(rocketmqMessage);
                        if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
                            stLogger.error("send log to rocketmq error {}", sendResult.getSendStatus().toString());
                        }
                    } catch (InterruptedException | MQClientException | RemotingException | MQBrokerException e) {
                        stLogger.error("send log to rocketmq error", e);
                    }
                }
            }
        }).start();
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
    public boolean offer(long timestamp, String message, int levelValue, String loggerName, String threadName, Throwable throwable) {
        LogMessage logMessage = new LogMessage();
        logMessage.setTimestamp(new Date(timestamp));
        logMessage.setMessage(message);
        logMessage.setLevelValue(levelValue);
        logMessage.setLoggerName(loggerName);
        logMessage.setThreadName(threadName);
        logMessage.setThrowable(throwable);
        return logQueue.offer(logMessage);
    }

    /**
     * queue
     */
    protected ArrayBlockingQueue<LogMessage> logQueue = new ArrayBlockingQueue<LogMessage>(50000);

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

    public class LogMessage {
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
