package dev.pulsiris.checks.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import dev.pulsiris.checks.config.ChecksProperties;
import dev.pulsiris.checks.domain.CheckResult;
import dev.pulsiris.checks.domain.HealthState;
import dev.pulsiris.checks.domain.ProbeMethod;
import dev.pulsiris.checks.domain.ProbeTarget;
import dev.pulsiris.registry.TargetLookup;
import dev.pulsiris.registry.TargetSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * Finds the targets that are due, probes them in parallel and records each result.
 */
@Service
public class CheckRunner {

	private static final Logger log = LoggerFactory.getLogger(CheckRunner.class);

	private final TargetLookup targets;

	private final HealthStateRepository states;

	private final Prober prober;

	private final CheckRecorder recorder;

	private final Clock clock;

	private final int maxConcurrentProbes;

	CheckRunner(TargetLookup targets, HealthStateRepository states, Prober prober, CheckRecorder recorder,
			Clock clock, ChecksProperties properties) {
		this.targets = targets;
		this.states = states;
		this.prober = prober;
		this.recorder = recorder;
		this.clock = clock;
		this.maxConcurrentProbes = properties.maxConcurrentProbes();
	}

	/**
	 * @return how many targets were checked
	 */
	public int runDueChecks() {
		List<TargetSnapshot> due = findDue(clock.instant());
		if (due.isEmpty()) {
			return 0;
		}

		// One virtual thread per probe: waiting on the network costs almost nothing. The semaphore
		// caps how many probes hit the network at once.
		Semaphore permits = new Semaphore(maxConcurrentProbes);
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<CheckResult>> probes = new ArrayList<>(due.size());
			for (TargetSnapshot target : due) {
				probes.add(executor.submit(() -> probe(target, permits)));
			}
			for (int i = 0; i < due.size(); i++) {
				record(due.get(i), probes.get(i));
			}
		}
		log.debug("Checked {} target(s)", due.size());
		return due.size();
	}

	private List<TargetSnapshot> findDue(Instant now) {
		List<TargetSnapshot> enabled = targets.findEnabled();
		Map<UUID, HealthState> known = states.findByTargetIds(enabled.stream().map(TargetSnapshot::id).toList());
		return enabled.stream()
			.filter(target -> known.getOrDefault(target.id(), HealthState.unknown(target.id()))
				.isDue(target.interval(), now))
			.toList();
	}

	private CheckResult probe(TargetSnapshot target, Semaphore permits) throws InterruptedException {
		permits.acquire();
		try {
			return prober.probe(new ProbeTarget(target.id(), target.endpointUrl(),
					ProbeMethod.valueOf(target.httpMethod()), target.expectedStatus(), target.timeout()));
		}
		finally {
			permits.release();
		}
	}

	// One target's problem must not stop the others from being recorded.
	private void record(TargetSnapshot target, Future<CheckResult> probe) {
		try {
			recorder.record(target, probe.get());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException | RuntimeException ex) {
			log.error("Could not check target '{}'", target.name(), ex);
		}
	}

}
