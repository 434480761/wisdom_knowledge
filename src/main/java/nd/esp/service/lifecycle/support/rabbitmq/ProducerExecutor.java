package nd.esp.service.lifecycle.support.rabbitmq;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.lcmq.component.model.MqMessage;
import com.lcmq.component.mq.RabbitMqProducer;
/**
 * 消息队列 -- 生产者执行器
 * @author xiezy
 * @date 2016年8月1日
 */
//@Component
public class ProducerExecutor {
	private static final Logger logger = LoggerFactory.getLogger(ProducerExecutor.class);
	
	private ExecutorService excutorService = Executors.newCachedThreadPool();
	
	public void startProducer(final JSONObject object) {
		this.excutorService.submit(new Thread() {
			public void run() {
				try {
					RabbitMqProducer rabbitMqProducer = new RabbitMqProducer();
					
					logger.info("producer start");
					MqMessage message = new MqMessage(UUID.randomUUID().toString(), "服务名", object);
					//发送消息
					rabbitMqProducer.send(RabbitMqConstant.ROUTING_KEY, message);
				} catch (Exception e) {
					logger.error("producer execute exception", e);
				}
			}
		});
	}
}
