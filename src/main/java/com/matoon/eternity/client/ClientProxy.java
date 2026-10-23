package com.matoon.eternity.client;

import com.matoon.eternity.CommonProxy;
import com.matoon.eternity.Eternity;
import com.matoon.eternity.client.render.RegulatorItemRenderer;
import com.matoon.eternity.client.render.RenderAstralRegulator;
import com.matoon.eternity.client.render.RenderCosmicComet;
import com.matoon.eternity.client.render.RenderInBetweener;
import com.matoon.eternity.client.render.RenderEternity;
import com.matoon.eternity.client.render.RenderLivingTribunal;
import com.matoon.eternity.client.render.RenderOrderChaos;
import com.matoon.eternity.client.render.TesseractRenderer;
import com.matoon.eternity.entity.EntityAstralRegulator;
import com.matoon.eternity.entity.EntityCosmicComet;
import com.matoon.eternity.entity.EntityEternity;
import com.matoon.eternity.entity.EntityInBetweener;
import com.matoon.eternity.entity.EntityLivingTribunal;
import com.matoon.eternity.entity.EntityOrderChaos;
import com.matoon.eternity.item.EternityItems;
import com.matoon.eternity.network.MsgComet;
import com.matoon.eternity.network.MsgInBetweener;
import com.matoon.eternity.network.MsgTribunal;
import com.matoon.eternity.network.MsgTribunalFx;
import lucraft.mods.heroesexpansion.items.HEItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Eternity.MODID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityEternity.class, RenderEternity::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityAstralRegulator.class, RenderAstralRegulator::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCosmicComet.class, RenderCosmicComet::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityLivingTribunal.class, RenderLivingTribunal::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityInBetweener.class, RenderInBetweener::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityOrderChaos.class, RenderOrderChaos::new);
        EternityItems.ASTRAL_REGULATOR.setTileEntityItemStackRenderer(new RegulatorItemRenderer());
        MinecraftForge.EVENT_BUS.register(EternityClient.INSTANCE);
        MinecraftForge.EVENT_BUS.register(RegulatorTooltip.INSTANCE);
        MinecraftForge.EVENT_BUS.register(CometClient.INSTANCE);
        MinecraftForge.EVENT_BUS.register(TribunalClient.INSTANCE);
        MinecraftForge.EVENT_BUS.register(NexusClient.INSTANCE);
        MinecraftForge.EVENT_BUS.register(InBetweenerClient.INSTANCE);
        MinecraftForge.EVENT_BUS.register(DialogueCamera.INSTANCE);
        MinecraftForge.EVENT_BUS.register(FightMusic.INSTANCE);
        MinecraftForge.EVENT_BUS.register(TesseractClient.INSTANCE);
        EternityItems.RELIC_TRACKER.addPropertyOverride(new ResourceLocation("angle"), new RelicTrackerAngle());
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(EternityItems.RELIC_TRACKER, 0,
                new ModelResourceLocation(EternityItems.RELIC_TRACKER.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(EternityItems.ASTRAL_REGULATOR, 0,
                new ModelResourceLocation(EternityItems.ASTRAL_REGULATOR.getRegistryName(), "inventory"));
    }

    /**
     * HeroesExpansion's Tesseract is drawn by ours, with its own display transforms. Last, so it
     * lands after HeroesExpansion registers its own model and renderer for it.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replaceTesseract(ModelRegistryEvent event) {
        HEItems.TESSERACT.setTileEntityItemStackRenderer(new TesseractRenderer());
        ModelLoader.setCustomModelResourceLocation(HEItems.TESSERACT, 0,
                new ModelResourceLocation(new ResourceLocation(Eternity.MODID, "tesseract"), "inventory"));
    }

    /** Packets arrive on the network thread; everything visual happens on the client thread. */
    @Override
    public void receive(IMessage message) {
        if (message instanceof MsgTribunal) {
            Minecraft.getMinecraft().addScheduledTask(() -> TribunalClient.INSTANCE.handle((MsgTribunal) message));
        } else if (message instanceof MsgTribunalFx) {
            Minecraft.getMinecraft().addScheduledTask(() -> TribunalClient.INSTANCE.effect((MsgTribunalFx) message));
        } else if (message instanceof MsgInBetweener) {
            Minecraft.getMinecraft().addScheduledTask(() -> InBetweenerClient.INSTANCE.handle((MsgInBetweener) message));
        } else if (message instanceof MsgComet) {
            Minecraft.getMinecraft().addScheduledTask(() -> CometClient.INSTANCE.handle((MsgComet) message));
        } else {
            Minecraft.getMinecraft().addScheduledTask(() -> EternityClient.INSTANCE.handle(message));
        }
    }
}
