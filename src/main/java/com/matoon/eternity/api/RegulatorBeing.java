package com.matoon.eternity.api;

import lucraft.mods.lucraftcore.superpowers.abilities.Ability;
import lucraft.mods.lucraftcore.util.abilitybar.EnumAbilityBarColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Something the Astral Regulator can drink in, and what it lends whoever holds the Regulator afterwards: its powers on
 * the ability bar (framed in its colour), the Forge Energy it pours out, and its place in the Regulator's tooltip.
 * Eternity's own beings (Eternity, the Living Tribunal, Master Order, Lord Chaos, the In-Betweener, Death) are
 * registered the same way. Built with {@link #builder(String)} and registered with
 * {@link EternityAPI#registerBeing(RegulatorBeing)} in pre-init.
 * <p>
 * Taking a being in is the addon's to decide: either give it an {@link Absorber}, run when the Regulator is raised,
 * or call {@link EternityAPI#absorb(ItemStack, String)} from your own fight when the moment comes.
 */
public final class RegulatorBeing {

    /** Raising the Regulator on the server: takes the being in if it can be taken now, and says whether it was. */
    @FunctionalInterface
    public interface Absorber {
        boolean tryAbsorb(EntityPlayerMP player, ItemStack regulator, EnumHand hand, List<String> held);
    }

    /** On the client: whether raising the Regulator here would take something in, so the hand swings. */
    @FunctionalInterface
    public interface Sensor {
        boolean near(EntityPlayer player, List<String> held);
    }

    /** What the being lends: its abilities put into the map it is given. */
    @FunctionalInterface
    public interface Grant {
        void grant(EntityLivingBase entity, Ability.AbilityMap map);
    }

    /**
     * The being drawn inside the Regulator's tooltip lens, client only. The matrix is already at the lens centre with
     * Y up and one unit the lens radius; draw it about a unit tall, scaled by {@code shrink} (smaller when it shares
     * the lens). {@code time} is in ticks.
     */
    @FunctionalInterface
    public interface Figure {
        void draw(float shrink, float time);
    }

    /** One power as the tooltip lists it. */
    public static final class Power {
        public final String key;
        public final String nameKey;
        public final ResourceLocation icon;

        public Power(String key, String nameKey, ResourceLocation icon) {
            this.key = key;
            this.nameKey = nameKey;
            this.icon = icon;
        }
    }

    public final String id;
    public final String nameKey, headerKey;
    public final int nameColour, headerColour, powerColour;
    public final EnumAbilityBarColor barColor;
    public final long energyPerTick;
    public final List<Power> powers;
    /** Whether the tooltip lists it. Death, who lends nothing, is held but not shown. */
    public final boolean listed;
    @Nullable
    private final Grant grant;
    @Nullable
    private final Absorber absorber;
    @Nullable
    private final Sensor sensor;
    @Nullable
    private final Class<? extends Entity> entity;
    @Nullable
    private Figure figure;

    private RegulatorBeing(Builder b) {
        id = b.id;
        nameKey = b.nameKey;
        headerKey = b.headerKey;
        nameColour = b.nameColour;
        headerColour = b.headerColour;
        powerColour = b.powerColour;
        barColor = b.barColor;
        energyPerTick = b.energy;
        powers = Collections.unmodifiableList(new ArrayList<>(b.powers));
        listed = b.listed;
        grant = b.grant;
        absorber = b.absorber;
        sensor = b.sensor;
        entity = b.entity;
        figure = b.figure;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Puts its powers into the map, each framed in its colour. */
    public void grant(EntityLivingBase holder, Ability.AbilityMap map) {
        if (grant == null) {
            return;
        }
        Set<String> before = new HashSet<>(map.keySet());
        grant.grant(holder, map);
        for (String key : map.keySet()) {
            if (!before.contains(key)) {
                EternityAPI.tint(map.get(key), barColor);
            }
        }
    }

    public boolean tryAbsorb(EntityPlayerMP player, ItemStack regulator, EnumHand hand, List<String> held) {
        return absorber != null && absorber.tryAbsorb(player, regulator, hand, held);
    }

    public boolean near(EntityPlayer player, List<String> held) {
        return sensor != null && sensor.near(player, held);
    }

    /** Whether this entity is the being, so raising the Regulator at it counts as raising it at all. */
    public boolean is(Entity target) {
        return entity != null && entity.isInstance(target);
    }

    @Nullable
    public Figure figure() {
        return figure;
    }

    /** Sets how the tooltip draws it, from client code (the rest of the being is common). */
    public RegulatorBeing setFigure(@Nullable Figure figure) {
        this.figure = figure;
        return this;
    }

    public static final class Builder {
        private final String id;
        private final String domain;
        private String nameKey, headerKey = "tooltip.eternity.powers";
        private int nameColour = 0xE6D9FF, headerColour = 0xD49A1C, powerColour = 0xF4F0FF;
        private EnumAbilityBarColor barColor = EnumAbilityBarColor.LIGHT_GRAY;
        private long energy;
        private final List<Power> powers = new ArrayList<>();
        private boolean listed = true;
        private Grant grant;
        private Absorber absorber;
        private Sensor sensor;
        private Class<? extends Entity> entity;
        private Figure figure;

        private Builder(String id) {
            this.id = id;
            this.domain = new ResourceLocation(id).getNamespace();
            String path = new ResourceLocation(id).getPath();
            this.nameKey = "entity." + domain + "." + path + ".name";
        }

        /** The translation key of its name in the tooltip; by default {@code entity.<modid>.<name>.name}. */
        public Builder name(String key, int colour) {
            nameKey = key;
            nameColour = colour;
            return this;
        }

        /** The heading over its powers ("Powers", "Judgements", "Laws"...) and the colour of the powers under it. */
        public Builder header(String key, int colour, int powerColour) {
            headerKey = key;
            headerColour = colour;
            this.powerColour = powerColour;
            return this;
        }

        /** The frame its abilities get on the ability bar. */
        public Builder bar(EnumAbilityBarColor color) {
            barColor = color;
            return this;
        }

        public Builder bar(Force force) {
            return bar(force.color);
        }

        /** Forge Energy a tick it adds to what the Regulator gives off. */
        public Builder energy(long perTick) {
            energy = perTick;
            return this;
        }

        /**
         * A power the tooltip lists, by the key it has in the ability map: named {@code <modid>.abilities.<key>.name},
         * with its icon at {@code <modid>:textures/abilities/<key>.png} (9x9 in the tooltip, 16x16 on the bar).
         */
        public Builder power(String key) {
            return power(key, domain + ".abilities." + key + ".name", new ResourceLocation(domain, "textures/abilities/" + key + ".png"));
        }

        public Builder power(String key, String nameKey, ResourceLocation icon) {
            powers.add(new Power(key, nameKey, icon));
            return this;
        }

        public Builder powers(String... keys) {
            for (String key : keys) {
                power(key);
            }
            return this;
        }

        public Builder unlisted() {
            listed = false;
            return this;
        }

        public Builder grants(Grant grant) {
            this.grant = grant;
            return this;
        }

        public Builder absorbedBy(Absorber absorber) {
            this.absorber = absorber;
            return this;
        }

        public Builder sensedBy(Sensor sensor) {
            this.sensor = sensor;
            return this;
        }

        /** The entity class the being is, so the Regulator can be used on it directly. */
        public Builder entity(Class<? extends Entity> type) {
            entity = type;
            return this;
        }

        public Builder figure(Figure figure) {
            this.figure = figure;
            return this;
        }

        public RegulatorBeing build() {
            return new RegulatorBeing(this);
        }
    }
}
