package org.immutables.bench.nw;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public final class Runner {
	public final MetricRegistry registry = new MetricRegistry();
	private final SuccessRatioGauge successRatio;
	private final Timer latencyTimer;

	public Runner() {
		this.successRatio = registry.register(name("success"), new SuccessRatioGauge());
		this.latencyTimer = registry.timer(name("latency"));
	}

	private static String name(String name) {
		return name;
	}
}
