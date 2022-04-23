package cqb13.Numby;

import cqb13.Numby.modules.*;
import cqb13.Numby.modules.hud.*;
import cqb13.Numby.utils.PlayerParticle;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;


public class Numby extends MeteorAddon {
	public static final Logger LOG = LoggerFactory.getLogger(Numby.class);
	public static final Category CATEGORY = new Category("Numby hack", Items.TURTLE_HELMET.getDefaultStack());

	@Override
	public void onInitialize() {
		LOG.info("Number81 On Top");

		// Required when using @EventHandler
		MeteorClient.EVENT_BUS.registerLambdaFactory("cqb13.Numby", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        new PlayerParticle();

		Modules modules = Modules.get();
		modules.add(new ChatEncryption());
		modules.add(new Confetti());
		modules.add(new Number81());
		modules.add(new NumbyChat());
		modules.add(new NumbyEZ());
		modules.add(new NumbyRPC());
		modules.add(new TimeLog());

        HUD hud = Systems.get(HUD.class);
        hud.elements.add(new cqb13.Numby.modules.hud.Numby(hud));
        hud.elements.add(new Logo(hud));
	}

	@Override
	public void onRegisterCategories() {
		Modules.registerCategory(CATEGORY);
	}
}