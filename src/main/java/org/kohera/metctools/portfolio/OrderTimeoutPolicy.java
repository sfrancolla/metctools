package org.kohera.metctools.portfolio;

import java.io.Serializable;

import org.kohera.metctools.DelegatorStrategy;
import org.marketcetera.trade.OrderID;

/**
 * Interface for specifying order timeout policies.
 * 
 * @author Jake Brukhman
 *
 */
public interface OrderTimeoutPolicy extends Serializable {
	
	/**
	 * Override this method to implement an order timeout policy.
	 * 
	 * @param sender
	 * @param orderId
	 * @param timeout
	 * @param trade
	 */
	public void onOrderTimeout( 
			DelegatorStrategy sender, 
			OrderID orderId, 
			long timeout, 
			Trade trade 
			);

}
