package com.github.litermc.vschunkloader.util;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

@Mod.EventBusSubscriber
public final class TaskUtil {
	private static final Queue<Task> TICK_START_QUEUE = new PriorityBlockingQueue<>();
	private static final Queue<Task> TICK_END_QUEUE = new PriorityBlockingQueue<>();
	private static long tick = 0;

	private TaskUtil() {}

	@SubscribeEvent
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		final Queue<Task> queue = switch (event.phase) {
			case START -> {
				tick++;
				yield TICK_START_QUEUE;
			}
			case END -> TICK_END_QUEUE;
		};
		for (int i = queue.size(); i > 0; i--) {
			final Task task = queue.element();
			if (task.tick() > tick) {
				return;
			}
			queue.remove();
			task.task().run();
		}
	}

	public static void queueTickStart(final int delay, final Runnable task) {
		TICK_START_QUEUE.add(new Task(tick + delay, task));
	}

	public static void queueTickEnd(final int delay, final Runnable task) {
		TICK_END_QUEUE.add(new Task(tick + delay, task));
	}

	record Task(long tick, Runnable task) implements Comparable<Task> {
		@Override
		public int compareTo(final Task other) {
			return this.tick < other.tick ? -1 : this.tick > other.tick ? 1 : 0;
		}
	}
}
