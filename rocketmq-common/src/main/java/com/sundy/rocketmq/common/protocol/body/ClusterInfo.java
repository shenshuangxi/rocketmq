package com.sundy.rocketmq.common.protocol.body;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import com.sundy.rocketmq.common.protocol.route.BrokerData;
import com.sundy.rocketmq.remoting.protocol.RemotingSerializable;

@Getter
@Setter
public class ClusterInfo extends RemotingSerializable {

	private HashMap<String, BrokerData> brokerAddrTable;
	private HashMap<String, Set<String>> clusterAddrTable;
	
	
	public String[] retrieveAllAddrByCluster(String cluster) {
        List<String> addrs = new ArrayList<String>();
        if (clusterAddrTable.containsKey(cluster)) {
            Set<String> brokerNames = clusterAddrTable.get(cluster);
            for (String brokerName : brokerNames) {
                BrokerData brokerData = brokerAddrTable.get(brokerName);
                if (null != brokerData) {
                    addrs.addAll(brokerData.getBrokerAddrs().values());
                }
            }
        }
        return addrs.toArray(new String[] {});
    }

    public String[] retrieveAllClusterNames() {
        return clusterAddrTable.keySet().toArray(new String[] {});
    }
	
}
