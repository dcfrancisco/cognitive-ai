package ph.francisco.memory;

import ph.francisco.perception.Observation;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Working memory is in-session and NOT persisted.
 * Keeps only a short rolling window of recent observations.
 */
public class WorkingMemory {
	public record Item(Instant at, String source, String content) {
	}

	private final int maxItems;
	private final Deque<Item> items;

	public WorkingMemory(int maxItems) {
		this.maxItems = Math.max(1, maxItems);
		this.items = new ArrayDeque<>(this.maxItems);
	}

	public synchronized void add(Observation observation) {
		items.addLast(new Item(Instant.now(), observation.source(), observation.content()));
		while (items.size() > maxItems) {
			items.removeFirst();
		}
	}

	public synchronized List<Item> snapshot() {
		return List.copyOf(items);
	}
}
