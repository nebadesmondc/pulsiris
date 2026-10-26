package dev.pulsiris.checks.adapter.scheduling;

import dev.pulsiris.checks.application.CheckRunner;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Looks for due targets on every tick. ShedLock lets only one instance run a tick, so targets are
 * not checked twice when Pulsiris runs on several instances.
 */
@Component
@ConditionalOnBooleanProperty(name = "pulsiris.checks.scheduler-enabled", matchIfMissing = true)
class CheckScheduler {

	private final CheckRunner runner;

	CheckScheduler(CheckRunner runner) {
		this.runner = runner;
	}

	// lockAtMostFor: released even if this instance dies mid-run.
	// lockAtLeastFor: another instance whose clock is slightly ahead cannot run the same tick again.
	@Scheduled(fixedDelayString = "${pulsiris.checks.tick}", initialDelayString = "${pulsiris.checks.tick}")
	@SchedulerLock(name = "checks.run-due", lockAtMostFor = "PT5M", lockAtLeastFor = "PT2S")
	void runDueChecks() {
		runner.runDueChecks();
	}

}
