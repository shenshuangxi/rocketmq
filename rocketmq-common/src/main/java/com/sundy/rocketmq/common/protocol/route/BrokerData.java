package com.sundy.rocketmq.common.protocol.route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.sundy.rocketmq.common.namesrv.MixAll;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrokerData implements Comparable<BrokerData> {

	private String cluster;
	private String brokerName;
	private HashMap<Long, String> brokerAddrs;
	
	private final Random random = new Random();
	
	public BrokerData() {
		
	}
	
	public BrokerData(String cluster, String brokerName, HashMap<Long, String> brokerAddrs) {
		this.cluster = cluster;
		this.brokerName = brokerName;
		this.brokerAddrs = brokerAddrs;
	}


	public String selectBrokerAddr() {
		String addr = this.brokerAddrs.get(MixAll.MASTER_ID);
		if (addr == null) {
			List<String> addrs = new ArrayList<String>(this.brokerAddrs.values());
			return addrs.get(random.nextInt(addrs.size()));
		}
		return addr;
	}
	

	@Override
	public int compareTo(BrokerData o) {
		return this.brokerName.compareTo(o.getBrokerName());
	}
	
	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((brokerAddrs == null) ? 0 : brokerAddrs.hashCode());
        result = prime * result + ((brokerName == null) ? 0 : brokerName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BrokerData other = (BrokerData) obj;
        if (brokerAddrs == null) {
            if (other.brokerAddrs != null)
                return false;
        } else if (!brokerAddrs.equals(other.brokerAddrs))
            return false;
        if (brokerName == null) {
            if (other.brokerName != null)
                return false;
        } else if (!brokerName.equals(other.brokerName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BrokerData [brokerName=" + brokerName + ", brokerAddrs=" + brokerAddrs + "]";
    }

}
