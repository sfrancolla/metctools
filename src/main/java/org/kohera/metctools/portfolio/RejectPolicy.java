package org.kohera.metctools.portfolio;

import java.io.Serializable;

import org.kohera.metctools.DelegatorStrategy;
import org.marketcetera.trade.ExecutionReport;
import org.marketcetera.trade.OrderID;

/**
 * 
 * Interface for custom policies for FIX message rejections.
 * 
 * @author Jake Brukhman
 *
 */
public interface RejectPolicy 
	extends Serializable {
	
	public void onReject( DelegatorStrategy sender, OrderID orderId, Trade trade, ExecutionReport report );
	
}
