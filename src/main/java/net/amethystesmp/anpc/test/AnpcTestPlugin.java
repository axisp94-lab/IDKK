package net.amethystesmp.anpc.test;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.DummyTracker;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prototype : un NPC BetterModel (DummyTracker, sans entite) glisse sur N blocs
 * devant le joueur en jouant une animation, puis s'arrete et passe en "idle".
 *
 * /anpctest [modele] [blocs] [vitesse_blocs_par_s] [pas_ticks] [animation]
 *  - modele    : nom d'un modele de BetterModel/models (defaut blue_wizard),
 *                ou limb:nom pour un modele de joueur de BetterModel/players (skin du joueur)
 *  - blocs     : distance parcourue (defaut 10)
 *  - vitesse   : blocs par seconde (defaut 4.0)
 *  - pas_ticks : intervalle entre deux envois de position (defaut 1 = chaque tick)
 *  - animation : animation jouee pendant le trajet (defaut walk)
 */
public final class AnpcTestPlugin extends JavaPlugin {

    private final Set<DummyTracker> trackers = ConcurrentHashMap.newKeySet();
    private final Set<BukkitTask> tasks = ConcurrentHashMap.newKeySet();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande reservee aux joueurs.");
            return true;
        }

        String modelName = args.length > 0 ? args[0] : "blue_wizard";
        int blocks = parseInt(args, 1, 10, 1, 200);
        double speed = parseDouble(args, 2, 4.0, 0.1, 40.0);
        int step = parseInt(args, 3, 1, 1, 20);
        String animation = args.length > 4 ? args[4] : "walk";

        // Position de depart : 3 blocs devant le joueur, meme orientation horizontale.
        Location base = player.getLocation();
        base.setPitch(0f);
        Location start = base.clone().add(base.getDirection().setY(0).normalize().multiply(3));
        final double dx = base.getDirection().setY(0).normalize().getX();
        final double dz = base.getDirection().setY(0).normalize().getZ();

        ModelRenderer renderer;
        boolean limb = modelName.startsWith("limb:");
        if (limb) {
            renderer = BetterModel.limbOrNull(modelName.substring(5));
        } else {
            renderer = BetterModel.modelOrNull(modelName);
        }
        if (renderer == null) {
            player.sendMessage("Modele introuvable : " + modelName
                    + " (voir BetterModel/models ou BetterModel/players).");
            return true;
        }

        DummyTracker tracker = limb
                ? renderer.create(BukkitAdapter.adapt(start), ModelProfile.of(BukkitAdapter.adapt(player)))
                : renderer.create(BukkitAdapter.adapt(start));
        trackers.add(tracker);

        // Un DummyTracker n'est pas visible tout seul : on l'envoie aux joueurs proches.
        for (Player viewer : start.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(start) <= 64 * 64) {
                tracker.spawn(BukkitAdapter.adapt(viewer));
            }
        }

        boolean playing = tracker.animate(animation, AnimationModifier.DEFAULT);
        if (!playing) {
            player.sendMessage("Animation '" + animation + "' introuvable sur ce modele : le NPC glissera sans animer.");
        }
        player.sendMessage("Test lance : " + blocks + " blocs, " + speed + " blocs/s, envoi toutes les "
                + step + " tick(s), animation " + animation + ".");

        final double totalTicks = blocks / speed * 20.0;
        final long[] elapsed = {0};
        final BukkitTask[] self = new BukkitTask[1];

        self[0] = getServer().getScheduler().runTaskTimer(this, () -> {
            if (tracker.isClosed()) {
                cancel(self[0]);
                return;
            }
            elapsed[0] += step;
            double dist = Math.min(blocks, speed * elapsed[0] / 20.0);
            Location now = start.clone().add(dx * dist, 0, dz * dist);
            tracker.location(BukkitAdapter.adapt(now));

            if (elapsed[0] >= totalTicks) {
                cancel(self[0]);
                tracker.animate("idle", AnimationModifier.DEFAULT);
                player.sendMessage("Arrive. Le NPC disparait dans 5 secondes.");
                BukkitTask end = getServer().getScheduler().runTaskLater(this, () -> {
                    tracker.close();
                    trackers.remove(tracker);
                }, 100L);
                tasks.add(end);
            }
        }, 1L, step);
        tasks.add(self[0]);
        return true;
    }

    private void cancel(BukkitTask task) {
        if (task != null) {
            task.cancel();
            tasks.remove(task);
        }
    }

    @Override
    public void onDisable() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        trackers.forEach(DummyTracker::close);
        trackers.clear();
    }

    private static int parseInt(String[] a, int i, int def, int min, int max) {
        if (a.length <= i) return def;
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(a[i])));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double parseDouble(String[] a, int i, double def, double min, double max) {
        if (a.length <= i) return def;
        try {
            return Math.max(min, Math.min(max, Double.parseDouble(a[i])));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
