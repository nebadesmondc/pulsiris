package dev.pulsiris.checks.adapter.scheduling;

import dev.pulsiris.checks.application.RetentionJob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBooleanProperty(name = "pulsiris.checks.scheduler-enabled", matchIfMissing = true)
class RetentionScheduler {

	private final RetentionJob job;

	RetentionScheduler(RetentionJob job) {
		this.job = job;
	}

	// lockAtLeastFor: an instance whose clock is a few minutes behind cannot run it again tonight.
	@Scheduled(cron = "${pulsiris.checks.retention.cron}", zone = "UTC")
	@SchedulerLock(name = "checks.retention", lockAtMostFor = "PT2H", lockAtLeastFor = "PT10M")
	void run() {
		job.run();
	}

}
