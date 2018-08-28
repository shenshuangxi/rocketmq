package com.sundy.rocketmq.namesrv.processor;

import io.netty.channel.ChannelHandlerContext;

import com.sundy.rocketmq.common.constants.LoggerName;
import com.sundy.rocketmq.common.help.FAQUrl;
import com.sundy.rocketmq.common.protocol.ResponseCode;
import com.sundy.rocketmq.common.protocol.header.GetRouteInfoRequestHeader;
import com.sundy.rocketmq.common.protocol.route.TopicRouteData;
import com.sundy.rocketmq.logging.InternalLogger;
import com.sundy.rocketmq.logging.InternalLoggerFactory;
import com.sundy.rocketmq.namesrv.NamesrvController;
import com.sundy.rocketmq.namesrv.NamesrvUtil;
import com.sundy.rocketmq.remoting.exception.RemotingCommandException;
import com.sundy.rocketmq.remoting.netty.NettyRequestProcessor;
import com.sundy.rocketmq.remoting.protocol.RemotingCommand;

public class ClusterTestRequestProcessor extends DefaultRequestProcessor {

	private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.NAMESRV_LOGGER_NAME);
    private final DefaultMQAdminExt adminExt;
    private final String productEnvName;

    public ClusterTestRequestProcessor(NamesrvController namesrvController, String productEnvName) {
        super(namesrvController);
        this.productEnvName = productEnvName;
        adminExt = new DefaultMQAdminExt();
        adminExt.setInstanceName("CLUSTER_TEST_NS_INS_" + productEnvName);
        adminExt.setUnitName(productEnvName);
        try {
            adminExt.start();
        } catch (MQClientException e) {
            log.error("Failed to start processor", e);
        }
    }

    @Override
    public RemotingCommand getRouteInfoByTopic(ChannelHandlerContext ctx,
        RemotingCommand request) throws RemotingCommandException {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final GetRouteInfoRequestHeader requestHeader =
            (GetRouteInfoRequestHeader) request.decodeCommandCustomHeader(GetRouteInfoRequestHeader.class);

        TopicRouteData topicRouteData = this.namesrvController.getRouteInfoManager().pickupTopicRouteData(requestHeader.getTopic());
        if (topicRouteData != null) {
            String orderTopicConf =
                this.namesrvController.getKvConfigManager().getKVConfig(NamesrvUtil.NAMESPACE_ORDER_TOPIC_CONFIG,
                    requestHeader.getTopic());
            topicRouteData.setOrderTopicConf(orderTopicConf);
        } else {
            try {
                topicRouteData = adminExt.examineTopicRouteInfo(requestHeader.getTopic());
            } catch (Exception e) {
                log.info("get route info by topic from product environment failed. envName={},", productEnvName);
            }
        }

        if (topicRouteData != null) {
            byte[] content = topicRouteData.encode();
            response.setBody(content);
            response.setCode(ResponseCode.SUCCESS);
            response.setRemark(null);
            return response;
        }

        response.setCode(ResponseCode.TOPIC_NOT_EXIST);
        response.setRemark("No topic route info in name server for the topic: " + requestHeader.getTopic() + FAQUrl.suggestTodo(FAQUrl.APPLY_TOPIC_URL));
        return response;
    }

}
