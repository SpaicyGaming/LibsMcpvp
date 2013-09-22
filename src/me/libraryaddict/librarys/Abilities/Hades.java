package me.libraryaddict.librarys.Abilities;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.v1_6_R3.EntityCreature;
import net.minecraft.server.v1_6_R3.EntityInsentient;
import net.minecraft.server.v1_6_R3.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_6_R3.PathfinderGoalSelector;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftCreature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import me.libraryaddict.Hungergames.Events.PlayerKilledEvent;
import me.libraryaddict.Hungergames.Interfaces.Disableable;
import me.libraryaddict.Hungergames.Types.AbilityListener;
import me.libraryaddict.librarys.Misc.FollowOwner;
import me.libraryaddict.librarys.Misc.OwnerAttacked;
import me.libraryaddict.librarys.Misc.OwnerAttacks;

public class Hades extends AbilityListener implements Disableable {
    private int itemToUse = Material.ROTTEN_FLESH.getId();
    private transient HashMap<Zombie, Player> tamed = new HashMap<Zombie, Player>();

    private void makeSlave(LivingEntity entity, Player tamer) {
        EntityCreature eIG = ((CraftCreature) entity).getHandle();
        FollowOwner navig = new FollowOwner(eIG, 0.3F, 10.0F, 2.0F, tamer);
        try {
            Field field = EntityInsentient.class.getDeclaredField("targetSelector");
            field.setAccessible(true);
            PathfinderGoalSelector targetSelector = (PathfinderGoalSelector) field.get(eIG);
            Field targeta = PathfinderGoalSelector.class.getDeclaredField("a");
            targeta.setAccessible(true);
            ((List) targeta.get(targetSelector)).clear();
            targetSelector.a(4, new PathfinderGoalMeleeAttack(eIG, 0.3F, true));
            targetSelector.a(5, navig);
            field = EntityInsentient.class.getDeclaredField("goalSelector");
            field.setAccessible(true);
            targetSelector = (PathfinderGoalSelector) field.get(eIG);
            targeta = PathfinderGoalSelector.class.getDeclaredField("a");
            targeta.setAccessible(true);
            ((List) targeta.get(targetSelector)).clear();
            targetSelector.a(3, new OwnerAttacks(eIG, tamer));
            targetSelector.a(3, new OwnerAttacked(eIG, tamer));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        tamed.remove(event.getEntity());
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (hasAbility(event.getPlayer()) && event.getRightClicked() instanceof Zombie) {
            ItemStack item = event.getPlayer().getItemInHand();
            if (item != null && item.getTypeId() == itemToUse && !tamed.containsKey(event.getRightClicked())) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() == 0)
                    event.getPlayer().setItemInHand(new ItemStack(0));
                Zombie old = (Zombie) event.getRightClicked();
                Zombie monster = (Zombie) old.getWorld().spawnEntity(old.getLocation(), old.getType());
                monster.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                monster.getEquipment().setHelmetDropChance(0F);
                monster.setHealth(old.getHealth());
                monster.setFireTicks(old.getFireTicks());
                monster.setFallDistance(old.getFallDistance());
                monster.setVelocity(old.getVelocity());
                for (PotionEffect effect : old.getActivePotionEffects())
                    monster.addPotionEffect(effect);
                old.remove();
                old.setTicksLived(Integer.MAX_VALUE);
                old.setHealth(0);
                tamed.put(monster, event.getPlayer());
                makeSlave(monster, event.getPlayer());
                monster.setTarget(null);
            }
        }
    }

    @EventHandler
    public void onKilled(PlayerKilledEvent event) {
        if (hasAbility(event.getKilled().getPlayer())) {
            Iterator<Zombie> itel = tamed.keySet().iterator();
            while (itel.hasNext()) {
                Zombie zombie = itel.next();
                if (tamed.get(zombie) == event.getKilled().getPlayer()) {
                    itel.remove();
                    zombie.damage(999);
                }
            }
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (tamed.containsKey(event.getEntity()) && tamed.get(event.getEntity()) == event.getTarget()) {
            event.setCancelled(true);
        }
    }

}
