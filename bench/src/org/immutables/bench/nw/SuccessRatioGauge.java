package org.immutables.bench.nw;

import java.util.concurrent.atomic.AtomicLong;
import com.codahale.metrics.RatioGauge;

public class SuccessRatioGauge extends RatioGauge {
	final AtomicLong totalCount = new AtomicLong();
	final AtomicLong successCount = new AtomicLong();

	void mark(boolean success) {
		totalCount.incrementAndGet();
		if (success) {
			successCount.incrementAndGet();
		}
	}

	@Override
	protected Ratio getRatio() {
		return Ratio.of(
				successCount.doubleValue(),
				totalCount.doubleValue());
	}
}
