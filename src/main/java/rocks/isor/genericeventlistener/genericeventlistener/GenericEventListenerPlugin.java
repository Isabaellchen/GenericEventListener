package rocks.isor.genericeventlistener.genericeventlistener;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashSet;

public class GenericEventListenerPlugin extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		// Implementation of https://www.spigotmc.org/threads/listening-to-all-events-listing-all-events.337466
		// Trying to fetch all available Events by using ClassGraph reflection on the spigot event system

		HashSet<String> filter = new HashSet<>(Arrays.asList(
				"EntityAirChangeEvent",
				"VehicleUpdateEvent",
				"ChunkUnloadEvent",
				"ChunkLoadEvent",
				"PlayerStatisticIncrementEvent"
		));

		ClassInfoList events = new ClassGraph()
				.enableClassInfo()
				.scan() //you should use try-catch-resources instead
				.getClassInfo(Event.class.getName())
				.getSubclasses()
				.filter(info -> !info.isAbstract() && !filter.contains(info.getSimpleName()));

		Listener listener = new Listener() {};
		EventExecutor executor = (ignored, event) -> getLogger().info("Event got fired: " + event.getEventName());

		try {
			for (ClassInfo event : events) {
				//noinspection unchecked
				Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(event.getName());

				if (Arrays.stream(eventClass.getDeclaredMethods()).anyMatch(method ->
						method.getParameterCount() == 0 && method.getName().equals("getHandlers"))) {
					//We could do this further filtering on the ClassInfoList instance instead,
					//but that would mean that we have to enable method info scanning.
					//I believe the overhead of initializing ~20 more classes
					//is better than that alternative.

					Bukkit.getPluginManager().registerEvent(eventClass, listener,
							EventPriority.NORMAL, executor, this);
				}
			}
		} catch (ClassNotFoundException e) {
			throw new AssertionError("Scanned class wasn't found", e);
		}

		String[] eventNames = events.stream()
				.map(info -> info.getName().substring(info.getName().lastIndexOf('.') + 1))
				.toArray(String[]::new);

		getLogger().info("List of events: " + String.join(", ", eventNames));
		getLogger().info("Events found: " + events.size());
		getLogger().info("HandlerList size: " + HandlerList.getHandlerLists().size());

	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll();
	}

	@EventHandler
	public void onEvent(Event event){
		Bukkit.getLogger().info(event.getEventName());
	}
}
