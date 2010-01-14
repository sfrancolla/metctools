package org.kohera.metctools.portfolio;

import java.math.BigDecimal;

import org.kohera.metctools.util.OrderBuilder;
import org.kohera.metctools.util.Timer;
import org.kohera.metctools.util.Timer.Task;
import org.kohera.metctools.util.Timer.TaskThread;
import org.kohera.metctools.DelegatorStrategy;
import org.marketcetera.event.TradeEvent;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.ExecutionReport;
import org.marketcetera.trade.MSymbol;
import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.OrderSingle;
import org.marketcetera.trade.OrderStatus;

final class Trade {

	/* internal fields  */
	transient private final 
		Portfolio 			parentPortfolio;		// parent strategy
	private  
		OrderProcessor 		orderProcessor;		// order processing object

	/* accounting */
	private final MSymbol 	symbol;				// underlying symbol
	private BigDecimal 		quantity;			// unsigned position
	private Side 			side;				// side of the position
	
	private BigDecimal 		leavesQty;			// leavesQuantity of the pending order
	private BigDecimal 		cumulativeQty;		// number of shares pending fill
	private OrderStatus		orderStatus;		// order status of last (pertinent) execution report
	private Side 			pendingSide;		// side of the incoming fills
	private OrderID 		pendingOrderId;		// orderId of the order being filled
	private OrderID			cancelOrderId;
	private BigDecimal 		averagePrice;		// average entry price of last fill
	private TradeEvent 		lastTrade;			// last trade of the underylying symbol
	
	/* policies */
	private long 			orderTimeout;		// default timeout in milliseconds
	public FillPolicy 		fillPolicy;			// default fill policy (what to do on a fill?)
	public OrderTimeoutPolicy orderTimeoutPolicy; // default order timeout policy (what to do if order times out?)
	
	
	/**
	 * Create a new Trade object instance.
	 * 
	 * @param parent
	 * @param symbol
	 * @param brokerId
	 * @param account
	 */
	public Trade( MSymbol symbol, Portfolio parent ) {
		this.symbol = symbol;
		this.parentPortfolio = parent;
		init();
	}
	
	private void init() {
		quantity = leavesQty = cumulativeQty = BigDecimal.ZERO;
		averagePrice = BigDecimal.ZERO;
		pendingOrderId = null;
		lastTrade = null;
		side = pendingSide = Side.NONE;
		
		fillPolicy = FillPolicies.ON_FILL_WARN;
		orderTimeoutPolicy = OrderTimeoutPolicies.ON_TIMEOUT_WARN;
		orderTimeout = 60*1000;
		
		orderProcessor = new OrderProcessor();	
	}
	
	/**
	 * Returns the cost basis (average price of position).
	 * 
	 * @return
	 */
	public BigDecimal getCostBasis() {
		return averagePrice;
	}

	/**
	 * Returns the number of shares still pending fill.
	 * 
	 * @return
	 */
	public BigDecimal getLeavesQuantity() {
		return leavesQty;
	}

	/**
	 * With respect to a pending order, returns the cumulative quantity
	 * of shares that have so far been filled.
	 * 
	 * If there is no pending order, this method returns 0.
	 * 
	 * @return
	 */
	public BigDecimal getCumulativeQuantity() {
		return cumulativeQty;
	}

	/**
	 * Returns the unsigned number of shares of this position.
	 * 
	 * This does not take into consideration a currently filling order,
	 * even if some of the order has been filled.
	 * 
	 * @return
	 */
	public BigDecimal getQuantity() {
		return quantity;
	}

	/**
	 * Returns the underlying symbol of this Trade.
	 * 
	 * @return
	 */
	public MSymbol getSymbol() {
		return symbol;
	}

	/**
	 * Returns true if the Trade is currently has a pending order
	 * that is filling.
	 * 
	 * @return
	 */
	public boolean isFilling() {
		return (pendingOrderId!=null && leavesQty.compareTo(BigDecimal.ZERO)!=0);
	}

	/**
	 * Returns true if the Trade currently has a pending order.
	 * 
	 * @return
	 */
	public boolean isPending() {
		return (pendingOrderId!=null);
	}
	
	/**
	 * Returns the orderId of the pending order (or null).
	 * 
	 * @return
	 */
	public OrderID getPendingOrderID() {
		return pendingOrderId;
	}

	/**
	 * Returns the side of the trade (or Side.NONE).
	 * 
	 * @return
	 */
	public Side getSide() {
		return side;
	}

	/**
	 * Returns the signed position of the Trade.
	 * 
	 * @return
	 */
	public BigDecimal getSignedQuantity() {
		return quantity.multiply(side.toBigDecimal());
	}

	/**
	 * Returns the side of the pending order.
	 * 
	 * @return
	 */
	public Side getPendingSide() {
		return pendingSide;
	}

	/**
	 * Returns the last trading price of the underlying symbol.
	 * 
	 * In order for this method to return an accurate value,
	 * data flows for this symbol must have been turned on,
	 * and this Trade must reside in a PortfolioStrategy's
	 * Portfolio.
	 * 
	 * @return
	 */
	public BigDecimal getLastPrice() {
		if (lastTrade==null) return BigDecimal.ZERO;
		return lastTrade.getPrice();
	}

	/**
	 * Returns the last TradeEvent.
	 * 
	 * In order for this method to return an accurate value,
	 * data flows for this symbol must have been turned on,
	 * and this Trade must reside in a PortfolioStrategy's
	 * Portfolio.
	 * 
	 * @return
	 */
	public TradeEvent getLastTrade() {
		return lastTrade;
	}
	
	/**
	 * Returns the instantaneous (usually unsigned) position of this trade.
	 * 
	 * If there is no pending order currently being filled, this method is
	 * equivalent to getQuantity().
	 * 
	 * If there is a pending order currently being filled, this method returns
	 * the instantaneous position of the trade, taking into account the fills
	 * up to this point in time.
	 * 
	 * If the fills up to this point in time have caused the position to change
	 * sides, this is denoted with a negative sign in the result.
	 * 
	 * @return
	 */
	public BigDecimal getNetQuantity() {
		BigDecimal polarity = BigDecimal.valueOf(side.value()*pendingSide.value(),0);
		return quantity.add( polarity.multiply(cumulativeQty));
	}
	
	/**
	 * Returns a reference to the parent Portfolio for this Trade.
	 * 
	 * @return
	 */
	public Portfolio getParentPortfolio() {
		return parentPortfolio;
	}
	
	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	/**
	 * Interface for getting the latest TradeEvent.
	 * 
	 * @param tradeEvent
	 */
	public void acceptTrade(TradeEvent tradeEvent) {
		lastTrade = tradeEvent;
	}

	/**
	 * Returns the OrderProcessor object for this Trade.
	 * 
	 * @return
	 */
	public OrderProcessor order() {
		return orderProcessor;
	}

	public void scrapeReport(ExecutionReport report) {
		orderStatus = report.getOrderStatus();
		cumulativeQty = report.getCumulativeQuantity();
		leavesQty = report.getLeavesQuantity();
		pendingSide = Side.fromMetcSide(report.getSide());
	}
	
	/**
	 * Adjusts the trade information based on an incoming execution report.
	 * 
	 * @param sender
	 * @param report
	 */
	
	public final void acceptExecutionReport(DelegatorStrategy sender,
			ExecutionReport report) {
		/* check the correct symbol */
		if ( symbol!=report.getSymbol()) {
			sender.getFramework().warn(
					symbol + ": received external execution report (ignoring).");
			return;
		}
		
		/* check the correct order id */
		if ( pendingOrderId!=report.getOrderID()) {
			sender.getFramework().warn(
					symbol + ": received external execution report (accepting).");
		}
		
		orderStatus = report.getOrderStatus();
		
		if ( orderStatus==OrderStatus.New ) {
			sender.getFramework().info(this + ": Execution report status is "+orderStatus+".");
		}
		else if ( orderStatus == OrderStatus.PartiallyFilled ) {
			processPartialFill(report);
		}
		else if ( orderStatus == OrderStatus.Filled ) {
			processFill(report);
		}
		else if ( orderStatus == OrderStatus.Canceled ) {
			
		}
		else {
			// TODO: implement this
			sender.getFramework().error("Execution report status is "+orderStatus+", which is not implemented.");
		}
		
	}
	
	private void processPartialFill( ExecutionReport report ) {
		
	}
	
	private void processFill( ExecutionReport report ) {
		
	}
	
	/**
	 * Formats this Trade object for text output.
	 */
	public String toString() {
		return String.format("{%s:[%.2f]:%s%d@%.4f}",
				getSymbol(),
				getLastPrice().floatValue(),
				(side==Side.BUY?"+":(side==Side.SELL?"-":"")),
				quantity.intValue(),
				averagePrice.floatValue());
	}

	/**
	 * Sets the FillPolicy for this Trade.
	 * 
	 * When an order is successfully filled, the FillPolicy.onFill()
	 * method is executed.
	 * 
	 * @param policy
	 */
	public void setFillPolicy( FillPolicy policy ) {
		fillPolicy = policy;
	}
	
	/**
	 * Sets the OrderTimeoutPolicy for this Trade.
	 * 
	 * When an order has not been completely filled by the time
	 * an order timeout occurs, then this policy is executed.
	 * 
	 * @param policy
	 */
	public void setOrderTimeoutPolicy( OrderTimeoutPolicy policy ) {
		orderTimeoutPolicy = policy;
	}
	
	/**
	 * Sets the order timeout time (in ms.) for this Trade.
	 * 
	 * @param timeout
	 */
	public void setOrderTimeout( long timeout ) {
		orderTimeout = timeout;
	}
	
	
	/**
	 * 
	 * Order Processor.
	 * 
	 */
	private final class OrderProcessor {
		
		private OrderBuilder orderBuilder;
		private final Timer timer;
		private TaskThread orderTimeoutThr;

		
		public OrderProcessor() {
			final BrokerID brokerId = getParentPortfolio().getBrokerID();
			final String account = getParentPortfolio().getAccount();
			orderBuilder = new OrderBuilder(brokerId,account);
			timer = new Timer();
		}
		
		private void sendOrder( OrderSingle order, final long timeout, final OrderTimeoutPolicy policy ) {
			
			// TODO: may not be wise to have so many checks in this method
			
			/* check the parent portfolio */
			if (parentPortfolio==null) {
				throw new RuntimeException("Trade "+this+" doesn't have a parent portfolio.");
			}
			
			/* get the parent strategy */
			final PortfolioStrategy parentStrategy = 
				parentPortfolio.getParentStrategy();
			if ( parentStrategy==null) {
				throw new RuntimeException("Trade "+this+" doesn't have a parent strategy.");
			}
			
			/* check pending order */
			if ( isPending() ) {
				parentStrategy.getFramework().error(
						this + ": Cannot send an order while order " + pendingOrderId + " is pending.");
				return;
			}
			
			
			/* send the order */
			final OrderID id = order.getOrderID();
			pendingOrderId = id;
			parentStrategy.getFramework().send(order);
			parentStrategy.getFramework().info("Sending order " + id + ".");
			
			/* create timeout */
			orderTimeoutThr = timer.fireIn(timeout, new Task() {
				public void performTask() {
					if ( orderTimeoutPolicy!=null ) {
						orderTimeoutPolicy
						  .onOrderTimeout(parentStrategy, id, timeout, Trade.this);
					}
				}			
			});
		}
		
		public void killTimeoutThread() {
			if ( orderTimeoutThr!=null ) {
				timer.kill(orderTimeoutThr);
			}
		}
		
		public void marketOrder( BigDecimal qty, Side side, long timeout, OrderTimeoutPolicy policy ) {
			OrderSingle order = orderBuilder
									.makeMarket(symbol, qty, side.toMetcSide())
									.getOrder();
			sendOrder(order,timeout,policy);
		}
		
		public void marketOrder( BigDecimal qty, Side side, long timeout ) {
			marketOrder(qty,side,timeout,orderTimeoutPolicy);
		}

		public void marketOrder( BigDecimal qty, Side side ) {
			marketOrder(qty,side,orderTimeout,orderTimeoutPolicy);
		}
		
		public void longTradeMarket(BigDecimal qty, long timeout, OrderTimeoutPolicy policy) {
			marketOrder(qty,Side.BUY,timeout,policy);
		}

		public void longTradeMarket(BigDecimal qty, long timeout) {
			longTradeMarket(qty, timeout, orderTimeoutPolicy);
		}
	
		public void longTradeMarket(BigDecimal qty) {
			longTradeMarket(qty, orderTimeout, orderTimeoutPolicy);
		}
	
		public void shortTradeMarket(BigDecimal qty, long timeout, OrderTimeoutPolicy policy) {
			marketOrder(qty,Side.SELL,timeout,policy);
		}
			
		public void shortTradeMarket(BigDecimal qty, long timeout) {
			shortTradeMarket(qty, timeout, orderTimeoutPolicy);
		}
	
		public void shortTradeMarket(BigDecimal qty) {
			shortTradeMarket(qty, orderTimeout, orderTimeoutPolicy);
		}
		
		public void closeTradeMarket(long timeout, OrderTimeoutPolicy policy) {
			reduceTradeMarket(quantity,timeout,policy);
		}
		
		public void closeTradeMarket(long timeout) {
			closeTradeMarket(timeout,orderTimeoutPolicy);
		}
		
		public void closeTradeMarket() {
			closeTradeMarket(orderTimeout,orderTimeoutPolicy);
		}
		
		public void reduceTradeMarket(BigDecimal qty, long timeout, OrderTimeoutPolicy policy) {
			marketOrder(qty,side.opposite(),timeout,policy);
		}

		public void reduceTradeMarket(BigDecimal qty, long timeout) {
			reduceTradeMarket(qty,timeout,orderTimeoutPolicy);
		}
		
		public void reduceTradeMarket(BigDecimal qty) {
			reduceTradeMarket(qty,orderTimeout,orderTimeoutPolicy);
		}
	}
}
