package org.kohera.metctools.util;

import java.math.*;
import org.marketcetera.trade.*;

/**
 * Utility class to help build orders.
 * 
 * TODO: Should have more functionality.
 * 
 * @author Jake Brukhman
 *
 */
public class OrderBuilder {
	
	/* fields */
	private OrderSingle 	order;
	private BrokerID 		defaultBrokerId;
	private String 			defaultAccount;

	private TimeInForce 	defaultTimeInForce;
	
	/**
	 * Create a new instance of OrderBuilder.
	 * 
	 */
	public OrderBuilder() {
		newOrder();
	}
	
	/**
	 * Create a new instance of OrderBuilder with default parameters.
	 * 
	 * @param brokerId
	 * @param account
	 * @param tif
	 */
	public OrderBuilder(BrokerID brokerId, String account, TimeInForce tif ) {
		this.defaultBrokerId = brokerId;
		this.defaultAccount = account;
		this.defaultTimeInForce = tif;
		newOrder();
	}
	
	/**
	 * Create a new instance of OrderBuilder with default parameters.
	 * 
	 * @param brokerId
	 * @param account
	 */	
	public OrderBuilder(BrokerID brokerId, String account) {
		this(brokerId,account,TimeInForce.Day);
	}
	
	/**
	 * Create a new instance of OrderBuilder with default parameters.
	 * 
	 * @param brokerId
	 */	
	public OrderBuilder(BrokerID brokerId) {
		this(brokerId,null,TimeInForce.Day);
	}
	
	/**
	 * Get the broker id.
	 * 
	 * @return
	 */
	public BrokerID getDefaultBrokerId() {
		return defaultBrokerId;
	}

	/**
	 * Set the broker id.
	 * 
	 * @param defaultBrokerId
	 */
	public void setDefaultBrokerId(BrokerID defaultBrokerId) {
		this.defaultBrokerId = defaultBrokerId;
	}

	/**
	 * Get the account.
	 * 
	 * @return
	 */
	public String getDefaultAccount() {
		return defaultAccount;
	}

	/**
	 * Set the account.
	 * 
	 * @param defaultAccount
	 */
	public void setDefaultAccount(String defaultAccount) {
		this.defaultAccount = defaultAccount;
	}

	/**
	 * Get the TIF.
	 * 
	 * @return
	 */
	public TimeInForce getDefaultTimeInForce() {
		return defaultTimeInForce;
	}

	/**
	 * Set the TIF.
	 * 
	 * @param defaultTimeInForce
	 */
	public void setDefaultTimeInForce(TimeInForce defaultTimeInForce) {
		this.defaultTimeInForce = defaultTimeInForce;
	}
	
	/**
	 * Generate the base order with defaults.
	 */
	public OrderBuilder newOrder() {
		order = Factory.getInstance().createOrderSingle();
		
		if ( defaultAccount != null ) {
			withAccount(defaultAccount);
		}
		if ( defaultBrokerId != null ) {
			withBrokerID(defaultBrokerId);
		}
		if ( defaultTimeInForce != null ) {
			withTimeInForce(defaultTimeInForce);
		}	
		return this;
	}
	
	/**
	 * Returns the order that has been built.
	 * 
	 * @return
	 */
	public OrderSingle getOrder() {
		return order;
	}
	
	/**
	 * Sets the Account value for the order.
	 * 
	 * @param account
	 * @return
	 */
	public OrderBuilder withAccount( String account ) {
		order.setAccount(account);
		return this;
	}

	/**
	 * Sets the brokerId value for the order.
	 * 
	 * @param brokerId
	 * @return
	 */
	public OrderBuilder withBrokerID( BrokerID brokerId ) {
		order.setBrokerID(brokerId);
		return this;
	}
	
    /**
     * Sets the orderType value for the order.
     * 
     * @param type
     * @return
     */
    public OrderBuilder withOrderType( OrderType type ) {
        order.setOrderType(type);
        return this;
    }
    
    /**
     * Sets the orderCapacity value for the order.
     * 
     * @param orderCapacity
     * @return
     */
    public OrderBuilder withOrderCapacity( OrderCapacity orderCapacity ) {
        order.setOrderCapacity(orderCapacity);
        return this;
    }
    
    /**
     * Sets the price value for the order.
     * 
     * @param price
     * @return
     */
    public OrderBuilder withPrice( BigDecimal price ) {
        order.setPrice(price);
        return this;
    }
    
    /**
     * Sets the positionEffect value for the order.
     * 
     * @param positionEffect
     * @return
     */
    public OrderBuilder withPositionEffect( PositionEffect positionEffect ) {
        order.setPositionEffect(positionEffect);
        return this;
    }
    
	/**
	 * Sets the orderQuantity value for the order.
	 * 
	 * @param qty
	 * @return
	 */
	public OrderBuilder withQuantity( BigDecimal qty ) {
		order.setQuantity(qty);
		return this;
	}
	
	/**
	 * Sets the orderQuantity value for the order.
	 * 
	 * @param qty
	 * @return
	 */	
	public OrderBuilder withQuantity( int qty ) {
		return this.withQuantity(BigDecimal.valueOf(qty, 0));
	}

	/**
	 * Sets the side value for the order.
	 * 
	 * @param qty
	 * @return
	 */
	public OrderBuilder withSide( Side side ) {
		order.setSide(side);
		return this;
	}

	/**
	 * Sets symbol for the order.
	 * 
	 * @param symbol
	 * @return
	 */
	public OrderBuilder withSymbol( String symbol ) {
		order.setSymbol(new MSymbol(symbol));
		return this;
	}
	
	/**
	 * Sets timeInForce value for the order.
	 * 
	 * @param tif
	 * @return
	 */
	public OrderBuilder withTimeInForce( TimeInForce tif ) {
		order.setTimeInForce(tif);
		return this;
	}
	
	/**
	 * Creates a market order.
	 * 
	 * @param brokerId
	 * @param account
	 * @param symbol
	 * @param qty
	 * @param side
	 * @return
	 */
	public OrderBuilder makeMarket(BrokerID brokerId, String account, String symbol, BigDecimal qty, Side side ) {
		return makeMarket(symbol,qty,side)
			.withBrokerID(brokerId)
			.withAccount(account);
	}
	
	/**
	 * Creates a market order.
	 * 
	 * @param symbol
	 * @param qty
	 * @param side
	 * @return
	 */
	public OrderBuilder makeMarket(String symbol, BigDecimal qty, Side side) {
		newOrder();
		return this
			.withSymbol(symbol)
			.withQuantity(qty)
			.withSide(side)
			.withOrderType(OrderType.Market);
	}

}
