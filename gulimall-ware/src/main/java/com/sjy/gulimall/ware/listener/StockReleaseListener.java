package com.sjy.gulimall.ware.listener;

import com.sjy.common.to.mq.OrderTo;
import com.sjy.common.to.mq.StockLockedTo;
import com.sjy.gulimall.ware.service.WareSkuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;

import java.io.IOException;

@Slf4j
@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {
 
    @Autowired
    WareSkuService wareSkuService;
 
    @RabbitHandler
//消息内容为库存锁定单传输对象，里面包括库存锁定单id和库存锁定详情单对象
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        log.info("************收到库存解锁的消息****************");
        System.out.println("收到锁库存成功的消息，准备解锁库存");
        try {
            wareSkuService.unlockStock(to);
//消费者确认消息接收成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

    @RabbitHandler
    public void handleStockLockedRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        log.info("************从订单模块收到库存解锁的消息********************");
        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
}