package com.bigstock.schedule.job.core;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class JobRegistry {
	private final Map<String, ManagedJob> jobs;

	public JobRegistry(List<ManagedJob> entries) {
		Map<String, ManagedJob> m = new LinkedHashMap<>();
		for (ManagedJob job : entries) {
			if (m.putIfAbsent(job.code(), job) != null)
				throw new IllegalStateException("duplicate job code: " + job.code());
		}
		jobs = Collections.unmodifiableMap(m);
	}

	public ManagedJob required(String code) {
		ManagedJob j = jobs.get(code);
		if (j == null)
			throw new NoSuchElementException("Unknown job: " + code);
		return j;
	}

	public Collection<ManagedJob> all() {
		return jobs.values();
	}
}
